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

import org.eclipse.scout.rt.client.ui.basic.table.columns.IStringColumn;
import org.eclipse.scout.rt.client.ui.form.fields.stringfield.IStringField;

/**
 * @since 3.9.0
 */
public class StringColumnToFieldPropertyDelegator extends ColumnToFormFieldPropertyDelegator<IStringColumn, IStringField> {

  public StringColumnToFieldPropertyDelegator(IStringColumn sender, IStringField receiver) {
    super(sender, receiver);
  }

  @Override
  public void init() {
    super.init();

    getReceiver().setInputMasked(getSender().isInputMasked());
    getReceiver().setFormat(getSender().getDisplayFormat());
    getReceiver().setWrapText(getSender().isTextWrap());
  }

}
