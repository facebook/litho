// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#include "FlexItem.h"

#include <cassert>

auto facebook::flexlayout::algo::FlexItem::crossSizeRange(
    const bool isMainAxisRow,
    const Float availableInnerCrossDim,
    const AlignItems align,
    const bool isExactCrossDim,
    const bool isSingleLineContainer,
    const FlexDirection crossAxis,
    const Float availableInnerWidth) const -> Range {
  assert(std::isfinite(targetMainSize));

  const auto crossSize = isMainAxisRow ? resolvedHeight : resolvedWidth;
  const auto resolvedCrossSize = crossSize.resolve(availableInnerCrossDim);

  // Try to determine the exact cross size
  const auto exactCrossSize = [&]() {
    // Derived from aspect ratio
    const auto ratio = flexItemStyle.aspectRatio;
    if (ratio > 0) {
      return isMainAxisRow ? targetMainSize / ratio : targetMainSize * ratio;
    }

    // Cannot resolve percentages if the cross dimension of the container is not
    // known
    if (crossSize.unit == Unit::Percent && !isExactCrossDim) {
      return NAN;
    }

    // Exact specified cross size
    if (isDefined(resolvedCrossSize)) {
      return resolvedCrossSize;
    }

    // Derived from align-items: stretch
    const auto noAutoMarginsOnCrossAxis =
        flexItemStyle.getMargin(getLeadingEdge(crossAxis)).unit != Unit::Auto &&
        flexItemStyle.getMargin(getTrailingEdge(crossAxis)).unit != Unit::Auto;
    if (isExactCrossDim && isSingleLineContainer &&
        align == AlignItems::Stretch && noAutoMarginsOnCrossAxis) {
      return availableInnerCrossDim -
          flexItemStyle.getMarginForAxis(crossAxis, availableInnerWidth);
    }

    // If we are here, there is no exact size for this item
    return NAN;
  }();

  const auto minCross =
      isMainAxisRow ? flexItemStyle.minHeight : flexItemStyle.minWidth;
  const auto maxCross =
      isMainAxisRow ? flexItemStyle.maxHeight : flexItemStyle.maxWidth;
  const auto resolvedMinCross = minCross.resolve(availableInnerCrossDim);
  const auto resolvedMaxCross = maxCross.resolve(availableInnerCrossDim);

  // The min / max constraints are applied differently depending on whether the
  // cross size is exact or not
  if (isDefined(exactCrossSize)) {
    // If the cross size is exact, apply min / max constraints...
    const auto usedMinCrossSize =
        isDefined(resolvedMinCross) ? resolvedMinCross : 0.0f;
    const auto usedMaxCrossSize = isDefined(resolvedMaxCross)
        ? resolvedMaxCross
        : std::numeric_limits<Float>::infinity();
    const auto usedCrossSize =
        std::max(std::min(exactCrossSize, usedMaxCrossSize), usedMinCrossSize);

    // ...and produce a single value range
    return {usedCrossSize, usedCrossSize};
  }

  // From https://www.w3.org/TR/css-flexbox-1/#algo-cross-item:
  // "Determine the hypothetical cross size of each item by performing layout
  // with the used main size and the available space, treating auto as
  // fit-content."
  //
  // The exact cross size isn't known, measure the item with a
  // range from 0 to size available on the cross axis (fit-content in CSS
  // terms)...
  const auto tentativeMaxCrossSize = availableInnerCrossDim <= 0
      ? NAN
      : availableInnerCrossDim -
          flexItemStyle.getMarginForAxis(crossAxis, availableInnerWidth);

  // ... applying item's own size constraints if present
  const auto usedMinCrossSize = [&]() -> Float {
    if (utils::isDefined(resolvedMinCross)) {
      return resolvedMinCross;
    }
    return isUndefined(availableInnerCrossDim) ? NAN : 0.0f;
  }();
  const auto usedMaxCrossSize = [&]() -> Float {
    if (utils::isDefined(resolvedMaxCross)) {
      // Item has its own max cross size
      if (utils::isDefined(tentativeMaxCrossSize)) {
        // See which is smaller (the container cross size or the max size of the
        // item) and return it
        return std::min(resolvedMaxCross, tentativeMaxCrossSize);
      }
      // Container cross size is not defined; use item's own max size
      return resolvedMaxCross;
    }
    // Item doesn't have its own max size; use the container cross size (even if
    // undefined)
    return tentativeMaxCrossSize;
  }();

  return {usedMinCrossSize, usedMaxCrossSize};
}
