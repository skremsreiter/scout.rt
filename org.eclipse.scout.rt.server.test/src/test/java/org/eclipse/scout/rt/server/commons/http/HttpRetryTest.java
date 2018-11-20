/*******************************************************************************
 * Copyright (c) 2018 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.commons.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.scout.rt.platform.exception.ProcessingException;
import org.eclipse.scout.rt.platform.util.IOUtility;
import org.eclipse.scout.rt.platform.util.ObjectUtility;
import org.eclipse.scout.rt.platform.util.SleepUtil;
import org.eclipse.scout.rt.platform.util.concurrent.FutureCancelledError;
import org.eclipse.scout.rt.server.commons.servlet.AbstractHttpServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;

/**
 * Test if the Google HTTP Client API (here called 'google level') together with the Apache HTTP Client (here called
 * 'apache level') works as expected by triggering various failures in the http transport layer.
 * <p>
 * The Google HTTP Client API is not only an API, it contains an execution loop that handles various retry scenarios.
 * However in its core it uses a http transport - hers it is Apache HTTP Client.
 * <p>
 * Apache HTTP client also handles various http retry scenarios in its exec loop.
 */
public class HttpRetryTest {
  private static final String CORRELATION_ID = "Correlation-Id";

  private TestingHttpClient m_client;
  private TestingHttpServer m_server;

  @Before
  public void before() {
    Servlet.doGetLog.clear();
    Servlet.doPostLog.clear();
    Servlet.doPostError = null;
    m_client = new TestingHttpClient();
    m_server = new TestingHttpServer(TestingHttpPorts.PORT_33000, "/", getClass().getResource("/webapps/" + getClass().getSimpleName()));
    m_server.start();
  }

  @After
  public void after() {
    m_client.stop();
    m_server.stop();
  }

  /**
   * Expect retry on apache level
   */
  @Test
  public void testGetRetry() throws IOException {
    //emulate a socket close before data is received
    AtomicInteger count = new AtomicInteger(1);
    m_server.setChannelInterceptor((channel, superCall) -> {
      if (count.getAndIncrement() < 2) {
        channel.getHttpTransport().abort(new SocketException("TEST:cannot write"));
        return;
      }
      superCall.call();
    });

    HttpRequestFactory reqFactory = m_client.getHttpRequestFactory();
    HttpRequest req = reqFactory.buildGetRequest(new GenericUrl(m_server.getContextUrl() + "retry?foo=bar"));
    req.getHeaders().set(CORRELATION_ID, "01");
    HttpResponse resp = req.execute();
    byte[] bytes = IOUtility.readBytes(resp.getContent(), ObjectUtility.nvl(resp.getHeaders().getContentLength(), -1L).intValue());
    String text = new String(bytes, StandardCharsets.UTF_8).trim();
    assertEquals(text, "Hello bar");
    assertEquals(StandardCharsets.UTF_8, resp.getContentCharset());
    assertEquals(new String(bytes), 11, bytes.length);//text + CR + LF
    assertEquals(Arrays.asList("01"), Servlet.doGetLog);
  }

  /**
   * Expect retry on apache level
   */
  @Test
  public void testPostWithUnsupportedRetryAndFailureWhileHeadersAreSent() throws IOException {
    //emulate a header write error
    AtomicInteger count = new AtomicInteger(1);
    m_client.setRequestInterceptor(
        (request, conn, context, superCall) -> {
          if (count.getAndIncrement() < 2) {
            conn.close();
          }
          return superCall.call();
        });

    HttpRequestFactory reqFactory = m_client.getHttpRequestFactory();
    HttpRequest req = reqFactory.buildPostRequest(new GenericUrl(m_server.getContextUrl() + "retry"), new HttpContent() {
      @Override
      public void writeTo(OutputStream out) throws IOException {
        out.write("bar".getBytes());
      }

      @Override
      public boolean retrySupported() {
        return false;
      }

      @Override
      public String getType() {
        return "text/plain;charset=UTF-8";
      }

      @Override
      public long getLength() throws IOException {
        return 3;
      }
    });
    req.getHeaders().set(CORRELATION_ID, "02");
    HttpResponse resp = req.execute();
    byte[] bytes = IOUtility.readBytes(resp.getContent(), ObjectUtility.nvl(resp.getHeaders().getContentLength(), -1L).intValue());
    String text = new String(bytes, StandardCharsets.UTF_8).trim();
    assertEquals(text, "Post bar");
    assertEquals(StandardCharsets.UTF_8, resp.getContentCharset());
    assertEquals(new String(bytes), 10, bytes.length);//text + CR + LF
    assertEquals(Arrays.asList("02"), Servlet.doPostLog);
    assertNull(Servlet.doPostError);
  }

  /**
   * Expect no-retry on apache and google level
   */
  @Test
  public void testPostWithUnsupportedRetryAndFailureWhileBodyIsSent() throws IOException {
    //emulate a request body write error
    AtomicInteger count = new AtomicInteger(1);

    HttpRequestFactory reqFactory = m_client.getHttpRequestFactory();
    HttpRequest req = reqFactory.buildPostRequest(new GenericUrl(m_server.getContextUrl() + "retry"), new HttpContent() {
      @Override
      public void writeTo(OutputStream out) throws IOException {
        if (count.getAndIncrement() < 2) {
          out.write("ba".getBytes());
          throw new SocketException("TEST:cannot write");
        }
        out.write("bar".getBytes());
      }

      @Override
      public boolean retrySupported() {
        return false;
      }

      @Override
      public String getType() {
        return "text/plain;charset=UTF-8";
      }

      @Override
      public long getLength() throws IOException {
        return 3;
      }
    });
    req.getHeaders().set(CORRELATION_ID, "03");
    try {
      req.execute();
    }
    catch (org.apache.http.client.ClientProtocolException e) {
      //the request was partially sent, resulting in a servlet side org.eclipse.jetty.io.EofException: Early EOF
      // awaiting completion of that exception
      for (int i = 0; i < 100 && Servlet.doPostError == null; i++) {
        SleepUtil.sleepSafe(100, TimeUnit.MILLISECONDS);
      }
      assertEquals(Arrays.asList("03"), Servlet.doPostLog);
      assertNotNull(Servlet.doPostError);
      assertEquals(ProcessingException.class, Servlet.doPostError.getClass());
      assertEquals(org.eclipse.jetty.io.EofException.class, Servlet.doPostError.getCause().getClass());
      return;
    }
    fail("Expected to fail");
  }

  /**
   * Expect no-retry on apache and google level
   */
  @Test
  public void testPostWithUnsupportedRetryAndFailureAfterRequestIsSent() throws IOException {
    //emulate a socket close before data is received
    AtomicInteger count = new AtomicInteger(1);
    m_server.setChannelInterceptor((channel, superCall) -> {
      if (count.getAndIncrement() < 2) {
        channel.getHttpTransport().abort(new SocketException("TEST:cannot write"));
        return;
      }
      superCall.call();
    });

    HttpRequestFactory reqFactory = m_client.getHttpRequestFactory();
    HttpRequest req = reqFactory.buildPostRequest(new GenericUrl(m_server.getContextUrl() + "retry"), new HttpContent() {
      @Override
      public void writeTo(OutputStream out) throws IOException {
        out.write("bar".getBytes());
      }

      @Override
      public boolean retrySupported() {
        return false;
      }

      @Override
      public String getType() {
        return "text/plain;charset=UTF-8";
      }

      @Override
      public long getLength() throws IOException {
        return 3;
      }
    });
    req.getHeaders().set(CORRELATION_ID, "04");
    try {
      req.execute();
    }
    catch (org.apache.http.NoHttpResponseException e) {
      assertEquals(Arrays.asList(), Servlet.doPostLog);
      assertNull(Servlet.doPostError);
      return;
    }
    fail("Expected to fail");
  }

  /**
   * Expect no-retry on apache but retry on google level
   */
  @Test
  public void testPostWithSupportedRetryAndFailureAfterRequestIsSent() throws IOException {
    //emulate a socket close before data is received
    AtomicInteger count = new AtomicInteger(1);
    m_server.setChannelInterceptor((channel, superCall) -> {
      if (count.getAndIncrement() < 2) {
        channel.getHttpTransport().abort(new SocketException("TEST:cannot write"));
        return;
      }
      superCall.call();
    });

    HttpRequestFactory reqFactory = m_client.getHttpRequestFactory();
    HttpRequest req = reqFactory.buildPostRequest(new GenericUrl(m_server.getContextUrl() + "retry"), new HttpContent() {
      @Override
      public void writeTo(OutputStream out) throws IOException {
        out.write("bar".getBytes());
      }

      @Override
      public boolean retrySupported() {
        return true;
      }

      @Override
      public String getType() {
        return "text/plain;charset=UTF-8";
      }

      @Override
      public long getLength() throws IOException {
        return 3;
      }
    });
    req.getHeaders().set(CORRELATION_ID, "05");
    HttpResponse resp = req.execute();
    byte[] bytes = IOUtility.readBytes(resp.getContent(), ObjectUtility.nvl(resp.getHeaders().getContentLength(), -1L).intValue());
    String text = new String(bytes, StandardCharsets.UTF_8).trim();
    assertEquals(text, "Post bar");
    assertEquals(StandardCharsets.UTF_8, resp.getContentCharset());
    assertEquals(new String(bytes), 10, bytes.length);//text + CR + LF
    assertEquals(Arrays.asList("05"), Servlet.doPostLog);
    assertNull(Servlet.doPostError);
  }

  /**
   * Expect no-retry on apache and google level since a valid response (400) was received
   */
  @Test
  public void testPostWithSupportedRetryAndFailureWithValidHttpResponseCode() throws IOException {
    HttpRequestFactory reqFactory = m_client.getHttpRequestFactory();
    HttpRequest req = reqFactory.buildPostRequest(new GenericUrl(m_server.getContextUrl() + "retry"), new HttpContent() {
      @Override
      public void writeTo(OutputStream out) throws IOException {
        //emulate a failure in the servlet
        out.write("bar".getBytes());
      }

      @Override
      public boolean retrySupported() {
        return true;
      }

      @Override
      public String getType() {
        return "text/plain;charset=UTF-8";
      }

      @Override
      public long getLength() throws IOException {
        return 3;
      }
    });
    req.getHeaders().set(CORRELATION_ID, "06");
    Servlet.failOnce = (hreq, hresp) -> {
      hresp.sendError(400);
    };
    req.setThrowExceptionOnExecuteError(false);
    HttpResponse resp = req.execute();
    assertEquals(400, resp.getStatusCode());
    assertEquals(Arrays.asList("06"), Servlet.doPostLog);
    assertNull(Servlet.doPostError);
  }

  /**
   * Expect no-retry on apache and google level since a valid response (500) was received
   */
  @Test
  public void testPostWithSupportedRetryAndFailureWithoutHttpResponse() throws IOException {
    HttpRequestFactory reqFactory = m_client.getHttpRequestFactory();
    HttpRequest req = reqFactory.buildPostRequest(new GenericUrl(m_server.getContextUrl() + "retry"), new HttpContent() {
      @Override
      public void writeTo(OutputStream out) throws IOException {
        //emulate a failure in the servlet
        out.write("bar".getBytes());
      }

      @Override
      public boolean retrySupported() {
        return true;
      }

      @Override
      public String getType() {
        return "text/plain;charset=UTF-8";
      }

      @Override
      public long getLength() throws IOException {
        return 3;
      }
    });
    req.getHeaders().set(CORRELATION_ID, "07");
    Servlet.failOnce = (hreq, hresp) -> {
      throw new FutureCancelledError("TEST");
    };
    req.setThrowExceptionOnExecuteError(false);
    HttpResponse resp = req.execute();
    assertEquals(500, resp.getStatusCode());
    assertEquals(Arrays.asList("07"), Servlet.doPostLog);
    assertNull(Servlet.doPostError);
  }

  interface IFailOnce {
    void run(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
  }

  /**
   * http://172.0.0.1:33xyz/retry
   */
  public static class Servlet extends AbstractHttpServlet {
    private static final long serialVersionUID = 1L;
    static IFailOnce failOnce;
    static final List<String> doGetLog = Collections.synchronizedList(new ArrayList<>());
    static final List<String> doPostLog = Collections.synchronizedList(new ArrayList<>());
    static Exception doPostError;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doGetLog.add(req.getHeader(CORRELATION_ID));
      resp.setContentType("text/plain;charset=UTF-8");
      resp.getOutputStream().println("Hello " + req.getParameter("foo"));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doPostLog.add(req.getHeader(CORRELATION_ID));
      String arg = null;
      try {
        assertEquals("text/plain;charset=UTF-8", req.getContentType());
        assertEquals("UTF-8", req.getCharacterEncoding());
        assertEquals(3, req.getContentLength());
        arg = IOUtility.readString(req.getInputStream(), req.getCharacterEncoding(), req.getContentLength());
      }
      catch (Exception e) {
        doPostError = e;
        throw e;
      }

      if (failOnce != null) {
        try {
          failOnce.run(req, resp);
          return;
        }
        finally {
          failOnce = null;
        }
      }

      resp.setContentType("text/plain;charset=UTF-8");
      resp.getOutputStream().println("Post " + arg);
    }
  }
}
