// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#pragma once

#include "Dimension.h"
#include "FlexItemStyle.h"

namespace facebook {
namespace flexlayout {
namespace algo {

using namespace facebook::flexlayout;
using namespace facebook::flexlayout::utils;

class FlexItem {
 public:
  size_t index;
  const FlexItemStyleBase& flexItemStyle;
  // TODO T68413071 Use Aggregrate Initialization
  Dimension resolvedWidth = Dimension();
  Dimension resolvedHeight = Dimension();
  float computedFlexBasis = 0;
  Float targetMainSize = NAN;

  explicit FlexItem(
      size_t index,
      const FlexItemStyleBase& flexItemStyleValue,
      Dimension width,
      Dimension height)
      : index(index),
        flexItemStyle(flexItemStyleValue),
        resolvedWidth(width),
        resolvedHeight(height) {}

  /**
  Returns the range of sizes along the cross axis that must be used when
  measuring this item.

  Preconditions:
    - The used size along the main axis ( \c targetMainSize) is determined
   */
  auto crossSizeRange(
      bool isMainAxisRow,
      Float availableInnerCrossDim,
      AlignItems align,
      bool isExactCrossDim,
      bool isSingleLineContainer,
      FlexDirection crossAxis,
      Float availableInnerWidth) const -> Range;
};

} // namespace algo
} // namespace flexlayout
} // namespace facebook
