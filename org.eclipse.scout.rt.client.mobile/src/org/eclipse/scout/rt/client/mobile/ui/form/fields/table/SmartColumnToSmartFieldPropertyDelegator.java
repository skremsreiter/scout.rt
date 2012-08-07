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
package org.eclipse.scout.rt.client.mobile.ui.form.fields.table;

import org.eclipse.scout.rt.client.ui.basic.table.columns.ISmartColumn;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.ISmartField;

/**
 * @since 3.9.0
 */
public class SmartColumnToSmartFieldPropertyDelegator extends ColumnToFormFieldPropertyDelegator<ISmartColumn<?>, ISmartField<?>> {

  public SmartColumnToSmartFieldPropertyDelegator(ISmartColumn<?> sender, ISmartField<?> receiver) {
    super(sender, receiver);
  }

  @Override
  public void init() {
    super.init();

    getReceiver().setCodeTypeClass(getSender().getCodeTypeClass());
    getReceiver().setLookupCall(getSender().getLookupCall());
  }

}
