/*******************************************************************************
 * Copyright (c) 2014-2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.ui.html.res.loader;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.scout.rt.client.services.common.icon.IconLocator;
import org.eclipse.scout.rt.client.services.common.icon.IconSpec;
import org.eclipse.scout.rt.platform.resource.BinaryResource;
import org.eclipse.scout.rt.platform.resource.BinaryResources;
import org.eclipse.scout.rt.server.commons.servlet.cache.HttpCacheControl;
import org.eclipse.scout.rt.server.commons.servlet.cache.HttpCacheKey;
import org.eclipse.scout.rt.server.commons.servlet.cache.HttpCacheObject;

/**
 * This class loads static icon images from {@link IconLocator} (<code>/resource/icons</code> folders of all jars on the
 * classpath).
 */
public class IconLoader extends AbstractResourceLoader {

  public IconLoader(HttpServletRequest req) {
    super(req);
  }

  @Override
  public HttpCacheObject loadResource(HttpCacheKey cacheKey) {
    String pathInfo = cacheKey.getResourcePath();
    final String imageId = pathInfo.substring(pathInfo.lastIndexOf('/') + 1);
    IconSpec iconSpec = IconLocator.instance().getIconSpec(imageId);
    if (iconSpec != null) {
      // cache: use max-age caching for at most 4 hours
      BinaryResource content = BinaryResources.create()
          .withFilename(iconSpec.getName())
          .withContent(iconSpec.getContent())
          .withLastModified(System.currentTimeMillis())
          .withCachingAllowed(true)
          .withCacheMaxAge(HttpCacheControl.MAX_AGE_4_HOURS)
          .build();

      return new HttpCacheObject(cacheKey, content);
    }
    return null;
  }

}
