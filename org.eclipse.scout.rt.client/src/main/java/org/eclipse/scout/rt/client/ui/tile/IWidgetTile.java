/*******************************************************************************
 * Copyright (c) 2017 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.ui.tile;

import org.eclipse.scout.rt.client.ui.IWidget;

public interface IWidgetTile<T extends IWidget> extends ITile {
  String PROP_REF_WIDGET = "refWidget";

  T getRefWidget();
}
