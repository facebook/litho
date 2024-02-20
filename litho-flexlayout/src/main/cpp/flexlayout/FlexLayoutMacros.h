// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#pragma once
#include <cfloat>
#include <limits>

#define FLEX_LAYOUT_EXPORT __attribute__((visibility("default")))
constexpr float UNDEFINED = std::numeric_limits<float>::quiet_NaN();
