/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.ui.html.json;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.IOUtility;
import org.eclipse.scout.commons.annotations.Priority;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.ui.html.AbstractUiServlet;
import org.eclipse.scout.rt.ui.html.IServletRequestInterceptor;
import org.eclipse.scout.service.AbstractService;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This interceptor contributes to the {@link AbstractUiServlet} as the default POST handler
 */
@Priority(-10)
public class JsonMessageRequestInterceptor extends AbstractService implements IServletRequestInterceptor {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(JsonMessageRequestInterceptor.class);

  @Override
  public boolean interceptGet(AbstractUiServlet servlet, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    return false;
  }

  @Override
  public boolean interceptPost(AbstractUiServlet servlet, HttpServletRequest httpReq, HttpServletResponse httpResp) throws ServletException, IOException {
    //serve only /json
    String pathInfo = httpReq.getPathInfo();
    if (CompareUtility.notEquals(pathInfo, "/json")) {
      LOG.info("404_NOT_FOUND_POST: " + pathInfo);
      httpResp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return true;
    }
    IJsonSession jsonSession = null;
    JsonRequest jsonReq = null;
    try {
      //disable cache
      servlet.getHttpCacheControl().disableCacheHeaders(httpReq, httpResp);

      JSONObject jsonReqObj = decodeJSONRequest(httpReq);
      if (isPingRequest(jsonReqObj)) {
        writeResponse(httpResp, createPingJsonResponse().toJson());
        return true;
      }

      jsonReq = new JsonRequest(jsonReqObj);
      jsonSession = getOrCreateJsonSession(servlet, httpReq, httpResp, jsonReq);
      if (jsonSession == null) {
        return true;
      }

      if (jsonReq.isPollForBackgroundJobsRequest()) {
        // Blocks the current thread until:
        // - a model job terminates
        // - the max. wait time has exceeded
        jsonSession.waitForBackgroundJobs();
      }

      // GUI requests for the same session must be processed consecutively
      synchronized (jsonSession) {
        JSONObject jsonResp = jsonSession.processRequest(httpReq, jsonReq);
        writeResponse(httpResp, jsonResp);
      }
    }
    catch (Exception e) {
      if (jsonReq == null || jsonSession == null || jsonReq.isStartupRequest()) {
        // Send a special error code when an error happens during initialization, because
        // the UI has no translated texts to show in this case.
        LOG.error("Error while initializing json session", e);
        writeResponse(httpResp, createStartupFailedJsonResponse());
      }
      else {
        LOG.error("Unexpected error while processing JSON request", e);
        writeResponse(httpResp, createUnrecoverableFailureJsonResponse());
      }
    }
    return true;
  }

  /**
   * Check if request is a simple ping. We don't use JsonRequest here because a ping request has no jsonSessionId.
   */
  private boolean isPingRequest(JSONObject json) {
    return json.has("ping");
  }

  protected IJsonSession getOrCreateJsonSession(AbstractUiServlet servlet, HttpServletRequest req, HttpServletResponse resp, JsonRequest jsonReq) throws ServletException, IOException {
    String jsonSessionAttributeName = IJsonSession.HTTP_SESSION_ATTRIBUTE_PREFIX + jsonReq.getJsonSessionId();
    HttpSession httpSession = req.getSession();

    // FIXME cgu really synchronize on this? blocks every call, maybe introduce a lock object saved on httpSession or even better use java.util.concurrent.locks.ReadWriteLock
    synchronized (httpSession) {
      IJsonSession jsonSession = (IJsonSession) httpSession.getAttribute(jsonSessionAttributeName);

      if (jsonReq.isUnloadRequest()) {
        LOG.info("Unloading JSON session with ID " + jsonReq.getJsonSessionId() + " (requested by UI)");
        if (jsonSession != null) {
          // Unbinding the jsonSession will cause it to be disposed automatically, see AbstractJsonSession.valueUnbound()
          httpSession.removeAttribute(jsonSessionAttributeName);
        }
        writeResponse(resp, new JSONObject()); // send empty response to satisfy clients expecting a valid response
        return null;
      }

      if (jsonSession == null) {
        if (!jsonReq.isStartupRequest()) {
          LOG.info("Request cannot be processed due to JSON session timeout [id=" + jsonReq.getJsonSessionId() + "]");
          writeResponse(resp, createSessionTimeoutJsonResponse());
          return null;
        }
        LOG.info("Creating new JSON session with ID " + jsonReq.getJsonSessionId() + "...");
        jsonSession = servlet.createJsonSession();
        jsonSession.init(req, new JsonStartupRequest(jsonReq));
        httpSession.setAttribute(jsonSessionAttributeName, jsonSession);
      }
      else if (jsonReq.isStartupRequest()) {
        throw new IllegalStateException("Startup requested for existing JSON session with ID " + jsonReq.getJsonSessionId());
      }
      return jsonSession;
    }
  }

  protected JsonResponse createSessionTimeoutJsonResponse() {
    JsonResponse response = new JsonResponse();
    response.markAsError(JsonResponse.ERR_SESSION_TIMEOUT, "The session has expired, please reload the page.");
    return response;
  }

  protected JsonResponse createUnrecoverableFailureJsonResponse() {
    JsonResponse response = new JsonResponse();
    response.markAsError(JsonResponse.ERR_UI_PROCESSING, "UI processing error");
    return response;
  }

  protected JsonResponse createStartupFailedJsonResponse() {
    JsonResponse response = new JsonResponse();
    response.markAsError(JsonResponse.ERR_STARTUP_FAILED, "Initialization failed");
    return response;
  }

  protected JsonResponse createPingJsonResponse() {
    return new JsonResponse();
  }

  protected void writeResponse(HttpServletResponse res, JsonResponse response) throws IOException {
    writeResponse(res, response.toJson());
  }

  protected void writeResponse(HttpServletResponse httpResp, JSONObject jsonResp) throws IOException {
    String jsonText = jsonResp.toString();
    byte[] data = jsonText.getBytes("UTF-8");
    httpResp.setContentLength(data.length);
    httpResp.setContentType("application/json");
    httpResp.setCharacterEncoding("UTF-8");
    httpResp.getOutputStream().write(data);
    if (LOG.isTraceEnabled()) {
      LOG.trace("Returned: " + jsonText);
    }
    else if (LOG.isDebugEnabled()) {
      // Truncate log output to not spam the log (and in case of eclipse to not make it freeze: https://bugs.eclipse.org/bugs/show_bug.cgi?id=175888)
      if (jsonText.length() > 10000) {
        jsonText = jsonText.substring(0, 10000) + "...";
      }
      LOG.debug("Returned: " + jsonText);
    }
  }

  protected JSONObject decodeJSONRequest(HttpServletRequest req) {
    try {
      String jsonData = IOUtility.getContent(req.getReader());
      if (LOG.isDebugEnabled()) {
        LOG.debug("Received: " + jsonData);
      }
      return (jsonData == null ? new JSONObject() : new JSONObject(jsonData));
    }
    catch (ProcessingException | IOException | JSONException e) {
      throw new JsonException(e.getMessage(), e);
    }
  }
}
