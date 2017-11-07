/*******************************************************************************
 * Copyright (c) 2010-2017 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.commons;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.List;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.Platform;
import org.eclipse.scout.rt.platform.config.AbstractBooleanConfigProperty;
import org.eclipse.scout.rt.platform.config.AbstractConfigProperty;
import org.eclipse.scout.rt.platform.config.AbstractMapConfigProperty;
import org.eclipse.scout.rt.platform.config.PlatformConfigProperties.PlatformDevModeProperty;
import org.eclipse.scout.rt.platform.util.StringUtility;
import org.eclipse.scout.rt.server.commons.healthcheck.RemoteHealthChecker;
import org.eclipse.scout.rt.server.commons.servlet.ContentSecurityPolicy;

public final class ServerCommonsConfigProperties {

  private ServerCommonsConfigProperties() {
  }

  public static class UrlHintsEnabledProperty extends AbstractBooleanConfigProperty {

    @Override
    public Boolean getDefaultValue() {
      return Platform.get().inDevelopmentMode();
    }

    @Override
    public String description() {
      return String.format("Enable or disable changing UrlHints using URL parameters in the browser address line.\n"
          + "By default has the same value as the config property '%s' meaning it is by default only enabled in development mode.",
          BEANS.get(PlatformDevModeProperty.class).getKey());
    }

    @Override
    public String getKey() {
      return "scout.urlHints.enabled";
    }
  }

  public static class RemoteHealthCheckUrlsProperty extends AbstractConfigProperty<List<String>, String> {

    @Override
    public String getKey() {
      return "scout.healthCheckRemoteUrls";
    }

    @Override
    public String description() {
      return String.format("Comma separated list of URLs the '%s' should access.\n"
          + "By default no URLs are set.", RemoteHealthChecker.class.getSimpleName());
    }

    @Override
    protected List<String> parse(String value) {
      String[] tokens = StringUtility.tokenize(value, ',');
      // Prevent accidental modification by returning an unmodifiable list because property is cached and always returns the same instance
      return unmodifiableList(asList(tokens));
    }

    @Override
    public List<String> getDefaultValue() {
      return emptyList();
    }
  }

  public static class CspEnabledProperty extends AbstractBooleanConfigProperty {

    @Override
    public Boolean getDefaultValue() {
      return true;
    }

    @Override
    public String description() {
      return String.format("Enable or disable Content Security Policy (CSP) headers. The headers can be modified by replacing the bean '%s' or using the property '%s'.",
          ContentSecurityPolicy.class.getName(), BEANS.get(CspDirectiveProperty.class).getKey());
    }

    @Override
    public String getKey() {
      return "scout.cspEnabled";
    }

    public static class CspDirectiveProperty extends AbstractMapConfigProperty {

      @Override
      public String getKey() {
        return "scout.cspDirective";
      }

      @Override
      public String description() {
        return String.format("Configures individual Content Security Policy (CSP) directives.\n"
            + "See https://www.w3.org/TR/CSP2/ and the Bean '%s' for more details.\n"
            + "The value must be provided as a Map.\n"
            + "Example: scout.cspDirective[img-src]='self' data: https: http://localhost:8086",
            ContentSecurityPolicy.class.getName());
      }
    }
  }
}
