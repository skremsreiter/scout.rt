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

/**
 * This class is used differently in online and JS-only case. In the online case we only have instances
 * of Page in an outline. The server sets the property <code>nodeType</code> which is used to distinct
 * between pages with tables and pages with nodes in some cases. In the JS only case, Page is an abstract
 * class and is never instantiated directly, instead we always use subclasses of PageWithTable or PageWithNodes.
 * Implementations of these classes contain code which loads table data or child nodes.
 *
 * @class
 * @extends scout.TreeNode
 */
scout.Page = function() {
  scout.Page.parent.call(this);

  /**
   * This property is set by the server, see: JsonOutline#putNodeType.
   */
  this.nodeType;
  this.detailTable;
  this.detailTableVisible = true;
  this.detailForm;
  this.detailFormVisible = true;
  this.detailFormVisibleByUi = true;

  /**
   * This property contains the class-name of the form to be instantiated, when createDetailForm() is called.
   */
  this.detailFormType = null;
  this.tableStatusVisible = true;
  /**
   * True to select the page linked with the selected row when the row was selected. May be useful on touch devices.
   */
  this.drillDownOnRowClick = false;
};
scout.inherits(scout.Page, scout.TreeNode);

/**
 * This enum defines a node-type. This is basically used for the online case where we only have instances
 * of scout.Page, but never instances of PageWithTable or PageWithNodes. The server simply sets a nodeType
 * instead.
 *
 * @type {{NODES: string, TABLE: string}}
 */
scout.Page.NodeType = {
  NODES: 'nodes',
  TABLE: 'table'
};

/**
 * Override this function to return a detail form which is displayed in the outline when this page is selected.
 * The default impl. returns null.
 */
scout.Page.prototype.createDetailForm = function() {
  return null;
};

/**
 * @override TreeNode.js
 */
scout.Page.prototype._init = function(model) {
  scout.Page.parent.prototype._init.call(this, model);
  this._internalInitTable();
  this._internalInitDetailForm();
};

scout.Page.prototype._internalInitTable = function() {
  var table = this.detailTable;
  if (table) {
    // this case is used for Scout classic
    table = this.getOutline()._createChild(table);
  } else {
    table = this._createTable();
  }

  this.setDetailTable(table);
};

scout.Page.prototype._internalInitDetailForm = function() {
  var detailForm = this.detailForm;
  if (detailForm) {
    detailForm = this.getOutline()._createChild(detailForm);
  }

  this.setDetailForm(detailForm);
};

/**
 * Override this function to create the internal table. Default impl. returns null.
 */
scout.Page.prototype._createTable = function() {
  return null;
};

/**
 * Override this function to initialize the internal (detail) table. Default impl. delegates
 * <code>filter</code> events to the outline mediator.
 */
scout.Page.prototype._initTable = function(table) {
  table.on('filter', this._onTableFilter.bind(this));
  if (this.drillDownOnRowClick) {
    table.on('rowClick', this._onTableRowClick.bind(this));
    table.setMultiSelect(false);
  }
};

scout.Page.prototype._ensureDetailForm = function() {
  if (this.detailForm) {
    return;
  }
  var form = this.createDetailForm();
  if (form && !form.displayParent) {
    form.setDisplayParent(this.getOutline());
  }
  this.setDetailForm(form);
};

// see Java: AbstractPage#pageActivatedNotify
scout.Page.prototype.activate = function() {
  this._ensureDetailForm();
};

// see Java: AbstractPage#pageDeactivatedNotify
scout.Page.prototype.deactivate = function() {
};

/**
 * @returns {scout.Outline} the tree / outline / parent instance. it's all the same,
 *     but it's more intuitive to work with the 'outline' when we deal with pages.
 */
scout.Page.prototype.getOutline = function() {
  return this.parent;
};

/**
 * @returns {Array.<scout.Page>} an array of pages linked with the given rows.
 *   The order of the returned pages will be the same as the order of the rows.
 */
scout.Page.prototype.pagesForTableRows = function(rows) {
  return rows.map(this.pageForTableRow);
};

scout.Page.prototype.pageForTableRow = function(row) {
  if (!row.page) {
    throw new Error('Table-row is not linked to a page');
  }
  return row.page;
};

scout.Page.prototype.setDetailForm = function(form) {
  this.detailForm = form;
};

scout.Page.prototype.setDetailTable = function(table) {
  if (table) {
    this._initTable(table);
    table.setTableStatusVisible(this.tableStatusVisible);
  }
  this.detailTable = table;
};

/**
 * Updates relevant properties from the pages linked with the given rows using the method updatePageFromTableRow and returns the pages.
 *
 * @returns {Array.<scout.Page>} pages linked with the given rows.
 */
scout.Page.prototype.updatePagesFromTableRows = function(rows) {
  return rows.map(function(row) {
    var page = row.page;
    page.updatePageFromTableRow(row);
    return page;
  });
};

/**
 * Updates relevant properties (text, enabled, htmlEnabled) from the page linked with the given row.
 *
 * @returns {scout.Page} page linked with the given row.
 */
scout.Page.prototype.updatePageFromTableRow = function(row) {
  var page = row.page;
  page.enabled = row.enabled;
  page.text = page.computeTextForRow(row);
  if (row.cells.length >= 1) {
    page.htmlEnabled = row.cells[0].htmlEnabled;
    page.cssClass = row.cells[0].cssClass;
  }
  return page;
};

/**
 * This function creates the text property of this page. The default implementation returns the
 * text from the first cell of the given row. It's allowed to ignore the given row entirely, when you override
 * this function.
 *
 * @param {scout.TableRow} row
 */
scout.Page.prototype.computeTextForRow = function(row) {
  var text = '';
  if (row.cells.length >= 1) {
    text = row.cells[0].text;
  }
  return text;
};

/**
 * @returns {object} a page parameter object used to pass to newly created child pages. Sets the parent
 *     to our outline instance and adds optional other properties. Typically you'll pass an
 *     object (entity-key or arbitrary data) to a child page.
 */
scout.Page.prototype._pageParam = function(paramProperties) {
  var param = {
    parent: this.getOutline()
  };
  $.extend(param, paramProperties);
  return param;
};

scout.Page.prototype.reloadPage = function () {
  var outline = this.getOutline();
  if (outline) {
    this.loadChildren();
  }
};

scout.Page.prototype.linkWithRow = function(row) {
  this.row = row;
  row.page = this;
  this.getOutline().trigger('pageRowLink', {
    page: this,
    row: row
  });
};

scout.Page.prototype.unlinkWithRow = function(row) {
  delete this.row;
  delete row.page;
};

scout.Page.prototype._onTableFilter = function(event) {
  this.getOutline().mediator.onTableFilter(event, this);
};

scout.Page.prototype._onTableRowClick = function(event) {
  if (!this.drillDownOnRowClick) {
    return;
  }
  var row = event.row;
  var drillNode = this.pageForTableRow(row);
  this.getOutline().selectNode(drillNode);
  this.detailTable.deselectRow(row);
};
