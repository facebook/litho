/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#pragma once

#include "YGMacros.h"

YG_EXTERN_C_BEGIN

#define YGAlignCount 8
typedef YG_ENUM_BEGIN(YGAlign) {
  YGAlignAuto,
  YGAlignFlexStart,
  YGAlignCenter,
  YGAlignFlexEnd,
  YGAlignStretch,
  YGAlignBaseline,
  YGAlignSpaceBetween,
  YGAlignSpaceAround,
} YG_ENUM_END(YGAlign);

#define YGDimensionCount 2
typedef YG_ENUM_BEGIN(YGDimension) {
  YGDimensionWidth,
  YGDimensionHeight,
} YG_ENUM_END(YGDimension);

#define YGDirectionCount 3
typedef YG_ENUM_BEGIN(YGDirection) {
  YGDirectionInherit,
  YGDirectionLTR,
  YGDirectionRTL,
} YG_ENUM_END(YGDirection);

#define YGDisplayCount 2
typedef YG_ENUM_BEGIN(YGDisplay) {
  YGDisplayFlex,
  YGDisplayNone,
} YG_ENUM_END(YGDisplay);

#define YGEdgeCount 9
typedef YG_ENUM_BEGIN(YGEdge) {
  YGEdgeLeft,
  YGEdgeTop,
  YGEdgeRight,
  YGEdgeBottom,
  YGEdgeStart,
  YGEdgeEnd,
  YGEdgeHorizontal,
  YGEdgeVertical,
  YGEdgeAll,
} YG_ENUM_END(YGEdge);

