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
package org.eclipse.scout.rt.client.ui;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.eclipse.scout.rt.client.ui.action.menu.AbstractMenu;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.platform.util.visitor.TreeVisitResult;
import org.junit.Test;

public class WidgetVisitorWithCycleTest {

  @Test
  public void testDepthFirst() {
    AtomicInteger counter = new AtomicInteger();
    Consumer<IWidget> visitor = widget -> counter.getAndIncrement();
    createFixture().visit(visitor);
    assertEquals(6, counter.intValue());
  }

  @Test
  public void testBreadthFirst() {
    AtomicInteger counter = new AtomicInteger();
    createFixture().visit((widget, level, index) -> {
      counter.getAndIncrement();
      return TreeVisitResult.CONTINUE;
    });
    assertEquals(6, counter.intValue());
  }

  private IMenu createFixture() {
    IMenu root = new P_Menu();
    IMenu m1 = new P_Menu();
    IMenu m2 = new P_Menu();
    IMenu m1_1 = new P_Menu();
    IMenu m1_2 = new P_Menu();
    IMenu m1_3 = new P_Menu();

    root.addChildAction(m1);
    root.addChildAction(m2);
    m1.addChildAction(m1_1);
    m1.addChildAction(m1_2);
    m1.addChildAction(m1_3);

    root.addChildAction(m1_3); // menu that is added at two places
    m1_3.addChildAction(root); // creates a cycle
    return root;
  }

  private static final class P_Menu extends AbstractMenu {
  }
}
