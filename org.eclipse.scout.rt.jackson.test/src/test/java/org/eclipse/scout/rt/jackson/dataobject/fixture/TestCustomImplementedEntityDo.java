/*
 * Copyright (c) 2010-2018 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
package org.eclipse.scout.rt.jackson.dataobject.fixture;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.scout.rt.dataobject.DoEntity;
import org.eclipse.scout.rt.dataobject.DoList;
import org.eclipse.scout.rt.dataobject.DoNode;
import org.eclipse.scout.rt.dataobject.DoValue;
import org.eclipse.scout.rt.dataobject.IDoEntity;
import org.eclipse.scout.rt.dataobject.IValueFormatConstants;
import org.eclipse.scout.rt.dataobject.TypeName;
import org.eclipse.scout.rt.dataobject.ValueFormat;
import org.eclipse.scout.rt.platform.util.ObjectUtility;

/**
 * Custom test implementation of {@link IDoEntity}, without extending {@link DoEntity}.
 */
@TypeName("TestCustomImplementedEntity")
public class TestCustomImplementedEntityDo implements IDoEntity {

  private final Map<String, DoNode<?>> m_attributes = new LinkedHashMap<>();

  // attributes
  private static final String DATE_ATTRIBUTE = "dateAttribute";

  @SuppressWarnings("unchecked")
  @ValueFormat(pattern = IValueFormatConstants.DATE_PATTERN)
  public DoValue<Date> dateAttribute() {
    if (!has(DATE_ATTRIBUTE)) {
      put(DATE_ATTRIBUTE, null);
    }
    return (DoValue<Date>) getNode(DATE_ATTRIBUTE);
  }

  // interface methods

  @Override
  public DoNode<?> getNode(String attributeName) {
    return m_attributes.get(attributeName);
  }

  @Override
  public boolean has(String attributeName) {
    return m_attributes.containsKey(attributeName);
  }

  @Override
  public void putNode(String attributeName, DoNode<?> attribute) {
    IDoEntity.super.putNode(attributeName, attribute);
    m_attributes.put(attributeName, attribute);
  }

  @Override
  public void put(String attributeName, Object value) {
    putNode(attributeName, DoValue.of(value));
  }

  @Override
  public <V> void putList(String attributeName, List<V> value) {
    DoList<V> doList = new DoList<>();
    doList.set(value);
    putNode(attributeName, doList);
  }

  @Override
  public boolean remove(String attributeName) {
    return m_attributes.remove(attributeName) != null;
  }

  @Override
  public boolean removeIf(Predicate<? super DoNode<?>> filter) {
    return m_attributes.values().removeIf(filter);
  }

  @Override
  public Map<String, DoNode<?>> allNodes() {
    return Collections.unmodifiableMap(m_attributes);
  }

  @Override
  public Map<String, ?> all() {
    return allNodes().entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().get()));
  }

  @Override
  public boolean equals(Object obj) {
    return ObjectUtility.equals(m_attributes, ((TestCustomImplementedEntityDo) obj).m_attributes);
  }

  @Override
  public String toString() {
    return TestCustomImplementedEntityDo.class.getSimpleName() + " [m_attributes=" + m_attributes + "]";
  }
}
