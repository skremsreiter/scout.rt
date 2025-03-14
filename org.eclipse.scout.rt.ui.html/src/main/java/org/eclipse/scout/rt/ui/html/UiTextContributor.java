/*
 * Copyright (c) 2010-2021 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
package org.eclipse.scout.rt.ui.html;

import java.util.Arrays;
import java.util.Set;

public class UiTextContributor implements IUiTextContributor {

  @Override
  public void contributeUiTextKeys(Set<String> textKeys) {
    textKeys.addAll(Arrays.asList(
        // From org.eclipse.scout.rt.shared
        "Cancel",
        "CancelButton",
        "CloseButton",
        "Column",
        "ColumnSorting",
        "ErrorWhileLoadingData",
        "FormEmptyMandatoryFieldsMessage",
        "FormInvalidFieldsMessage",
        "FormSaveChangesQuestion",
        "ConfirmApplyChanges",
        "GroupBy",
        "NoGrouping",
        "InactiveState",
        "InvalidNumberMessageX",
        "InvalidValueMessageX",
        "NavigationBackward",
        "No",
        "NoButton",
        "Ok",
        "OkButton",
        "Remove",
        "ResetButton",
        "ResetTableColumns",
        "SaveButton",
        "SmartFieldCannotComplete",
        "SmartFieldInactiveRow",
        "SmartFieldMoreThanXRows",
        "SmartFieldNoDataFound",
        "SortBy",
        "TooManyRows",
        "SmartFieldNotUnique",
        "Yes",
        "YesButton",
        "NumberTooLargeMessageXY",
        "NumberTooLargeMessageX",
        "NumberTooSmallMessageXY",
        "NumberTooSmallMessageX",
        "UnsavedChangesTitle",
        "SaveChangesOfSelectedItems",
        "FormsCannotBeSaved",
        "NotAllCheckedFormsCanBeSaved",
        // From org.eclipse.scout.rt.ui.html
        "ui.CodeUndefined",
        "ui.CalendarToday",
        "ui.CalendarDay",
        "ui.CalendarWorkWeek",
        "ui.CalendarWeek",
        "ui.CalendarCalendarWeek",
        "ui.CalendarMonth",
        "ui.CalendarYear",
        "ui.CannotInsertTextTooLong",
        "ui.PastedTextTooLong",
        "ui.CollapseAll",
        "ui.Column",
        "ui.addColumn",
        "ui.removeColumn",
        "ui.changeColumn",
        "ui.BooleanColumnGroupingTrue",
        "ui.BooleanColumnGroupingFalse",
        "ui.BooleanColumnGroupingMixed",
        "ui.InvalidDate",
        "ui.EmptyCell",
        "ui.ExpandAll",
        "ui.FilterBy_",
        "ui.SearchFor_",
        "ui.TableRowCount0",
        "ui.TableRowCount1",
        "ui.TableRowCount",
        "ui.TileView",
        "ui.NumRowSelected",
        "ui.NumRowsSelected",
        "ui.NumRowSelectedMin",
        "ui.NumRowsSelectedMin",
        "ui.NumRowsRendered",
        "ui.NumRowFiltered",
        "ui.NumRowsFiltered",
        "ui.NumRowFilteredBy",
        "ui.NumRowsFilteredBy",
        "ui.NumRowFilteredMin",
        "ui.NumRowsFilteredMin",
        "ui.RemoveFilter",
        "ui.NumRowLoaded",
        "ui.NumRowsLoaded",
        "ui.CountOfApproxTotal",
        "ui.NumRowLoadedMin",
        "ui.NumRowsLoadedMin",
        "ui.ReloadData",
        "ui.LoadNData",
        "ui.LoadAllData",
        "ui.Reload",
        "ui.showEveryDate",
        "ui.OtherValues",
        "ui.Count",
        "ui.ConnectionInterrupted",
        "ui.ConnectionReestablished",
        "ui.Reconnecting_",
        "ui.SelectAll",
        "ui.SelectAllFilter",
        "ui.SelectNone",
        "ui.SelectNoneFilter",
        "ui.ServerError",
        "ui.SessionTimeout",
        "ui.SessionExpiredMsg",
        "ui.UnsafeUpload",
        "ui.UnsafeUploadMsg",
        "ui.RejectedUpload",
        "ui.RejectedUploadMsg",
        "ui.Move",
        "ui.toBegin",
        "ui.forward",
        "ui.backward",
        "ui.toEnd",
        "ui.ascending",
        "ui.Copy",
        "ui.CopyToClipboardSuccessStatus",
        "ui.CopyToClipboardFailedStatus",
        "ui.descending",
        "ui.ascendingAdditionally",
        "ui.descendingAdditionally",
        "ui.Sum",
        "ui.Total",
        "ui.overEverything",
        "ui.overSelection",
        "ui.Coloring",
        "ui.from",
        "ui.fromRedToGreen",
        "ui.fromGreenToRed",
        "ui.withBarChart",
        "ui.remove",
        "ui.add",
        "ui.Filter",
        "ui.FilterInfoXOfY",
        "ui.FilterInfoCount",
        "ui.FreeText",
        "ui.NumberRange",
        "ui.DateRange",
        "ui.Up",
        "ui.Continue",
        "ui.Ignore",
        "ui.ErrorCodeX",
        "ui.InternalUiErrorMsg",
        "ui.UiInconsistentMsg",
        "ui.UnexpectedProblem",
        "ui.InternalProcessingErrorMsg",
        "ui.InvalidUriMsg",
        "ui.PleaseWait_",
        "ui.ShowAllNodes",
        "ui.CW",
        "ui.ChooseFile",
        "ui.ChooseFiles",
        "ui.Upload",
        "ui.Browse",
        "ui.FromXToY",
        "ui.to",
        "ui.FileSizeLimitTitle",
        "ui.FileSizeLimit",
        "ui.ClipboardTimeoutTitle",
        "ui.ClipboardTimeout",
        "ui.PopupBlockerDetected",
        "ui.OpenManually",
        "ui.FileChooserHint",
        "ui.Outlines",
        "ui.NetworkError",
        "ui.Grouping",
        "ui.Hierarchy",
        "ui.additionally",
        "ui.groupingApply",
        "ui.Average",
        "ui.Minimum",
        "ui.Maximum",
        "ui.Aggregation",
        "ui.LoadingPopupWindow",
        "ui.Active",
        "ui.Inactive",
        "ui.All",
        "ui.CloseAllTabs",
        "ui.CloseOtherTabs"));
  }
}
