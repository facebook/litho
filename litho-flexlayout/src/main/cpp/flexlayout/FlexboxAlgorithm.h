// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#pragma once

#include <algorithm>
#include <limits>
#include <numeric>
#include <vector>
#include "FlexBoxStyle.h"
#include "FlexItem.h"
#include "FlexItemStyle.h"
#include "FlexLine.h"
#include "LayoutOutput.h"
#include "Rounding.h"
#include "Type.h"

namespace facebook {
namespace flexlayout {
namespace algo {

using namespace facebook::flexlayout::style;

// A non-template abstract base class implementing the Flexbox algorithm. All
// code here is independent of the concrete types for the measure data and the
// measure result, so it doesn't have to be exposed in the header file.
class AlgorithmBase {
 public:
  AlgorithmBase(
      const FlexBoxStyle& node,
      const Float minWidth,
      const Float maxWidth,
      const Float minHeight,
      const Float maxHeight,
      const Float ownerWidth)
      : node(node),
        minWidth(minWidth),
        maxWidth(maxWidth),
        minHeight(minHeight),
        maxHeight(maxHeight),
        ownerWidth(ownerWidth) {}

  // Disallow copying because of storing references
  AlgorithmBase(const AlgorithmBase&) = delete;
  auto operator=(const AlgorithmBase&) -> AlgorithmBase& = delete;

  AlgorithmBase(AlgorithmBase&&) = default;

  virtual ~AlgorithmBase() = default;

  // Calculates the layout and stores the result in the layout output provided
  // by the derived class
  void calculateLayout();

 private:
  // Returns the number of child items
  virtual auto numberOfChildren() const -> std::size_t = 0;
  // Returns an immutable reference to the style of an item with a given index
  virtual auto itemStyleAt(const std::size_t idx) const
      -> const FlexItemStyleBase& = 0;
  // Returns a mutable reference to the layout of the flex container
  virtual auto containerLayout() -> layoutoutput::LayoutOutputBase& = 0;
  // Returns a mutable reference to the layout of an item with a given index
  virtual auto itemLayoutAt(const std::size_t idx)
      -> layoutoutput::LayoutOutputBase::Child& = 0;

  // Ensures the layout for an item with a given index is up to date,
  // remeasuring it if needed
  virtual void ensureItemLayoutAt(
      const std::size_t idx,
      const Float minWidth,
      const Float maxWidth,
      const Float minHeight,
      const Float maxHeight,
      const Float ownerWidth,
      const Float ownerHeight) = 0;

  // Returns the position of the custom baseline for an item with a given index
  // if it defines a custom baseline function
  virtual auto customBaselineForItemAt(
      const std::size_t idx,
      const Float width,
      const Float height) const -> Float = 0;

  const FlexBoxStyle& node;
  Float minWidth;
  Float maxWidth;
  Float minHeight;
  Float maxHeight;
  Float ownerWidth;
};

// A class template derived from \c AlgorithmBase that provides storage for
// concrete types of measure data and measure results
template <typename MeasureData, typename Result>
class FLEX_LAYOUT_EXPORT Algorithm : public AlgorithmBase {
 public:
  layoutoutput::LayoutOutput<Result> nodeLayoutOutput;

 public:
  Algorithm(
      const FlexBoxStyle& flexBoxStyle,
      const std::vector<FlexItemStyle<MeasureData, Result>>& children,
      const Float minWidth,
      const Float maxWidth,
      const Float minHeight,
      const Float maxHeight,
      const Float ownerWidth)
      : AlgorithmBase(
            flexBoxStyle,
            minWidth,
            maxWidth,
            minHeight,
            maxHeight,
            ownerWidth),
        children(children) {
    nodeLayoutOutput.children.reserve(children.size());
    for (auto i = std::size_t{0}; i < children.size(); ++i) {
      nodeLayoutOutput.children.push_back(
          typename layoutoutput::LayoutOutput<Result>::Child());
    }
  }

 private:
  auto numberOfChildren() const -> std::size_t override final {
    return children.size();
  }

  auto itemStyleAt(const std::size_t idx) const
      -> const FlexItemStyleBase& override final {
    return children[idx];
  }

  auto containerLayout() -> layoutoutput::LayoutOutputBase& override final {
    return nodeLayoutOutput;
  }

  auto itemLayoutAt(const std::size_t idx)
      -> layoutoutput::LayoutOutputBase::Child& override final {
    return nodeLayoutOutput.children[idx];
  }

  void ensureItemLayoutAt(
      const std::size_t idx,
      const Float minWidth,
      const Float maxWidth,
      const Float minHeight,
      const Float maxHeight,
      const Float ownerWidth,
      const Float ownerHeight) override final {
    const auto measureParams =
        layoutoutput::MeasureParams{minWidth, maxWidth, minHeight, maxHeight};
    if (nodeLayoutOutput.children[idx].canBeReusedFor(measureParams)) {
      return;
    }
    auto measureOutput = children[idx].measureFunction(
        children[idx].measureData,
        minWidth,
        maxWidth,
        minHeight,
        maxHeight,
        ownerWidth,
        ownerHeight);
    nodeLayoutOutput.children[idx].setMeasureOutput(
        std::move(measureOutput), measureParams);
  }

  auto customBaselineForItemAt(
      const std::size_t idx,
      const Float width,
      const Float height) const -> Float override final {
    if (const auto baselineFunc = children[idx].baselineFunction) {
      return baselineFunc(children[idx].measureData, width, height);
    }
    return UNDEFINED;
  }

  const std::vector<FlexItemStyle<MeasureData, Result>>& children;
};
} // namespace algo
} // namespace flexlayout
} // namespace facebook
