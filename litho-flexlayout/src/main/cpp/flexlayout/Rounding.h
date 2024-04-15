// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#include "FlexLayoutMacros.h"

namespace facebook {
namespace flexlayout {
namespace algo {

FLEX_LAYOUT_EXPORT auto RoundValueToPixelGrid(
    double value,
    double pointScaleFactor,
    bool forceCeil,
    bool forceFloor) -> float;

} // namespace algo
} // namespace flexlayout
} // namespace facebook
