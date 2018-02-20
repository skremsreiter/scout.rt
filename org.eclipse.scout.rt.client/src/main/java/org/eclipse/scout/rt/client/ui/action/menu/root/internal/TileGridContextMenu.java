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
package org.eclipse.scout.rt.client.ui.action.menu.root.internal;

import java.beans.PropertyChangeEvent;
import java.util.List;

import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.action.menu.MenuUtility;
import org.eclipse.scout.rt.client.ui.action.menu.root.AbstractContextMenu;
import org.eclipse.scout.rt.client.ui.action.menu.root.ITileGridContextMenu;
import org.eclipse.scout.rt.client.ui.tile.ITile;
import org.eclipse.scout.rt.client.ui.tile.ITileGrid;
import org.eclipse.scout.rt.platform.classid.ClassId;

/**
 * The invisible root menu node of tile grid. (internal usage only)
 */
@ClassId("6c1c8e1a-bee2-49fc-8bcc-e2169037fb7e")
public class TileGridContextMenu extends AbstractContextMenu<ITileGrid<? extends ITile>> implements ITileGridContextMenu {

  /**
   * @param owner
   */
  public TileGridContextMenu(ITileGrid<? extends ITile> owner, List<? extends IMenu> initialChildMenus) {
    super(owner, initialChildMenus);
  }

  @Override
  protected void initConfig() {
    super.initConfig();
    // set active filter
    setCurrentMenuTypes(MenuUtility.getMenuTypesForTilesSelection(getContainer().getSelectedTiles()));
    calculateLocalVisibility();
  }

  @Override
  public void callOwnerValueChanged() {
    handleOwnerValueChanged();
  }

  protected void handleOwnerValueChanged() {
    if (getContainer() != null) {
      final List<? extends ITile> ownerValue = getContainer().getSelectedTiles();
      setCurrentMenuTypes(MenuUtility.getMenuTypesForTilesSelection(ownerValue));
      visit(new MenuOwnerChangedVisitor(ownerValue, getCurrentMenuTypes()), IMenu.class);
      calculateLocalVisibility();
    }
  }

  @Override
  protected void handleOwnerPropertyChanged(PropertyChangeEvent evt) {
    super.handleOwnerPropertyChanged(evt);
    if (evt.getPropertyName() == ITileGrid.PROP_SELECTED_TILES) {
      handleOwnerValueChanged();
    }
    // FIXME [7.1] CGU tiles necessary to handle tile update events as done in table?
  }

}
