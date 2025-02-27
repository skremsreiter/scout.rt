/*
 * Copyright (c) 2010-2020 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
import {ObjectFactory, strings, styles} from '@eclipse-scout/core';
import $ from 'jquery';
import {Chart, AbstractChartRenderer} from '../index';

export default class AbstractSvgChartRenderer extends AbstractChartRenderer {

  constructor(chart) {
    super(chart);
    this.chartBox = {};
    this.labelBox = {};

    // Clipping and masking
    this.clipId = 'Clip-' + ObjectFactory.get().createUniqueId();
    this.maskId = 'Mask-' + ObjectFactory.get().createUniqueId();

    this.suppressLegendBox = false;
  }

  static FONT_SIZE_SMALLEST = 'smallestFont';
  static FONT_SIZE_SMALL = 'smallFont';
  static FONT_SIZE_MIDDLE = 'middleFont';
  static FONT_SIZE_BIG = 'bigFont';

  _render() {
    if (!this.$svg) {
      this.$svg = this.chart.$container.appendSVG('svg', 'chart-svg');
    }
    this.firstOpaqueBackgroundColor = styles.getFirstOpaqueBackgroundColor(this.$svg);
    this.svgHeight = this.$svg.height();
    this.svgWidth = this.$svg.width();
    // This works, because CSS specifies 100% width/height
    this.height = this.svgHeight;
    this.width = this.svgWidth;
    this._initChartBox();
    if (this._useFontSizeBig()) {
      this.$svg.addClass(AbstractSvgChartRenderer.FONT_SIZE_BIG);
    } else if (this._useFontSizeMiddle()) {
      this.$svg.addClass(AbstractSvgChartRenderer.FONT_SIZE_MIDDLE);
    } else if (this._useFontSizeSmall()) {
      this.$svg.addClass(AbstractSvgChartRenderer.FONT_SIZE_SMALL);
    } else if (this._useFontSizeSmallest()) {
      this.$svg.addClass(AbstractSvgChartRenderer.FONT_SIZE_SMALLEST);
    }
    if (!this.$svg.isAttached()) {
      // user navigated away. do not try to render->error
      return;
    }
    this._renderInternal();
  }

  _renderInternal() {
    // Override in subclasses
  }

  _useFontSizeBig() {
    return false;
  }

  _useFontSizeMiddle() {
    return false;
  }

  _useFontSizeSmall() {
    return false;
  }

  _useFontSizeSmallest() {
    return false;
  }

  remove(requestAnimation, afterRemoveFunc) {
    if (this.rendered && !this.chartAnimationStopping) {
      this.chartAnimationStopping = true;
      this.$svg.children().stop(true, false);
      this.chartAnimationStopping = false;
    }
    super.remove(requestAnimation, afterRemoveFunc);
  }

  _remove(afterRemoveFunc) {
    // this function is called directly from renderers after all removal animations are done
    // however, other animations may have been queued in the meantime (e.g. in case the chart was removed, then added (+animation queued), and then removed again)
    if (this.rendered) {
      this.$svg.children().stop(true, false);
      // need to check again, as stop() may have triggered a chart removal and this may not be rendered anymore
      if (this.rendered) {
        this.$svg.remove();
        this.$svg = null;
      }
    }
    this.rendered = false;
    afterRemoveFunc && afterRemoveFunc(this.chartAnimationStopping);
  }

  /**
   * For all parameters: use null when parameter is not used or set by a chart type.
   *
   * @param xIndex number
   * @param datasetIndex number
   */
  _createClickObject(xIndex, datasetIndex) {
    return {
      xIndex: xIndex,
      dataIndex: xIndex,
      datasetIndex: datasetIndex
    };
  }

  _measureText(text, legendLabelClass) {
    let $label = this.$svg.appendSVG('text', legendLabelClass)
      .attr('x', 0)
      .attr('y', 0)
      .attr('visibility', 'hidden')
      .text(text);
    let textBounds;
    try {
      // Firefox throws error when node is not in dom(already removed by navigating away). all other browser returns a boundingbox with 0
      textBounds = $label[0].getBBox();
    } catch (e) {
      return {
        height: 0,
        width: 0
      };
    }
    $label.remove();

    return textBounds;
  }

  _renderLine(x1, y1, x2, y2, lineClass) {
    let $line = this.$svg.appendSVG('line', lineClass)
      .attr('x1', x1).attr('y1', y1)
      .attr('x2', x2).attr('y2', y2);
    if (this.animationDuration) {
      $line
        .attr('opacity', 0)
        .animateSVG('opacity', 1, this.animationDuration, null, true);
    }
    return $line;
  }

  _renderLineLabel(x, y, label, labelClass, drawBackground) {
    let $label = this.$svg.appendSVG('text', labelClass ? labelClass : 'line-label')
      .attr('x', x).attr('y', y)
      .text(label);

    if (drawBackground) {
      $label.attr('mask', 'url(#' + this.maskId + ')');

      let $background = this.$svg.appendSVG('text', labelClass ? labelClass + ' background' : 'line-label-background')
        .attr('x', x).attr('y', y)
        .attr('clip-path', 'url(#' + this.clipId + ')')
        .text(label);

      $label.data('$background', $background);

      if (this.animationDuration) {
        $background
          .attr('opacity', 0)
          .animateSVG('opacity', 1, this.animationDuration, null, true);
      }
    }

    if (this.animationDuration) {
      $label
        .attr('opacity', 0)
        .animateSVG('opacity', 1, this.animationDuration, null, true);
    }
    return $label;
  }

  _initChartBox() {
    this.chartBox = {
      width: this.width,
      height: this.height,
      xOffset: 0,
      yOffset: 0,
      mX: function() {
        return this.xOffset + (this.width / 2);
      },
      mY: function() {
        return this.yOffset + (this.height / 2);
      }
    };
  }

  _createAnimationObjectWithTabIndexRemoval(animationFunc, duration) {
    return {
      step: function(now, fx) {
        try {
          animationFunc.bind(this)(now, fx);
        } catch (e) {
          // prevent logging thousands of exceptions (1 per animation step) by stopping and clearing the queue
          $(this).stop(true, false);
          throw e;
        }
      },
      duration: duration ? duration : Chart.DEFAULT_ANIMATION_DURATION,
      complete: function() {
        $(this).removeAttr('tabindex');
      }
    };
  }

  _addClipping(cssClass) {
    // add clip and mask paths for all relevant objects
    let $clip = this.$svg
      .appendSVG('clipPath');
    $clip[0].id = this.clipId;

    let $mask = this.$svg.appendSVG('mask');
    $mask.appendSVG('rect')
      .attr('x', 0)
      .attr('y', 0)
      .attr('width', '100%')
      .attr('height', '100%')
      .attr('fill', 'white');
    $mask[0].id = this.maskId;

    this.chart.$container.find('.' + cssClass).each(function(i) {
      this.id = 'ClipMask-' + ObjectFactory.get().createUniqueId();
      $clip.appendSVG('use').attrXLINK('href', '#' + this.id);
      $mask.appendSVG('use').attrXLINK('href', '#' + this.id);
    });
  }

  _renderWireLegend(text, legendPositions, className, drawBackgroundBox) {
    let legend = {
      detachFunc: () => {
      },
      attachFunc: () => {
      },
      removeFunc: () => {
      }
    };
    if (!this.chart.config.options.tooltips.enabled) {
      return legend;
    }
    let padding = 5,
      $background,
      backgroundWidth = 0,
      lineHeight = 17,
      backgroundHeight = lineHeight;

    if (drawBackgroundBox) {
      $background = this.$svg.appendSVG('rect', 'wire-legend-background-box')
        .attr('opacity', '1');
    }

    let positions = legendPositions;

    // draw and measure label

    let $legend,
      lengthLegend = 0,
      horizontalSpace = 0;
    if (positions.h === -1) {
      horizontalSpace = positions.x2 - 2 * padding;
    } else {
      horizontalSpace = this.width - positions.x2 - 2 * padding;
    }

    if (Array.isArray(text)) {
      for (let i = 0; i < text.length; i++) {
        let posIndex = text.length - i - 1;
        let yPos = positions.y2 + positions.v * padding - lineHeight * posIndex - padding * posIndex;
        let $line = this._renderLineLabel(positions.x2 + padding, yPos, strings.truncateText(text[i], horizontalSpace, this._measureText.bind(this)), '', drawBackgroundBox);
        $line.addClass(className);
        lengthLegend = Math.max(lengthLegend, $line[0].getComputedTextLength());
        if (i === 0) {
          $legend = $line;
        } else {
          if ($legend.data('lines')) {
            $legend.data('lines').push($line);
          } else {
            $legend.data('lines', [$line]);
          }
        }
      }
    } else {
      $legend = this._renderLineLabel(positions.x2 + padding, positions.y2 + positions.v * padding, strings.truncateText(text, horizontalSpace, this._measureText.bind(this)), '', drawBackgroundBox);
      $legend.addClass(className);
      lengthLegend = $legend[0].getComputedTextLength();
    }
    backgroundWidth = lengthLegend + 2 * padding;

    if (legendPositions.autoPosition) {
      positions = legendPositions.posFunc.call(this, backgroundWidth, backgroundHeight);
      // adjust legend
      $legend.attr('x', positions.x2 + padding);
      $legend.attr('y', positions.y2 + positions.v * padding);
    }

    // fix layout depending on orientation of legend
    if (positions.h === -1) {
      $legend.attr('x', positions.x2 - padding - lengthLegend);
      $legend.css('text-anchor', 'left');
      if ($legend.data('lines')) {
        $legend.data('lines').forEach($line => {
          $line.attr('x', positions.x2 - padding - lengthLegend);
          $line.css('text-anchor', 'left');
        });
      }
    } else {
      $legend.attr('x', positions.x2 + padding);
      $legend.css('text-anchor', 'right');
      if ($legend.data('lines')) {
        $legend.data('lines').forEach($line => {
          $line.attr('x', positions.x2 + padding);
          $line.css('text-anchor', 'right');
        });
      }
    }
    if (positions.v === 1) {
      if ($legend.data('lines')) {
        $legend.data('lines').forEach(($line, i) => {
          $line.attr('y', positions.x2 - padding - lengthLegend);
          let index = 1 + i;
          $line.attr('y', positions.y2 + positions.v * padding + lineHeight * index + padding * (index + 1));
        });
      }
      $legend.attr('dy', '0.7em');
    } else {
      if ($legend.data('lines')) {
        let index = $legend.data('lines').length;
        $legend.attr('y', positions.y2 + positions.v * padding - lineHeight * index - padding * index);
        $legend.data('lines').forEach(($line, i) => {
          index = $legend.data('lines').length - 1 - i;
          $line.attr('y', positions.y2 + positions.v * padding - lineHeight * index - padding * index);
        });
      }
    }

    // align background text
    $legend.add($legend.data('lines')).each((i, line) => {
      let $line = $(line),
        $background = $line.data('$background');
      if ($background) {
        $background.attr('x', $line.attr('x'));
        $background.attr('y', $line.attr('y'));
        $background.css('text-anchor', $line.css('text-anchor'));
        $background.attr('dy', $line.attr('dy'));
      }
    });

    // draw lines, if wished
    let wires = [];
    if (positions.x1 > 0 && positions.y1 > 0) {
      wires.push(this._renderLine(positions.x1, positions.y1, positions.x2, positions.y2, 'label-line'));
      wires.push(this._renderLine(positions.x2, positions.y2, positions.x2 + positions.h * (lengthLegend + 2 * padding), positions.y2, 'label-line'));
    }
    $legend.data('wires', wires);

    let $svg = this.$svg;
    legend.detachFunc = () => {
      $legend.data('wires').forEach($wire => {
        $wire.detach();
      });
      if ($legend.data('lines')) {
        $legend.data('lines').forEach($line => {
          if ($line.data('$background')) {
            $svg.append($line.data('$background'));
          }
          $line.detach();
        });
      }
      if ($legend.data('$background')) {
        $legend.data('$background').remove();
      }
      $legend.detach();
    };

    legend.attachFunc = () => {
      $svg.append($legend);
      if ($legend.data('$background')) {
        $svg.append($legend.data('$background'));
      }
      $svg.append($legend.data('wires'));
      if ($legend.data('lines')) {
        $legend.data('lines').forEach($line => {
          $svg.append($line);
          if ($line.data('$background')) {
            $svg.append($line.data('$background'));
          }
        });
      }
    };

    legend.removeFunc = () => {
      $legend.data('wires').forEach($wire => {
        $wire.remove();
      });
      if ($legend.data('lines')) {
        $legend.data('lines').forEach($line => {
          if ($line.data('$background')) {
            $line.data('$background').remove();
          }
          $line().remove();
        });
      }
      if ($legend.data('$background')) {
        $legend.data('$background').remove();
      }
      $legend.remove();
    };
    legend.$field = $legend;
    return legend;
  }
}
