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
package org.eclipse.scout.rt.platform.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.Platform;
import org.eclipse.scout.rt.platform.util.ObjectUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h3>{@link ConfigPropertyValidator}</h3>
 */
public class ConfigPropertyValidator implements IConfigurationValidator {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigPropertyValidator.class);

  private Map<String, IConfigProperty<?>> m_configProperties;

  protected Map<String, IConfigProperty<?>> getAllConfigProperties() {
    if (m_configProperties == null) {
      List<IConfigProperty> configProperties = BEANS.all(IConfigProperty.class);
      Map<String, IConfigProperty<?>> props = new HashMap<>(configProperties.size());
      for (IConfigProperty<?> prop : configProperties) {
        props.put(prop.getKey(), prop);
      }
      m_configProperties = props;
    }
    return m_configProperties;
  }

  protected String parseKey(String key) {
    int start = key.indexOf(PropertiesHelper.NAMESPACE_DELIMITER) + 1;
    int end = key.indexOf(PropertiesHelper.COLLECTION_DELIMITER_START, start);
    if (end < start) {
      end = key.length();
    }
    return key.substring(start, end);
  }

  @Override
  public boolean isValid(String key, String value) {
    String parsedKey = parseKey(key);
    IConfigProperty<?> property = getAllConfigProperties().get(parsedKey);
    if (property == null) {
      return PropertiesHelper.IMPORT_KEY.equals(parsedKey); // 'import' key should be accepted although there is no IConfigProperty class for this key.
    }

    try {
      property.getValue(); // check if the given value is valid according to the value constraints of that property class.
      checkDefaultValueConfiguration(parsedKey, property, value);
    }
    catch (Exception ex) {
      LOG.error("Failed parsing value of config property with key='{}'. Configured value='{}'.", parsedKey, value, ex);
      return false;
    }

    return true;
  }

  /**
   * Check if configured value matches the default value
   */
  protected void checkDefaultValueConfiguration(String parsedKey, IConfigProperty<?> property, String configuredValue) {
    Object actualValue = property.getValue();
    Object defaultValue = property.getDefaultValue();
    if (ObjectUtility.equals(actualValue, defaultValue)) {
      String msg = "Config property with key='{}' has configured value='{}'. This results in an actual value of '{}' which is equal to the default value. Remove config entry for this key to minimize properties file.";
      if (Platform.get().inDevelopmentMode()) {
        LOG.warn(msg, parsedKey, configuredValue, actualValue);
      }
      else {
        LOG.info(msg, parsedKey, configuredValue, actualValue);
      }
    }
  }
}
