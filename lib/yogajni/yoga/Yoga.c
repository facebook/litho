/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <string.h>

#include "YGNodeList.h"
#include "Yoga.h"

#ifdef _MSC_VER
#include <float.h>
#ifndef isnan
#define isnan _isnan
#endif

#ifndef __cplusplus
#define inline __inline
#endif

/* define fmaxf if < VC12 */
#if _MSC_VER < 1800
__forceinline const float fmaxf(const float a, const float b) {
  return (a > b) ? a : b;
}
#endif
#endif

typedef struct YGCachedMeasurement {
  float availableWidth;
  float availableHeight;
  YGMeasureMode widthMeasureMode;
  YGMeasureMode heightMeasureMode;

  float computedWidth;
  float computedHeight;
} YGCachedMeasurement;

// This value was chosen based on empiracle data. Even the most complicated
// layouts should not require more than 16 entries to fit within the cache.
#define YG_MAX_CACHED_RESULT_COUNT 16

typedef struct YGLayout {
  float position[4];
  float dimensions[2];
  float margin[6];
  float border[6];
  float padding[6];
  YGDirection direction;

  uint32_t computedFlexBasisGeneration;
  float computedFlexBasis;

  // Instead of recomputing the entire layout every single time, we
  // cache some information to break early when nothing changed
  uint32_t generationCount;
  YGDirection lastParentDirection;

  uint32_t nextCachedMeasurementsIndex;
  YGCachedMeasurement cachedMeasurements[YG_MAX_CACHED_RESULT_COUNT];
  float measuredDimensions[2];

  YGCachedMeasurement cachedLayout;
} YGLayout;

typedef struct YGStyle {
  YGDirection direction;
  YGFlexDirection flexDirection;
  YGJustify justifyContent;
  YGAlign alignContent;
  YGAlign alignItems;
  YGAlign alignSelf;
  YGPositionType positionType;
  YGWrap flexWrap;
  YGOverflow overflow;
  YGDisplay display;
  float flex;
  float flexGrow;
  float flexShrink;
  YGValue flexBasis;
  YGValue margin[YGEdgeCount];
  YGValue position[YGEdgeCount];
  YGValue padding[YGEdgeCount];
  YGValue border[YGEdgeCount];
  YGValue dimensions[2];
  YGValue minDimensions[2];
  YGValue maxDimensions[2];

  // Yoga specific properties, not compatible with flexbox specification
  float aspectRatio;
} YGStyle;

typedef struct YGConfig {
  bool experimentalFeatures[YGExperimentalFeatureCount + 1];
  bool useWebDefaults;
  float pointScaleFactor;
} YGConfig;

typedef struct YGNode {
  YGStyle style;
  YGLayout layout;
  uint32_t lineIndex;

  YGNodeRef parent;
  YGNodeListRef children;

  struct YGNode *nextChild;

  YGMeasureFunc measure;
  YGBaselineFunc baseline;
  YGPrintFunc print;
  YGConfigRef config;
  void *context;

  bool isDirty;
  bool hasNewLayout;

  YGValue const *resolvedDimensions[2];
} YGNode;

#define YG_UNDEFINED_VALUES \
  { .value = YGUndefined, .unit = YGUnitUndefined }

#define YG_AUTO_VALUES \
  { .value = YGUndefined, .unit = YGUnitAuto }

#define YG_DEFAULT_EDGE_VALUES_UNIT                                                   \
  {                                                                                   \
    [YGEdgeLeft] = YG_UNDEFINED_VALUES, [YGEdgeTop] = YG_UNDEFINED_VALUES,            \
    [YGEdgeRight] = YG_UNDEFINED_VALUES, [YGEdgeBottom] = YG_UNDEFINED_VALUES,        \
    [YGEdgeStart] = YG_UNDEFINED_VALUES, [YGEdgeEnd] = YG_UNDEFINED_VALUES,           \
    [YGEdgeHorizontal] = YG_UNDEFINED_VALUES, [YGEdgeVertical] = YG_UNDEFINED_VALUES, \
    [YGEdgeAll] = YG_UNDEFINED_VALUES,                                                \
  }

#define YG_DEFAULT_DIMENSION_VALUES \
  { [YGDimensionWidth] = YGUndefined, [YGDimensionHeight] = YGUndefined, }

#define YG_DEFAULT_DIMENSION_VALUES_UNIT \
  { [YGDimensionWidth] = YG_UNDEFINED_VALUES, [YGDimensionHeight] = YG_UNDEFINED_VALUES, }

#define YG_DEFAULT_DIMENSION_VALUES_AUTO_UNIT \
  { [YGDimensionWidth] = YG_AUTO_VALUES, [YGDimensionHeight] = YG_AUTO_VALUES, }

static const float kDefaultFlexGrow = 0.0f;
static const float kDefaultFlexShrink = 0.0f;
static const float kWebDefaultFlexShrink = 1.0f;

static YGNode gYGNodeDefaults = {
    .parent = NULL,
    .children = NULL,
    .hasNewLayout = true,
    .isDirty = false,
    .resolvedDimensions = {[YGDimensionWidth] = &YGValueUndefined,
                           [YGDimensionHeight] = &YGValueUndefined},

    .style =
        {
            .flex = YGUndefined,
            .flexGrow = YGUndefined,
            .flexShrink = YGUndefined,
            .flexBasis = YG_AUTO_VALUES,
            .justifyContent = YGJustifyFlexStart,
            .alignItems = YGAlignStretch,
            .alignContent = YGAlignFlexStart,
            .direction = YGDirectionInherit,
            .flexDirection = YGFlexDirectionColumn,
            .overflow = YGOverflowVisible,
            .display = YGDisplayFlex,
            .dimensions = YG_DEFAULT_DIMENSION_VALUES_AUTO_UNIT,
            .minDimensions = YG_DEFAULT_DIMENSION_VALUES_UNIT,
            .maxDimensions = YG_DEFAULT_DIMENSION_VALUES_UNIT,
            .position = YG_DEFAULT_EDGE_VALUES_UNIT,
            .margin = YG_DEFAULT_EDGE_VALUES_UNIT,
            .padding = YG_DEFAULT_EDGE_VALUES_UNIT,
            .border = YG_DEFAULT_EDGE_VALUES_UNIT,
            .aspectRatio = YGUndefined,
        },

    .layout =
        {
            .dimensions = YG_DEFAULT_DIMENSION_VALUES,
            .lastParentDirection = (YGDirection) -1,
            .nextCachedMeasurementsIndex = 0,
            .computedFlexBasis = YGUndefined,
            .measuredDimensions = YG_DEFAULT_DIMENSION_VALUES,

            .cachedLayout =
                {
                    .widthMeasureMode = (YGMeasureMode) -1,
                    .heightMeasureMode = (YGMeasureMode) -1,
                    .computedWidth = -1,
                    .computedHeight = -1,
                },
        },
};

static YGConfig gYGConfigDefaults = {
    .experimentalFeatures =
        {
                [YGExperimentalFeatureRounding] = false,
                [YGExperimentalFeatureMinFlexFix] = false,
                [YGExperimentalFeatureWebFlexBasis] = false,
        },
    .useWebDefaults = false,
    .pointScaleFactor = 1.0f
};

static void YGNodeMarkDirtyInternal(const YGNodeRef node);

YGMalloc gYGMalloc = &malloc;
YGCalloc gYGCalloc = &calloc;
YGRealloc gYGRealloc = &realloc;
YGFree gYGFree = &free;

static YGValue YGValueZero = {.value = 0, .unit = YGUnitPoint};

#ifdef ANDROID
#include <android/log.h>
static int YGAndroidLog(YGLogLevel level, const char *format, va_list args) {
  int androidLevel = YGLogLevelDebug;
  switch (level) {
    case YGLogLevelError:
      androidLevel = ANDROID_LOG_ERROR;
      break;
    case YGLogLevelWarn:
      androidLevel = ANDROID_LOG_WARN;
      break;
    case YGLogLevelInfo:
      androidLevel = ANDROID_LOG_INFO;
      break;
    case YGLogLevelDebug:
      androidLevel = ANDROID_LOG_DEBUG;
      break;
    case YGLogLevelVerbose:
      androidLevel = ANDROID_LOG_VERBOSE;
      break;
  }
  const int result = __android_log_vprint(androidLevel, "YG-layout", format, args);
  return result;
}
static YGLogger gLogger = &YGAndroidLog;
#else
static int YGDefaultLog(YGLogLevel level, const char *format, va_list args) {
  switch (level) {
    case YGLogLevelError:
      return vfprintf(stderr, format, args);
    case YGLogLevelWarn:
    case YGLogLevelInfo:
    case YGLogLevelDebug:
    case YGLogLevelVerbose:
    default:
      return vprintf(format, args);
  }
}
static YGLogger gLogger = &YGDefaultLog;
#endif

static inline const YGValue *YGComputedEdgeValue(const YGValue edges[YGEdgeCount],
                                                 const YGEdge edge,
                                                 const YGValue *const defaultValue) {
  YG_ASSERT(edge <= YGEdgeEnd, "Cannot get computed value of multi-edge shorthands");

  if (edges[edge].unit != YGUnitUndefined) {
    return &edges[edge];
  }

  if ((edge == YGEdgeTop || edge == YGEdgeBottom) &&
      edges[YGEdgeVertical].unit != YGUnitUndefined) {
    return &edges[YGEdgeVertical];
  }

  if ((edge == YGEdgeLeft || edge == YGEdgeRight || edge == YGEdgeStart || edge == YGEdgeEnd) &&
      edges[YGEdgeHorizontal].unit != YGUnitUndefined) {
    return &edges[YGEdgeHorizontal];
  }

  if (edges[YGEdgeAll].unit != YGUnitUndefined) {
    return &edges[YGEdgeAll];
  }

  if (edge == YGEdgeStart || edge == YGEdgeEnd) {
    return &YGValueUndefined;
  }

  return defaultValue;
}

static inline float YGResolveValue(const YGValue *const value, const float parentSize) {
  switch (value->unit) {
    case YGUnitUndefined:
    case YGUnitAuto:
      return YGUndefined;
    case YGUnitPoint:
      return value->value;
    case YGUnitPercent:
      return value->value * parentSize / 100.0f;
  }
  return YGUndefined;
}

static inline float YGResolveValueMargin(const YGValue *const value, const float parentSize) {
  return value->unit == YGUnitAuto ? 0 : YGResolveValue(value, parentSize);
}

int32_t gNodeInstanceCount = 0;

WIN_EXPORT YGNodeRef YGNodeNewWithConfig(const YGConfigRef config) {
  const YGNodeRef node = gYGMalloc(sizeof(YGNode));
  YG_ASSERT(node, "Could not allocate memory for node");
  gNodeInstanceCount++;

  memcpy(node, &gYGNodeDefaults, sizeof(YGNode));
  if (config->useWebDefaults) {
    node->style.flexDirection = YGFlexDirectionRow;
    node->style.alignContent = YGAlignStretch;
  }
  node->config = config;
  return node;
}

YGNodeRef YGNodeNew(void) {
  return YGNodeNewWithConfig(&gYGConfigDefaults);
}

void YGNodeFree(const YGNodeRef node) {
  if (node->parent) {
    YGNodeListDelete(node->parent->children, node);
    node->parent = NULL;
  }

  const uint32_t childCount = YGNodeGetChildCount(node);
  for (uint32_t i = 0; i < childCount; i++) {
    const YGNodeRef child = YGNodeGetChild(node, i);
    child->parent = NULL;
  }

  YGNodeListFree(node->children);
  gYGFree(node);
  gNodeInstanceCount--;
}

void YGNodeFreeRecursive(const YGNodeRef root) {
  while (YGNodeGetChildCount(root) > 0) {
    const YGNodeRef child = YGNodeGetChild(root, 0);
    YGNodeRemoveChild(root, child);
    YGNodeFreeRecursive(child);
  }
  YGNodeFree(root);
}

void YGNodeReset(const YGNodeRef node) {
  YG_ASSERT(YGNodeGetChildCount(node) == 0,
            "Cannot reset a node which still has children attached");
  YG_ASSERT(node->parent == NULL, "Cannot reset a node still attached to a parent");

  YGNodeListFree(node->children);

  const YGConfigRef config = node->config;
  memcpy(node, &gYGNodeDefaults, sizeof(YGNode));
  if (config->useWebDefaults) {
    node->style.flexDirection = YGFlexDirectionRow;
    node->style.alignContent = YGAlignStretch;
  }
  node->config = config;
}

int32_t YGNodeGetInstanceCount(void) {
  return gNodeInstanceCount;
}

YGConfigRef YGConfigNew(void) {
  const YGConfigRef config = gYGMalloc(sizeof(YGConfig));
  YG_ASSERT(config, "Could not allocate memory for config");
  memcpy(config, &gYGConfigDefaults, sizeof(YGConfig));
  return config;
}

void YGConfigFree(const YGConfigRef config) {
  gYGFree(config);
}

static void YGNodeMarkDirtyInternal(const YGNodeRef node) {
  if (!node->isDirty) {
    node->isDirty = true;
    node->layout.computedFlexBasis = YGUndefined;
    if (node->parent) {
      YGNodeMarkDirtyInternal(node->parent);
    }
  }
}

void YGNodeSetMeasureFunc(const YGNodeRef node, YGMeasureFunc measureFunc) {
  if (measureFunc == NULL) {
    node->measure = NULL;
  } else {
    YG_ASSERT(YGNodeGetChildCount(node) == 0,
              "Cannot set measure function: Nodes with measure functions cannot have children.");
    node->measure = measureFunc;
  }
}

YGMeasureFunc YGNodeGetMeasureFunc(const YGNodeRef node) {
  return node->measure;
}

void YGNodeSetBaselineFunc(const YGNodeRef node, YGBaselineFunc baselineFunc) {
  node->baseline = baselineFunc;
}

YGBaselineFunc YGNodeGetBaselineFunc(const YGNodeRef node) {
  return node->baseline;
}

void YGNodeInsertChild(const YGNodeRef node, const YGNodeRef child, const uint32_t index) {
  YG_ASSERT(child->parent == NULL, "Child already has a parent, it must be removed first.");
  YG_ASSERT(node->measure == NULL,
            "Cannot add child: Nodes with measure functions cannot have children.");
  YGNodeListInsert(&node->children, child, index);
  child->parent = node;
  YGNodeMarkDirtyInternal(node);
}

void YGNodeRemoveChild(const YGNodeRef node, const YGNodeRef child) {
  if (YGNodeListDelete(node->children, child) != NULL) {
    child->layout = gYGNodeDefaults.layout; // layout is no longer valid
    child->parent = NULL;
    YGNodeMarkDirtyInternal(node);
  }
}

YGNodeRef YGNodeGetChild(const YGNodeRef node, const uint32_t index) {
  return YGNodeListGet(node->children, index);
}

YGNodeRef YGNodeGetParent(const YGNodeRef node) {
  return node->parent;
}

inline uint32_t YGNodeGetChildCount(const YGNodeRef node) {
  return YGNodeListCount(node->children);
}

void YGNodeMarkDirty(const YGNodeRef node) {
  YG_ASSERT(node->measure != NULL,
            "Only leaf nodes with custom measure functions"
            "should manually mark themselves as dirty");
  YGNodeMarkDirtyInternal(node);
}

bool YGNodeIsDirty(const YGNodeRef node) {
  return node->isDirty;
}

void YGNodeCopyStyle(const YGNodeRef dstNode, const YGNodeRef srcNode) {
  if (memcmp(&dstNode->style, &srcNode->style, sizeof(YGStyle)) != 0) {
    memcpy(&dstNode->style, &srcNode->style, sizeof(YGStyle));
    YGNodeMarkDirtyInternal(dstNode);
  }
}

static inline float YGResolveFlexGrow(const YGNodeRef node) {
  if (!YGFloatIsUndefined(node->style.flexGrow)) {
    return node->style.flexGrow;
  }
  if (!YGFloatIsUndefined(node->style.flex) && node->style.flex > 0.0f) {
    return node->style.flex;
  }
  return kDefaultFlexGrow;
}

float YGNodeStyleGetFlexGrow(const YGNodeRef node) {
  return YGFloatIsUndefined(node->style.flexGrow) ? kDefaultFlexGrow : node->style.flexGrow;
}

float YGNodeStyleGetFlexShrink(const YGNodeRef node) {
  return YGFloatIsUndefined(node->style.flexShrink) ? (node->config->useWebDefaults ? kWebDefaultFlexShrink : kDefaultFlexShrink) : node->style.flexShrink;
}

static inline float YGNodeResolveFlexShrink(const YGNodeRef node) {
  if (!YGFloatIsUndefined(node->style.flexShrink)) {
    return node->style.flexShrink;
  }
  if (!node->config->useWebDefaults && !YGFloatIsUndefined(node->style.flex) && node->style.flex < 0.0f) {
    return -node->style.flex;
  }
  return node->config->useWebDefaults ? kWebDefaultFlexShrink : kDefaultFlexShrink;
}

static inline const YGValue *YGNodeResolveFlexBasisPtr(const YGNodeRef node) {
  if (node->style.flexBasis.unit != YGUnitAuto && node->style.flexBasis.unit != YGUnitUndefined) {
    return &node->style.flexBasis;
  }
  if (!YGFloatIsUndefined(node->style.flex) && node->style.flex > 0.0f) {
    return node->config->useWebDefaults ? &YGValueAuto : &YGValueZero;
  }
  return &YGValueAuto;
}

#define YG_NODE_PROPERTY_IMPL(type, name, paramName, instanceName) \
  void YGNodeSet##name(const YGNodeRef node, type paramName) {     \
    node->instanceName = paramName;                                \
  }                                                                \
                                                                   \
  type YGNodeGet##name(const YGNodeRef node) {                     \
    return node->instanceName;                                     \
  }

#define YG_NODE_STYLE_PROPERTY_SETTER_IMPL(type, name, paramName, instanceName) \
  void YGNodeStyleSet##name(const YGNodeRef node, const type paramName) {       \
    if (node->style.instanceName != paramName) {                                \
      node->style.instanceName = paramName;                                     \
      YGNodeMarkDirtyInternal(node);                                            \
    }                                                                           \
  }

#define YG_NODE_STYLE_PROPERTY_SETTER_UNIT_IMPL(type, name, paramName, instanceName) \
  void YGNodeStyleSet##name(const YGNodeRef node, const type paramName) {            \
    if (node->style.instanceName.value != paramName ||                               \
        node->style.instanceName.unit != YGUnitPoint) {                              \
      node->style.instanceName.value = paramName;                                    \
      node->style.instanceName.unit =                                                \
          YGFloatIsUndefined(paramName) ? YGUnitAuto : YGUnitPoint;                  \
      YGNodeMarkDirtyInternal(node);                                                 \
    }                                                                                \
  }                                                                                  \
                                                                                     \
  void YGNodeStyleSet##name##Percent(const YGNodeRef node, const type paramName) {   \
    if (node->style.instanceName.value != paramName ||                               \
        node->style.instanceName.unit != YGUnitPercent) {                            \
      node->style.instanceName.value = paramName;                                    \
      node->style.instanceName.unit =                                                \
          YGFloatIsUndefined(paramName) ? YGUnitAuto : YGUnitPercent;                \
      YGNodeMarkDirtyInternal(node);                                                 \
    }                                                                                \
  }

#define YG_NODE_STYLE_PROPERTY_SETTER_UNIT_AUTO_IMPL(type, name, paramName, instanceName)         \
  void YGNodeStyleSet##name(const YGNodeRef node, const type paramName) {                         \
    if (node->style.instanceName.value != paramName ||                                            \
        node->style.instanceName.unit != YGUnitPoint) {                                           \
      node->style.instanceName.value = paramName;                                                 \
      node->style.instanceName.unit = YGFloatIsUndefined(paramName) ? YGUnitAuto : YGUnitPoint;   \
      YGNodeMarkDirtyInternal(node);                                                              \
    }                                                                                             \
  }                                                                                               \
                                                                                                  \
  void YGNodeStyleSet##name##Percent(const YGNodeRef node, const type paramName) {                \
    if (node->style.instanceName.value != paramName ||                                            \
        node->style.instanceName.unit != YGUnitPercent) {                                         \
      node->style.instanceName.value = paramName;                                                 \
      node->style.instanceName.unit = YGFloatIsUndefined(paramName) ? YGUnitAuto : YGUnitPercent; \
      YGNodeMarkDirtyInternal(node);                                                              \
    }                                                                                             \
  }                                                                                               \
                                                                                                  \
  void YGNodeStyleSet##name##Auto(const YGNodeRef node) {                                         \
    if (node->style.instanceName.unit != YGUnitAuto) {                                            \
      node->style.instanceName.value = YGUndefined;                                               \
      node->style.instanceName.unit = YGUnitAuto;                                                 \
      YGNodeMarkDirtyInternal(node);                                                              \
    }                                                                                             \
  }

#define YG_NODE_STYLE_PROPERTY_IMPL(type, name, paramName, instanceName)  \
  YG_NODE_STYLE_PROPERTY_SETTER_IMPL(type, name, paramName, instanceName) \
                                                                          \
  type YGNodeStyleGet##name(const YGNodeRef node) {                       \
    return node->style.instanceName;                                      \
  }

#define YG_NODE_STYLE_PROPERTY_UNIT_IMPL(type, name, paramName, instanceName)   \
  YG_NODE_STYLE_PROPERTY_SETTER_UNIT_IMPL(float, name, paramName, instanceName) \
                                                                                \
  type YGNodeStyleGet##name(const YGNodeRef node) {                             \
    return node->style.instanceName;                                            \
  }

#define YG_NODE_STYLE_PROPERTY_UNIT_AUTO_IMPL(type, name, paramName, instanceName)   \
  YG_NODE_STYLE_PROPERTY_SETTER_UNIT_AUTO_IMPL(float, name, paramName, instanceName) \
                                                                                     \
  type YGNodeStyleGet##name(const YGNodeRef node) {                                  \
    return node->style.instanceName;                                                 \
  }

#define YG_NODE_STYLE_EDGE_PROPERTY_UNIT_AUTO_IMPL(type, name, instanceName) \
  void YGNodeStyleSet##name##Auto(const YGNodeRef node, const YGEdge edge) { \
    if (node->style.instanceName[edge].unit != YGUnitAuto) {                 \
      node->style.instanceName[edge].value = YGUndefined;                    \
      node->style.instanceName[edge].unit = YGUnitAuto;                      \
      YGNodeMarkDirtyInternal(node);                                         \
    }                                                                        \
  }

#define YG_NODE_STYLE_EDGE_PROPERTY_UNIT_IMPL(type, name, paramName, instanceName)            \
  void YGNodeStyleSet##name(const YGNodeRef node, const YGEdge edge, const float paramName) { \
    if (node->style.instanceName[edge].value != paramName ||                                  \
        node->style.instanceName[edge].unit != YGUnitPoint) {                                 \
      node->style.instanceName[edge].value = paramName;                                       \
      node->style.instanceName[edge].unit =                                                   \
          YGFloatIsUndefined(paramName) ? YGUnitUndefined : YGUnitPoint;                      \
      YGNodeMarkDirtyInternal(node);                                                          \
    }                                                                                         \
  }                                                                                           \
                                                                                              \
  void YGNodeStyleSet##name##Percent(const YGNodeRef node,                                    \
                                     const YGEdge edge,                                       \
                                     const float paramName) {                                 \
    if (node->style.instanceName[edge].value != paramName ||                                  \
        node->style.instanceName[edge].unit != YGUnitPercent) {                               \
      node->style.instanceName[edge].value = paramName;                                       \
      node->style.instanceName[edge].unit =                                                   \
          YGFloatIsUndefined(paramName) ? YGUnitUndefined : YGUnitPercent;                    \
      YGNodeMarkDirtyInternal(node);                                                          \
    }                                                                                         \
  }                                                                                           \
                                                                                              \
  WIN_STRUCT(type) YGNodeStyleGet##name(const YGNodeRef node, const YGEdge edge) {            \
    return WIN_STRUCT_REF(node->style.instanceName[edge]);                                    \
  }

#define YG_NODE_STYLE_EDGE_PROPERTY_IMPL(type, name, paramName, instanceName)                 \
  void YGNodeStyleSet##name(const YGNodeRef node, const YGEdge edge, const float paramName) { \
    if (node->style.instanceName[edge].value != paramName ||                                  \
        node->style.instanceName[edge].unit != YGUnitPoint) {                                 \
      node->style.instanceName[edge].value = paramName;                                       \
      node->style.instanceName[edge].unit =                                                   \
          YGFloatIsUndefined(paramName) ? YGUnitUndefined : YGUnitPoint;                      \
      YGNodeMarkDirtyInternal(node);                                                          \
    }                                                                                         \
  }                                                                                           \
                                                                                              \
  float YGNodeStyleGet##name(const YGNodeRef node, const YGEdge edge) {                       \
    return node->style.instanceName[edge].value;                                              \
  }

#define YG_NODE_LAYOUT_PROPERTY_IMPL(type, name, instanceName) \
  type YGNodeLayoutGet##name(const YGNodeRef node) {           \
    return node->layout.instanceName;                          \
  }

#define YG_NODE_LAYOUT_RESOLVED_PROPERTY_IMPL(type, name, instanceName)                    \
  type YGNodeLayoutGet##name(const YGNodeRef node, const YGEdge edge) {                    \
    YG_ASSERT(edge <= YGEdgeEnd, "Cannot get layout properties of multi-edge shorthands"); \
                                                                                           \
    if (edge == YGEdgeLeft) {                                                              \
      if (node->layout.direction == YGDirectionRTL) {                                      \
        return node->layout.instanceName[YGEdgeEnd];                                       \
      } else {                                                                             \
        return node->layout.instanceName[YGEdgeStart];                                     \
      }                                                                                    \
    }                                                                                      \
                                                                                           \
    if (edge == YGEdgeRight) {                                                             \
      if (node->layout.direction == YGDirectionRTL) {                                      \
        return node->layout.instanceName[YGEdgeStart];                                     \
      } else {                                                                             \
        return node->layout.instanceName[YGEdgeEnd];                                       \
      }                                                                                    \
    }                                                                                      \
                                                                                           \
    return node->layout.instanceName[edge];                                                \
  }

YG_NODE_PROPERTY_IMPL(void *, Context, context, context);
YG_NODE_PROPERTY_IMPL(YGPrintFunc, PrintFunc, printFunc, print);
YG_NODE_PROPERTY_IMPL(bool, HasNewLayout, hasNewLayout, hasNewLayout);

YG_NODE_STYLE_PROPERTY_IMPL(YGDirection, Direction, direction, direction);
YG_NODE_STYLE_PROPERTY_IMPL(YGFlexDirection, FlexDirection, flexDirection, flexDirection);
YG_NODE_STYLE_PROPERTY_IMPL(YGJustify, JustifyContent, justifyContent, justifyContent);
YG_NODE_STYLE_PROPERTY_IMPL(YGAlign, AlignContent, alignContent, alignContent);
YG_NODE_STYLE_PROPERTY_IMPL(YGAlign, AlignItems, alignItems, alignItems);
YG_NODE_STYLE_PROPERTY_IMPL(YGAlign, AlignSelf, alignSelf, alignSelf);
YG_NODE_STYLE_PROPERTY_IMPL(YGPositionType, PositionType, positionType, positionType);
YG_NODE_STYLE_PROPERTY_IMPL(YGWrap, FlexWrap, flexWrap, flexWrap);
YG_NODE_STYLE_PROPERTY_IMPL(YGOverflow, Overflow, overflow, overflow);
YG_NODE_STYLE_PROPERTY_IMPL(YGDisplay, Display, display, display);

YG_NODE_STYLE_PROPERTY_IMPL(float, Flex, flex, flex);
YG_NODE_STYLE_PROPERTY_SETTER_IMPL(float, FlexGrow, flexGrow, flexGrow);
YG_NODE_STYLE_PROPERTY_SETTER_IMPL(float, FlexShrink, flexShrink, flexShrink);
YG_NODE_STYLE_PROPERTY_UNIT_AUTO_IMPL(YGValue, FlexBasis, flexBasis, flexBasis);

YG_NODE_STYLE_EDGE_PROPERTY_UNIT_IMPL(YGValue, Position, position, position);
YG_NODE_STYLE_EDGE_PROPERTY_UNIT_IMPL(YGValue, Margin, margin, margin);
YG_NODE_STYLE_EDGE_PROPERTY_UNIT_AUTO_IMPL(YGValue, Margin, margin);
YG_NODE_STYLE_EDGE_PROPERTY_UNIT_IMPL(YGValue, Padding, padding, padding);
YG_NODE_STYLE_EDGE_PROPERTY_IMPL(float, Border, border, border);

YG_NODE_STYLE_PROPERTY_UNIT_AUTO_IMPL(YGValue, Width, width, dimensions[YGDimensionWidth]);
YG_NODE_STYLE_PROPERTY_UNIT_AUTO_IMPL(YGValue, Height, height, dimensions[YGDimensionHeight]);
YG_NODE_STYLE_PROPERTY_UNIT_IMPL(YGValue, MinWidth, minWidth, minDimensions[YGDimensionWidth]);
YG_NODE_STYLE_PROPERTY_UNIT_IMPL(YGValue, MinHeight, minHeight, minDimensions[YGDimensionHeight]);
YG_NODE_STYLE_PROPERTY_UNIT_IMPL(YGValue, MaxWidth, maxWidth, maxDimensions[YGDimensionWidth]);
YG_NODE_STYLE_PROPERTY_UNIT_IMPL(YGValue, MaxHeight, maxHeight, maxDimensions[YGDimensionHeight]);

// Yoga specific properties, not compatible with flexbox specification
YG_NODE_STYLE_PROPERTY_IMPL(float, AspectRatio, aspectRatio, aspectRatio);

YG_NODE_LAYOUT_PROPERTY_IMPL(float, Left, position[YGEdgeLeft]);
YG_NODE_LAYOUT_PROPERTY_IMPL(float, Top, position[YGEdgeTop]);
YG_NODE_LAYOUT_PROPERTY_IMPL(float, Right, position[YGEdgeRight]);
YG_NODE_LAYOUT_PROPERTY_IMPL(float, Bottom, position[YGEdgeBottom]);
YG_NODE_LAYOUT_PROPERTY_IMPL(float, Width, dimensions[YGDimensionWidth]);
YG_NODE_LAYOUT_PROPERTY_IMPL(float, Height, dimensions[YGDimensionHeight]);
YG_NODE_LAYOUT_PROPERTY_IMPL(YGDirection, Direction, direction);

YG_NODE_LAYOUT_RESOLVED_PROPERTY_IMPL(float, Margin, margin);
YG_NODE_LAYOUT_RESOLVED_PROPERTY_IMPL(float, Border, border);
YG_NODE_LAYOUT_RESOLVED_PROPERTY_IMPL(float, Padding, padding);

uint32_t gCurrentGenerationCount = 0;

bool YGLayoutNodeInternal(const YGNodeRef node,
                          const float availableWidth,
                          const float availableHeight,
                          const YGDirection parentDirection,
                          const YGMeasureMode widthMeasureMode,
                          const YGMeasureMode heightMeasureMode,
                          const float parentWidth,
                          const float parentHeight,
                          const bool performLayout,
                          const char *reason,
                          const YGConfigRef config);

inline bool YGFloatIsUndefined(const float value) {
  return isnan(value);
}

static inline bool YGValueEqual(const YGValue a, const YGValue b) {
  if (a.unit != b.unit) {
    return false;
  }

  if (a.unit == YGUnitUndefined) {
    return true;
  }

  return fabs(a.value - b.value) < 0.0001f;
}

static inline void YGResolveDimensions(YGNodeRef node) {
  for (YGDimension dim = YGDimensionWidth; dim <= YGDimensionHeight; dim++) {
    if (node->style.maxDimensions[dim].unit != YGUnitUndefined &&
        YGValueEqual(node->style.maxDimensions[dim], node->style.minDimensions[dim])) {
      node->resolvedDimensions[dim] = &node->style.maxDimensions[dim];
    } else {
      node->resolvedDimensions[dim] = &node->style.dimensions[dim];
    }
  }
}

static inline bool YGFloatsEqual(const float a, const float b) {
  if (YGFloatIsUndefined(a)) {
    return YGFloatIsUndefined(b);
  }
  return fabs(a - b) < 0.0001f;
}

static void YGIndent(const uint32_t n) {
  for (uint32_t i = 0; i < n; i++) {
    YGLog(YGLogLevelDebug, "  ");
  }
}

static void YGPrintNumberIfNotZero(const char *str, const YGValue *const number) {
  if (!YGFloatsEqual(number->value, 0)) {
    YGLog(YGLogLevelDebug,
          "%s: %g%s, ",
          str,
          number->value,
          number->unit == YGUnitPoint ? "pt" : "%");
  }
}

static void YGPrintNumberIfNotUndefinedf(const char *str, const float number) {
  if (!YGFloatIsUndefined(number)) {
    YGLog(YGLogLevelDebug, "%s: %g, ", str, number);
  }
}

static void YGPrintNumberIfNotUndefined(const char *str, const YGValue *const number) {
  if (number->unit != YGUnitUndefined) {
    YGLog(YGLogLevelDebug,
          "%s: %g%s, ",
          str,
          number->value,
          number->unit == YGUnitPoint ? "pt" : "%");
  }
}

static bool YGFourValuesEqual(const YGValue four[4]) {
  return YGValueEqual(four[0], four[1]) && YGValueEqual(four[0], four[2]) &&
         YGValueEqual(four[0], four[3]);
}

static void YGNodePrintInternal(const YGNodeRef node,
                                const YGPrintOptions options,
                                const uint32_t level) {
  YGIndent(level);
  YGLog(YGLogLevelDebug, "{");

  if (node->print) {
    node->print(node);
  }

  if (options & YGPrintOptionsLayout) {
    YGLog(YGLogLevelDebug, "layout: {");
    YGLog(YGLogLevelDebug, "width: %g, ", node->layout.dimensions[YGDimensionWidth]);
    YGLog(YGLogLevelDebug, "height: %g, ", node->layout.dimensions[YGDimensionHeight]);
    YGLog(YGLogLevelDebug, "top: %g, ", node->layout.position[YGEdgeTop]);
    YGLog(YGLogLevelDebug, "left: %g", node->layout.position[YGEdgeLeft]);
    YGLog(YGLogLevelDebug, "}, ");
  }

  if (options & YGPrintOptionsStyle) {
    if (node->style.flexDirection == YGFlexDirectionColumn) {
      YGLog(YGLogLevelDebug, "flexDirection: 'column', ");
    } else if (node->style.flexDirection == YGFlexDirectionColumnReverse) {
      YGLog(YGLogLevelDebug, "flexDirection: 'column-reverse', ");
    } else if (node->style.flexDirection == YGFlexDirectionRow) {
      YGLog(YGLogLevelDebug, "flexDirection: 'row', ");
    } else if (node->style.flexDirection == YGFlexDirectionRowReverse) {
      YGLog(YGLogLevelDebug, "flexDirection: 'row-reverse', ");
    }

    if (node->style.justifyContent == YGJustifyCenter) {
      YGLog(YGLogLevelDebug, "justifyContent: 'center', ");
    } else if (node->style.justifyContent == YGJustifyFlexEnd) {
      YGLog(YGLogLevelDebug, "justifyContent: 'flex-end', ");
    } else if (node->style.justifyContent == YGJustifySpaceAround) {
      YGLog(YGLogLevelDebug, "justifyContent: 'space-around', ");
    } else if (node->style.justifyContent == YGJustifySpaceBetween) {
      YGLog(YGLogLevelDebug, "justifyContent: 'space-between', ");
    }

    if (node->style.alignItems == YGAlignCenter) {
      YGLog(YGLogLevelDebug, "alignItems: 'center', ");
    } else if (node->style.alignItems == YGAlignFlexEnd) {
      YGLog(YGLogLevelDebug, "alignItems: 'flex-end', ");
    } else if (node->style.alignItems == YGAlignStretch) {
      YGLog(YGLogLevelDebug, "alignItems: 'stretch', ");
    }

    if (node->style.alignContent == YGAlignCenter) {
      YGLog(YGLogLevelDebug, "alignContent: 'center', ");
    } else if (node->style.alignContent == YGAlignFlexEnd) {
      YGLog(YGLogLevelDebug, "alignContent: 'flex-end', ");
    } else if (node->style.alignContent == YGAlignStretch) {
      YGLog(YGLogLevelDebug, "alignContent: 'stretch', ");
    }

    if (node->style.alignSelf == YGAlignFlexStart) {
      YGLog(YGLogLevelDebug, "alignSelf: 'flex-start', ");
    } else if (node->style.alignSelf == YGAlignCenter) {
      YGLog(YGLogLevelDebug, "alignSelf: 'center', ");
    } else if (node->style.alignSelf == YGAlignFlexEnd) {
      YGLog(YGLogLevelDebug, "alignSelf: 'flex-end', ");
    } else if (node->style.alignSelf == YGAlignStretch) {
      YGLog(YGLogLevelDebug, "alignSelf: 'stretch', ");
    }

    YGPrintNumberIfNotUndefinedf("flexGrow", YGResolveFlexGrow(node));
    YGPrintNumberIfNotUndefinedf("flexShrink", YGNodeResolveFlexShrink(node));
    YGPrintNumberIfNotUndefined("flexBasis", YGNodeResolveFlexBasisPtr(node));

    if (node->style.overflow == YGOverflowHidden) {
      YGLog(YGLogLevelDebug, "overflow: 'hidden', ");
    } else if (node->style.overflow == YGOverflowVisible) {
      YGLog(YGLogLevelDebug, "overflow: 'visible', ");
    } else if (node->style.overflow == YGOverflowScroll) {
      YGLog(YGLogLevelDebug, "overflow: 'scroll', ");
    }

    if (YGFourValuesEqual(node->style.margin)) {
      YGPrintNumberIfNotZero("margin",
                             YGComputedEdgeValue(node->style.margin, YGEdgeLeft, &YGValueZero));
    } else {
      YGPrintNumberIfNotZero("marginLeft",
                             YGComputedEdgeValue(node->style.margin, YGEdgeLeft, &YGValueZero));
      YGPrintNumberIfNotZero("marginRight",
                             YGComputedEdgeValue(node->style.margin, YGEdgeRight, &YGValueZero));
      YGPrintNumberIfNotZero("marginTop",
                             YGComputedEdgeValue(node->style.margin, YGEdgeTop, &YGValueZero));
      YGPrintNumberIfNotZero("marginBottom",
                             YGComputedEdgeValue(node->style.margin, YGEdgeBottom, &YGValueZero));
      YGPrintNumberIfNotZero("marginStart",
                             YGComputedEdgeValue(node->style.margin, YGEdgeStart, &YGValueZero));
      YGPrintNumberIfNotZero("marginEnd",
                             YGComputedEdgeValue(node->style.margin, YGEdgeEnd, &YGValueZero));
    }

    if (YGFourValuesEqual(node->style.padding)) {
      YGPrintNumberIfNotZero("padding",
                             YGComputedEdgeValue(node->style.padding, YGEdgeLeft, &YGValueZero));
    } else {
      YGPrintNumberIfNotZero("paddingLeft",
                             YGComputedEdgeValue(node->style.padding, YGEdgeLeft, &YGValueZero));
      YGPrintNumberIfNotZero("paddingRight",
                             YGComputedEdgeValue(node->style.padding, YGEdgeRight, &YGValueZero));
      YGPrintNumberIfNotZero("paddingTop",
                             YGComputedEdgeValue(node->style.padding, YGEdgeTop, &YGValueZero));
      YGPrintNumberIfNotZero("paddingBottom",
                             YGComputedEdgeValue(node->style.padding, YGEdgeBottom, &YGValueZero));
      YGPrintNumberIfNotZero("paddingStart",
                             YGComputedEdgeValue(node->style.padding, YGEdgeStart, &YGValueZero));
      YGPrintNumberIfNotZero("paddingEnd",
                             YGComputedEdgeValue(node->style.padding, YGEdgeEnd, &YGValueZero));
    }

    if (YGFourValuesEqual(node->style.border)) {
      YGPrintNumberIfNotZero("borderWidth",
                             YGComputedEdgeValue(node->style.border, YGEdgeLeft, &YGValueZero));
    } else {
      YGPrintNumberIfNotZero("borderLeftWidth",
                             YGComputedEdgeValue(node->style.border, YGEdgeLeft, &YGValueZero));
      YGPrintNumberIfNotZero("borderRightWidth",
                             YGComputedEdgeValue(node->style.border, YGEdgeRight, &YGValueZero));
      YGPrintNumberIfNotZero("borderTopWidth",
                             YGComputedEdgeValue(node->style.border, YGEdgeTop, &YGValueZero));
      YGPrintNumberIfNotZero("borderBottomWidth",
                             YGComputedEdgeValue(node->style.border, YGEdgeBottom, &YGValueZero));
      YGPrintNumberIfNotZero("borderStartWidth",
                             YGComputedEdgeValue(node->style.border, YGEdgeStart, &YGValueZero));
      YGPrintNumberIfNotZero("borderEndWidth",
                             YGComputedEdgeValue(node->style.border, YGEdgeEnd, &YGValueZero));
    }

    YGPrintNumberIfNotUndefined("width", &node->style.dimensions[YGDimensionWidth]);
    YGPrintNumberIfNotUndefined("height", &node->style.dimensions[YGDimensionHeight]);
    YGPrintNumberIfNotUndefined("maxWidth", &node->style.maxDimensions[YGDimensionWidth]);
    YGPrintNumberIfNotUndefined("maxHeight", &node->style.maxDimensions[YGDimensionHeight]);
    YGPrintNumberIfNotUndefined("minWidth", &node->style.minDimensions[YGDimensionWidth]);
    YGPrintNumberIfNotUndefined("minHeight", &node->style.minDimensions[YGDimensionHeight]);

    if (node->style.positionType == YGPositionTypeAbsolute) {
      YGLog(YGLogLevelDebug, "position: 'absolute', ");
    }

    YGPrintNumberIfNotUndefined(
        "left", YGComputedEdgeValue(node->style.position, YGEdgeLeft, &YGValueUndefined));
    YGPrintNumberIfNotUndefined(
        "right", YGComputedEdgeValue(node->style.position, YGEdgeRight, &YGValueUndefined));
    YGPrintNumberIfNotUndefined(
        "top", YGComputedEdgeValue(node->style.position, YGEdgeTop, &YGValueUndefined));
    YGPrintNumberIfNotUndefined(
        "bottom", YGComputedEdgeValue(node->style.position, YGEdgeBottom, &YGValueUndefined));
  }

  const uint32_t childCount = YGNodeListCount(node->children);
  if (options & YGPrintOptionsChildren && childCount > 0) {
    YGLog(YGLogLevelDebug, "children: [\n");
    for (uint32_t i = 0; i < childCount; i++) {
      YGNodePrintInternal(YGNodeGetChild(node, i), options, level + 1);
    }
    YGIndent(level);
    YGLog(YGLogLevelDebug, "]},\n");
  } else {
    YGLog(YGLogLevelDebug, "},\n");
  }
}

void YGNodePrint(const YGNodeRef node, const YGPrintOptions options) {
  YGNodePrintInternal(node, options, 0);
}

static const YGEdge leading[4] = {
        [YGFlexDirectionColumn] = YGEdgeTop,
        [YGFlexDirectionColumnReverse] = YGEdgeBottom,
        [YGFlexDirectionRow] = YGEdgeLeft,
        [YGFlexDirectionRowReverse] = YGEdgeRight,
};
static const YGEdge trailing[4] = {
        [YGFlexDirectionColumn] = YGEdgeBottom,
        [YGFlexDirectionColumnReverse] = YGEdgeTop,
        [YGFlexDirectionRow] = YGEdgeRight,
        [YGFlexDirectionRowReverse] = YGEdgeLeft,
};
static const YGEdge pos[4] = {
        [YGFlexDirectionColumn] = YGEdgeTop,
        [YGFlexDirectionColumnReverse] = YGEdgeBottom,
        [YGFlexDirectionRow] = YGEdgeLeft,
        [YGFlexDirectionRowReverse] = YGEdgeRight,
};
static const YGDimension dim[4] = {
        [YGFlexDirectionColumn] = YGDimensionHeight,
        [YGFlexDirectionColumnReverse] = YGDimensionHeight,
        [YGFlexDirectionRow] = YGDimensionWidth,
        [YGFlexDirectionRowReverse] = YGDimensionWidth,
};

static inline bool YGFlexDirectionIsRow(const YGFlexDirection flexDirection) {
  return flexDirection == YGFlexDirectionRow || flexDirection == YGFlexDirectionRowReverse;
}

static inline bool YGFlexDirectionIsColumn(const YGFlexDirection flexDirection) {
  return flexDirection == YGFlexDirectionColumn || flexDirection == YGFlexDirectionColumnReverse;
}

static inline float YGNodeLeadingMargin(const YGNodeRef node,
                                        const YGFlexDirection axis,
                                        const float widthSize) {
  if (YGFlexDirectionIsRow(axis) && node->style.margin[YGEdgeStart].unit != YGUnitUndefined) {
    return YGResolveValueMargin(&node->style.margin[YGEdgeStart], widthSize);
  }

  return YGResolveValueMargin(YGComputedEdgeValue(node->style.margin, leading[axis], &YGValueZero),
                              widthSize);
}

static float YGNodeTrailingMargin(const YGNodeRef node,
                                  const YGFlexDirection axis,
                                  const float widthSize) {
  if (YGFlexDirectionIsRow(axis) && node->style.margin[YGEdgeEnd].unit != YGUnitUndefined) {
    return YGResolveValueMargin(&node->style.margin[YGEdgeEnd], widthSize);
  }

  return YGResolveValueMargin(YGComputedEdgeValue(node->style.margin, trailing[axis], &YGValueZero),
                              widthSize);
}

static float YGNodeLeadingPadding(const YGNodeRef node,
                                  const YGFlexDirection axis,
                                  const float widthSize) {
  if (YGFlexDirectionIsRow(axis) && node->style.padding[YGEdgeStart].unit != YGUnitUndefined &&
      YGResolveValue(&node->style.padding[YGEdgeStart], widthSize) >= 0.0f) {
    return YGResolveValue(&node->style.padding[YGEdgeStart], widthSize);
  }

  return fmaxf(YGResolveValue(YGComputedEdgeValue(node->style.padding, leading[axis], &YGValueZero),
                              widthSize),
               0.0f);
}

static float YGNodeTrailingPadding(const YGNodeRef node,
                                   const YGFlexDirection axis,
                                   const float widthSize) {
  if (YGFlexDirectionIsRow(axis) && node->style.padding[YGEdgeEnd].unit != YGUnitUndefined &&
      YGResolveValue(&node->style.padding[YGEdgeEnd], widthSize) >= 0.0f) {
    return YGResolveValue(&node->style.padding[YGEdgeEnd], widthSize);
  }

  return fmaxf(YGResolveValue(YGComputedEdgeValue(node->style.padding, trailing[axis], &YGValueZero),
                              widthSize),
               0.0f);
}

static float YGNodeLeadingBorder(const YGNodeRef node, const YGFlexDirection axis) {
  if (YGFlexDirectionIsRow(axis) && node->style.border[YGEdgeStart].unit != YGUnitUndefined &&
      node->style.border[YGEdgeStart].value >= 0.0f) {
    return node->style.border[YGEdgeStart].value;
  }

  return fmaxf(YGComputedEdgeValue(node->style.border, leading[axis], &YGValueZero)->value, 0.0f);
}

static float YGNodeTrailingBorder(const YGNodeRef node, const YGFlexDirection axis) {
  if (YGFlexDirectionIsRow(axis) && node->style.border[YGEdgeEnd].unit != YGUnitUndefined &&
      node->style.border[YGEdgeEnd].value >= 0.0f) {
    return node->style.border[YGEdgeEnd].value;
  }

  return fmaxf(YGComputedEdgeValue(node->style.border, trailing[axis], &YGValueZero)->value, 0.0f);
}

static inline float YGNodeLeadingPaddingAndBorder(const YGNodeRef node,
                                                  const YGFlexDirection axis,
                                                  const float widthSize) {
  return YGNodeLeadingPadding(node, axis, widthSize) + YGNodeLeadingBorder(node, axis);
}

static inline float YGNodeTrailingPaddingAndBorder(const YGNodeRef node,
                                                   const YGFlexDirection axis,
                                                   const float widthSize) {
  return YGNodeTrailingPadding(node, axis, widthSize) + YGNodeTrailingBorder(node, axis);
}

static inline float YGNodeMarginForAxis(const YGNodeRef node,
                                        const YGFlexDirection axis,
                                        const float widthSize) {
  return YGNodeLeadingMargin(node, axis, widthSize) + YGNodeTrailingMargin(node, axis, widthSize);
}

static inline float YGNodePaddingAndBorderForAxis(const YGNodeRef node,
                                                  const YGFlexDirection axis,
                                                  const float widthSize) {
  return YGNodeLeadingPaddingAndBorder(node, axis, widthSize) +
         YGNodeTrailingPaddingAndBorder(node, axis, widthSize);
}

static inline YGAlign YGNodeAlignItem(const YGNodeRef node, const YGNodeRef child) {
  const YGAlign align =
      child->style.alignSelf == YGAlignAuto ? node->style.alignItems : child->style.alignSelf;
  if (align == YGAlignBaseline && YGFlexDirectionIsColumn(node->style.flexDirection)) {
    return YGAlignFlexStart;
  }
  return align;
}

static inline YGDirection YGNodeResolveDirection(const YGNodeRef node,
                                                 const YGDirection parentDirection) {
  if (node->style.direction == YGDirectionInherit) {
    return parentDirection > YGDirectionInherit ? parentDirection : YGDirectionLTR;
  } else {
    return node->style.direction;
  }
}

static float YGBaseline(const YGNodeRef node) {
  if (node->baseline != NULL) {
    const float baseline = node->baseline(node,
                                          node->layout.measuredDimensions[YGDimensionWidth],
                                          node->layout.measuredDimensions[YGDimensionHeight]);
    YG_ASSERT(!YGFloatIsUndefined(baseline), "Expect custom baseline function to not return NaN")
    return baseline;
  }

  YGNodeRef baselineChild = NULL;
  const uint32_t childCount = YGNodeGetChildCount(node);
  for (uint32_t i = 0; i < childCount; i++) {
    const YGNodeRef child = YGNodeGetChild(node, i);
    if (child->lineIndex > 0) {
      break;
    }
    if (child->style.positionType == YGPositionTypeAbsolute) {
      continue;
    }
    if (YGNodeAlignItem(node, child) == YGAlignBaseline) {
      baselineChild = child;
      break;
    }

    if (baselineChild == NULL) {
      baselineChild = child;
    }
  }

  if (baselineChild == NULL) {
    return node->layout.measuredDimensions[YGDimensionHeight];
  }

  const float baseline = YGBaseline(baselineChild);
  return baseline + baselineChild->layout.position[YGEdgeTop];
}

static inline YGFlexDirection YGResolveFlexDirection(const YGFlexDirection flexDirection,
                                                     const YGDirection direction) {
  if (direction == YGDirectionRTL) {
    if (flexDirection == YGFlexDirectionRow) {
      return YGFlexDirectionRowReverse;
    } else if (flexDirection == YGFlexDirectionRowReverse) {
      return YGFlexDirectionRow;
    }
  }

  return flexDirection;
}

static YGFlexDirection YGFlexDirectionCross(const YGFlexDirection flexDirection,
                                            const YGDirection direction) {
  return YGFlexDirectionIsColumn(flexDirection)
             ? YGResolveFlexDirection(YGFlexDirectionRow, direction)
             : YGFlexDirectionColumn;
}

static inline bool YGNodeIsFlex(const YGNodeRef node) {
  return (node->style.positionType == YGPositionTypeRelative &&
          (YGResolveFlexGrow(node) != 0 || YGNodeResolveFlexShrink(node) != 0));
}

static bool YGIsBaselineLayout(const YGNodeRef node) {
  if (YGFlexDirectionIsColumn(node->style.flexDirection)) {
    return false;
  }
  if (node->style.alignItems == YGAlignBaseline) {
    return true;
  }
  const uint32_t childCount = YGNodeGetChildCount(node);
  for (uint32_t i = 0; i < childCount; i++) {
    const YGNodeRef child = YGNodeGetChild(node, i);
    if (child->style.positionType == YGPositionTypeRelative &&
        child->style.alignSelf == YGAlignBaseline) {
      return true;
    }
  }

  return false;
}

static inline float YGNodeDimWithMargin(const YGNodeRef node,
                                        const YGFlexDirection axis,
                                        const float widthSize) {
  return node->layout.measuredDimensions[dim[axis]] + YGNodeLeadingMargin(node, axis, widthSize) +
         YGNodeTrailingMargin(node, axis, widthSize);
}

static inline bool YGNodeIsStyleDimDefined(const YGNodeRef node,
                                           const YGFlexDirection axis,
                                           const float parentSize) {
  return !(node->resolvedDimensions[dim[axis]]->unit == YGUnitAuto ||
           node->resolvedDimensions[dim[axis]]->unit == YGUnitUndefined ||
           (node->resolvedDimensions[dim[axis]]->unit == YGUnitPoint &&
            node->resolvedDimensions[dim[axis]]->value < 0.0f) ||
           (node->resolvedDimensions[dim[axis]]->unit == YGUnitPercent &&
            (node->resolvedDimensions[dim[axis]]->value < 0.0f || YGFloatIsUndefined(parentSize))));
}

static inline bool YGNodeIsLayoutDimDefined(const YGNodeRef node, const YGFlexDirection axis) {
  const float value = node->layout.measuredDimensions[dim[axis]];
  return !YGFloatIsUndefined(value) && value >= 0.0f;
}

static inline bool YGNodeIsLeadingPosDefined(const YGNodeRef node, const YGFlexDirection axis) {
  return (YGFlexDirectionIsRow(axis) &&
          YGComputedEdgeValue(node->style.position, YGEdgeStart, &YGValueUndefined)->unit !=
              YGUnitUndefined) ||
         YGComputedEdgeValue(node->style.position, leading[axis], &YGValueUndefined)->unit !=
             YGUnitUndefined;
}

static inline bool YGNodeIsTrailingPosDefined(const YGNodeRef node, const YGFlexDirection axis) {
  return (YGFlexDirectionIsRow(axis) &&
          YGComputedEdgeValue(node->style.position, YGEdgeEnd, &YGValueUndefined)->unit !=
              YGUnitUndefined) ||
         YGComputedEdgeValue(node->style.position, trailing[axis], &YGValueUndefined)->unit !=
             YGUnitUndefined;
}

static float YGNodeLeadingPosition(const YGNodeRef node,
                                   const YGFlexDirection axis,
                                   const float axisSize) {
  if (YGFlexDirectionIsRow(axis)) {
    const YGValue *leadingPosition =
        YGComputedEdgeValue(node->style.position, YGEdgeStart, &YGValueUndefined);
    if (leadingPosition->unit != YGUnitUndefined) {
      return YGResolveValue(leadingPosition, axisSize);
    }
  }

  const YGValue *leadingPosition =
      YGComputedEdgeValue(node->style.position, leading[axis], &YGValueUndefined);

  return leadingPosition->unit == YGUnitUndefined ? 0.0f
                                                  : YGResolveValue(leadingPosition, axisSize);
}

static float YGNodeTrailingPosition(const YGNodeRef node,
                                    const YGFlexDirection axis,
                                    const float axisSize) {
  if (YGFlexDirectionIsRow(axis)) {
    const YGValue *trailingPosition =
        YGComputedEdgeValue(node->style.position, YGEdgeEnd, &YGValueUndefined);
    if (trailingPosition->unit != YGUnitUndefined) {
      return YGResolveValue(trailingPosition, axisSize);
    }
  }

  const YGValue *trailingPosition =
      YGComputedEdgeValue(node->style.position, trailing[axis], &YGValueUndefined);

  return trailingPosition->unit == YGUnitUndefined ? 0.0f
                                                   : YGResolveValue(trailingPosition, axisSize);
}

static float YGNodeBoundAxisWithinMinAndMax(const YGNodeRef node,
                                            const YGFlexDirection axis,
                                            const float value,
                                            const float axisSize) {
  float min = YGUndefined;
  float max = YGUndefined;

  if (YGFlexDirectionIsColumn(axis)) {
    min = YGResolveValue(&node->style.minDimensions[YGDimensionHeight], axisSize);
    max = YGResolveValue(&node->style.maxDimensions[YGDimensionHeight], axisSize);
  } else if (YGFlexDirectionIsRow(axis)) {
    min = YGResolveValue(&node->style.minDimensions[YGDimensionWidth], axisSize);
    max = YGResolveValue(&node->style.maxDimensions[YGDimensionWidth], axisSize);
  }

  float boundValue = value;

  if (!YGFloatIsUndefined(max) && max >= 0.0f && boundValue > max) {
    boundValue = max;
  }

  if (!YGFloatIsUndefined(min) && min >= 0.0f && boundValue < min) {
    boundValue = min;
  }

  return boundValue;
}

static inline YGValue *YGMarginLeadingValue(const YGNodeRef node, const YGFlexDirection axis) {
  if (YGFlexDirectionIsRow(axis) && node->style.margin[YGEdgeStart].unit != YGUnitUndefined) {
    return &node->style.margin[YGEdgeStart];
  } else {
    return &node->style.margin[leading[axis]];
  }
}

static inline YGValue *YGMarginTrailingValue(const YGNodeRef node, const YGFlexDirection axis) {
  if (YGFlexDirectionIsRow(axis) && node->style.margin[YGEdgeEnd].unit != YGUnitUndefined) {
    return &node->style.margin[YGEdgeEnd];
  } else {
    return &node->style.margin[trailing[axis]];
  }
}

// Like YGNodeBoundAxisWithinMinAndMax but also ensures that the value doesn't go
// below the
// padding and border amount.
static inline float YGNodeBoundAxis(const YGNodeRef node,
                                    const YGFlexDirection axis,
                                    const float value,
                                    const float axisSize,
                                    const float widthSize) {
  return fmaxf(YGNodeBoundAxisWithinMinAndMax(node, axis, value, axisSize),
               YGNodePaddingAndBorderForAxis(node, axis, widthSize));
}

static void YGNodeSetChildTrailingPosition(const YGNodeRef node,
                                           const YGNodeRef child,
                                           const YGFlexDirection axis) {
  const float size = child->layout.measuredDimensions[dim[axis]];
  child->layout.position[trailing[axis]] =
      node->layout.measuredDimensions[dim[axis]] - size - child->layout.position[pos[axis]];
}

// If both left and right are defined, then use left. Otherwise return
// +left or -right depending on which is defined.
static float YGNodeRelativePosition(const YGNodeRef node,
                                    const YGFlexDirection axis,
                                    const float axisSize) {
  return YGNodeIsLeadingPosDefined(node, axis) ? YGNodeLeadingPosition(node, axis, axisSize)
                                               : -YGNodeTrailingPosition(node, axis, axisSize);
}

static void YGConstrainMaxSizeForMode(const YGNodeRef node,
                                      const enum YGFlexDirection axis,
                                      const float parentAxisSize,
                                      const float parentWidth,
                                      YGMeasureMode *mode,
                                      float *size) {
  const float maxSize = YGResolveValue(&node->style.maxDimensions[dim[axis]], parentAxisSize) +
                        YGNodeMarginForAxis(node, axis, parentWidth);
  switch (*mode) {
    case YGMeasureModeExactly:
    case YGMeasureModeAtMost:
      *size = (YGFloatIsUndefined(maxSize) || *size < maxSize) ? *size : maxSize;
      break;
    case YGMeasureModeUndefined:
      if (!YGFloatIsUndefined(maxSize)) {
        *mode = YGMeasureModeAtMost;
        *size = maxSize;
      }
      break;
  }
}

static void YGNodeSetPosition(const YGNodeRef node,
                              const YGDirection direction,
                              const float mainSize,
                              const float crossSize,
                              const float parentWidth) {
  const YGFlexDirection mainAxis = YGResolveFlexDirection(node->style.flexDirection, direction);
  const YGFlexDirection crossAxis = YGFlexDirectionCross(mainAxis, direction);
  const float relativePositionMain = YGNodeRelativePosition(node, mainAxis, mainSize);
  const float relativePositionCross = YGNodeRelativePosition(node, crossAxis, crossSize);

  node->layout.position[leading[mainAxis]] =
      YGNodeLeadingMargin(node, mainAxis, parentWidth) + relativePositionMain;
  node->layout.position[trailing[mainAxis]] =
      YGNodeTrailingMargin(node, mainAxis, parentWidth) + relativePositionMain;
  node->layout.position[leading[crossAxis]] =
      YGNodeLeadingMargin(node, crossAxis, parentWidth) + relativePositionCross;
  node->layout.position[trailing[crossAxis]] =
      YGNodeTrailingMargin(node, crossAxis, parentWidth) + relativePositionCross;
}

static void YGNodeComputeFlexBasisForChild(const YGNodeRef node,
                                           const YGNodeRef child,
                                           const float width,
                                           const YGMeasureMode widthMode,
                                           const float height,
                                           const float parentWidth,
                                           const float parentHeight,
                                           const YGMeasureMode heightMode,
                                           const YGDirection direction,
                                           const YGConfigRef config) {
  const YGFlexDirection mainAxis = YGResolveFlexDirection(node->style.flexDirection, direction);
  const bool isMainAxisRow = YGFlexDirectionIsRow(mainAxis);
  const float mainAxisSize = isMainAxisRow ? width : height;
  const float mainAxisParentSize = isMainAxisRow ? parentWidth : parentHeight;

  float childWidth;
  float childHeight;
  YGMeasureMode childWidthMeasureMode;
  YGMeasureMode childHeightMeasureMode;

  const float resolvedFlexBasis =
      YGResolveValue(YGNodeResolveFlexBasisPtr(child), mainAxisParentSize);
  const bool isRowStyleDimDefined = YGNodeIsStyleDimDefined(child, YGFlexDirectionRow, parentWidth);
  const bool isColumnStyleDimDefined =
      YGNodeIsStyleDimDefined(child, YGFlexDirectionColumn, parentHeight);

  if (!YGFloatIsUndefined(resolvedFlexBasis) && !YGFloatIsUndefined(mainAxisSize)) {
    if (YGFloatIsUndefined(child->layout.computedFlexBasis) ||
        (YGConfigIsExperimentalFeatureEnabled(child->config, YGExperimentalFeatureWebFlexBasis) &&
         child->layout.computedFlexBasisGeneration != gCurrentGenerationCount)) {
      child->layout.computedFlexBasis =
          fmaxf(resolvedFlexBasis, YGNodePaddingAndBorderForAxis(child, mainAxis, parentWidth));
    }
  } else if (isMainAxisRow && isRowStyleDimDefined) {
    // The width is definite, so use that as the flex basis.
    child->layout.computedFlexBasis =
        fmaxf(YGResolveValue(child->resolvedDimensions[YGDimensionWidth], parentWidth),
              YGNodePaddingAndBorderForAxis(child, YGFlexDirectionRow, parentWidth));
  } else if (!isMainAxisRow && isColumnStyleDimDefined) {
    // The height is definite, so use that as the flex basis.
    child->layout.computedFlexBasis =
        fmaxf(YGResolveValue(child->resolvedDimensions[YGDimensionHeight], parentHeight),
              YGNodePaddingAndBorderForAxis(child, YGFlexDirectionColumn, parentWidth));
  } else {
    // Compute the flex basis and hypothetical main size (i.e. the clamped
    // flex basis).
    childWidth = YGUndefined;
    childHeight = YGUndefined;
    childWidthMeasureMode = YGMeasureModeUndefined;
    childHeightMeasureMode = YGMeasureModeUndefined;

    const float marginRow = YGNodeMarginForAxis(child, YGFlexDirectionRow, parentWidth);
    const float marginColumn = YGNodeMarginForAxis(child, YGFlexDirectionColumn, parentWidth);

    if (isRowStyleDimDefined) {
      childWidth =
          YGResolveValue(child->resolvedDimensions[YGDimensionWidth], parentWidth) + marginRow;
      childWidthMeasureMode = YGMeasureModeExactly;
    }
    if (isColumnStyleDimDefined) {
      childHeight =
          YGResolveValue(child->resolvedDimensions[YGDimensionHeight], parentHeight) + marginColumn;
      childHeightMeasureMode = YGMeasureModeExactly;
    }

    // The W3C spec doesn't say anything about the 'overflow' property,
    // but all major browsers appear to implement the following logic.
    if ((!isMainAxisRow && node->style.overflow == YGOverflowScroll) ||
        node->style.overflow != YGOverflowScroll) {
      if (YGFloatIsUndefined(childWidth) && !YGFloatIsUndefined(width)) {
        childWidth = width;
        childWidthMeasureMode = YGMeasureModeAtMost;
      }
    }

    if ((isMainAxisRow && node->style.overflow == YGOverflowScroll) ||
        node->style.overflow != YGOverflowScroll) {
      if (YGFloatIsUndefined(childHeight) && !YGFloatIsUndefined(height)) {
        childHeight = height;
        childHeightMeasureMode = YGMeasureModeAtMost;
      }
    }

    // If child has no defined size in the cross axis and is set to stretch,
    // set the cross
    // axis to be measured exactly with the available inner width
    if (!isMainAxisRow && !YGFloatIsUndefined(width) && !isRowStyleDimDefined &&
        widthMode == YGMeasureModeExactly && YGNodeAlignItem(node, child) == YGAlignStretch) {
      childWidth = width;
      childWidthMeasureMode = YGMeasureModeExactly;
    }
    if (isMainAxisRow && !YGFloatIsUndefined(height) && !isColumnStyleDimDefined &&
        heightMode == YGMeasureModeExactly && YGNodeAlignItem(node, child) == YGAlignStretch) {
      childHeight = height;
      childHeightMeasureMode = YGMeasureModeExactly;
    }

    if (!YGFloatIsUndefined(child->style.aspectRatio)) {
      if (!isMainAxisRow && childWidthMeasureMode == YGMeasureModeExactly) {
        child->layout.computedFlexBasis =
            fmaxf((childWidth - marginRow) / child->style.aspectRatio,
                  YGNodePaddingAndBorderForAxis(child, YGFlexDirectionColumn, parentWidth));
        return;
      } else if (isMainAxisRow && childHeightMeasureMode == YGMeasureModeExactly) {
        child->layout.computedFlexBasis =
            fmaxf((childHeight - marginColumn) * child->style.aspectRatio,
                  YGNodePaddingAndBorderForAxis(child, YGFlexDirectionRow, parentWidth));
        return;
      }
    }

    YGConstrainMaxSizeForMode(
        child, YGFlexDirectionRow, parentWidth, parentWidth, &childWidthMeasureMode, &childWidth);
    YGConstrainMaxSizeForMode(
        child, YGFlexDirectionColumn, parentHeight, parentWidth, &childHeightMeasureMode, &childHeight);

    // Measure the child
    YGLayoutNodeInternal(child,
                         childWidth,
                         childHeight,
                         direction,
                         childWidthMeasureMode,
                         childHeightMeasureMode,
                         parentWidth,
                         parentHeight,
                         false,
                         "measure",
                         config);

    child->layout.computedFlexBasis =
        fmaxf(child->layout.measuredDimensions[dim[mainAxis]],
              YGNodePaddingAndBorderForAxis(child, mainAxis, parentWidth));
  }

  child->layout.computedFlexBasisGeneration = gCurrentGenerationCount;
}

static void YGNodeAbsoluteLayoutChild(const YGNodeRef node,
                                      const YGNodeRef child,
                                      const float width,
                                      const YGMeasureMode widthMode,
                                      const float height,
                                      const YGDirection direction,
                                      const YGConfigRef config) {
  const YGFlexDirection mainAxis = YGResolveFlexDirection(node->style.flexDirection, direction);
  const YGFlexDirection crossAxis = YGFlexDirectionCross(mainAxis, direction);
  const bool isMainAxisRow = YGFlexDirectionIsRow(mainAxis);

  float childWidth = YGUndefined;
  float childHeight = YGUndefined;
  YGMeasureMode childWidthMeasureMode = YGMeasureModeUndefined;
  YGMeasureMode childHeightMeasureMode = YGMeasureModeUndefined;

  const float marginRow = YGNodeMarginForAxis(child, YGFlexDirectionRow, width);
  const float marginColumn = YGNodeMarginForAxis(child, YGFlexDirectionColumn, width);

  if (YGNodeIsStyleDimDefined(child, YGFlexDirectionRow, width)) {
    childWidth = YGResolveValue(child->resolvedDimensions[YGDimensionWidth], width) + marginRow;
  } else {
    // If the child doesn't have a specified width, compute the width based
    // on the left/right
    // offsets if they're defined.
    if (YGNodeIsLeadingPosDefined(child, YGFlexDirectionRow) &&
        YGNodeIsTrailingPosDefined(child, YGFlexDirectionRow)) {
      childWidth = node->layout.measuredDimensions[YGDimensionWidth] -
                   (YGNodeLeadingBorder(node, YGFlexDirectionRow) +
                    YGNodeTrailingBorder(node, YGFlexDirectionRow)) -
                   (YGNodeLeadingPosition(child, YGFlexDirectionRow, width) +
                    YGNodeTrailingPosition(child, YGFlexDirectionRow, width));
      childWidth = YGNodeBoundAxis(child, YGFlexDirectionRow, childWidth, width, width);
    }
  }

  if (YGNodeIsStyleDimDefined(child, YGFlexDirectionColumn, height)) {
    childHeight =
        YGResolveValue(child->resolvedDimensions[YGDimensionHeight], height) + marginColumn;
  } else {
    // If the child doesn't have a specified height, compute the height
    // based on the top/bottom
    // offsets if they're defined.
    if (YGNodeIsLeadingPosDefined(child, YGFlexDirectionColumn) &&
        YGNodeIsTrailingPosDefined(child, YGFlexDirectionColumn)) {
      childHeight = node->layout.measuredDimensions[YGDimensionHeight] -
                    (YGNodeLeadingBorder(node, YGFlexDirectionColumn) +
                     YGNodeTrailingBorder(node, YGFlexDirectionColumn)) -
                    (YGNodeLeadingPosition(child, YGFlexDirectionColumn, height) +
                     YGNodeTrailingPosition(child, YGFlexDirectionColumn, height));
      childHeight = YGNodeBoundAxis(child, YGFlexDirectionColumn, childHeight, height, width);
    }
  }

  // Exactly one dimension needs to be defined for us to be able to do aspect ratio
  // calculation. One dimension being the anchor and the other being flexible.
  if (YGFloatIsUndefined(childWidth) ^ YGFloatIsUndefined(childHeight)) {
    if (!YGFloatIsUndefined(child->style.aspectRatio)) {
      if (YGFloatIsUndefined(childWidth)) {
        childWidth =
            marginRow + fmaxf((childHeight - marginColumn) * child->style.aspectRatio,
                              YGNodePaddingAndBorderForAxis(child, YGFlexDirectionColumn, width));
      } else if (YGFloatIsUndefined(childHeight)) {
        childHeight =
            marginColumn + fmaxf((childWidth - marginRow) / child->style.aspectRatio,
                                 YGNodePaddingAndBorderForAxis(child, YGFlexDirectionRow, width));
      }
    }
  }

  // If we're still missing one or the other dimension, measure the content.
  if (YGFloatIsUndefined(childWidth) || YGFloatIsUndefined(childHeight)) {
    childWidthMeasureMode =
        YGFloatIsUndefined(childWidth) ? YGMeasureModeUndefined : YGMeasureModeExactly;
    childHeightMeasureMode =
        YGFloatIsUndefined(childHeight) ? YGMeasureModeUndefined : YGMeasureModeExactly;

    // If the size of the parent is defined then try to constrain the absolute child to that size
    // as well. This allows text within the absolute child to wrap to the size of its parent.
    // This is the same behavior as many browsers implement.
    if (!isMainAxisRow && YGFloatIsUndefined(childWidth) && widthMode != YGMeasureModeUndefined &&
        width > 0) {
      childWidth = width;
      childWidthMeasureMode = YGMeasureModeAtMost;
    }

    YGLayoutNodeInternal(child,
                         childWidth,
                         childHeight,
                         direction,
                         childWidthMeasureMode,
                         childHeightMeasureMode,
                         childWidth,
                         childHeight,
                         false,
                         "abs-measure",
                         config);
    childWidth = child->layout.measuredDimensions[YGDimensionWidth] +
                 YGNodeMarginForAxis(child, YGFlexDirectionRow, width);
    childHeight = child->layout.measuredDimensions[YGDimensionHeight] +
                  YGNodeMarginForAxis(child, YGFlexDirectionColumn, width);
  }

  YGLayoutNodeInternal(child,
                       childWidth,
                       childHeight,
                       direction,
                       YGMeasureModeExactly,
                       YGMeasureModeExactly,
                       childWidth,
                       childHeight,
                       true,
                       "abs-layout",
                       config);

  if (YGNodeIsTrailingPosDefined(child, mainAxis) && !YGNodeIsLeadingPosDefined(child, mainAxis)) {
    child->layout.position[leading[mainAxis]] = node->layout.measuredDimensions[dim[mainAxis]] -
                                                child->layout.measuredDimensions[dim[mainAxis]] -
                                                YGNodeTrailingBorder(node, mainAxis) -
                                                YGNodeTrailingPosition(child, mainAxis, width);
  } else if (!YGNodeIsLeadingPosDefined(child, mainAxis) &&
             node->style.justifyContent == YGJustifyCenter) {
    child->layout.position[leading[mainAxis]] = (node->layout.measuredDimensions[dim[mainAxis]] -
                                                 child->layout.measuredDimensions[dim[mainAxis]]) /
                                                2.0f;
  } else if (!YGNodeIsLeadingPosDefined(child, mainAxis) &&
             node->style.justifyContent == YGJustifyFlexEnd) {
    child->layout.position[leading[mainAxis]] = (node->layout.measuredDimensions[dim[mainAxis]] -
                                                 child->layout.measuredDimensions[dim[mainAxis]]);
  }

  if (YGNodeIsTrailingPosDefined(child, crossAxis) &&
      !YGNodeIsLeadingPosDefined(child, crossAxis)) {
    child->layout.position[leading[crossAxis]] = node->layout.measuredDimensions[dim[crossAxis]] -
                                                 child->layout.measuredDimensions[dim[crossAxis]] -
                                                 YGNodeTrailingBorder(node, crossAxis) -
                                                 YGNodeTrailingPosition(child, crossAxis, width);
  } else if (!YGNodeIsLeadingPosDefined(child, crossAxis) &&
             YGNodeAlignItem(node, child) == YGAlignCenter) {
    child->layout.position[leading[crossAxis]] =
        (node->layout.measuredDimensions[dim[crossAxis]] -
         child->layout.measuredDimensions[dim[crossAxis]]) /
        2.0f;
  } else if (!YGNodeIsLeadingPosDefined(child, crossAxis) &&
             YGNodeAlignItem(node, child) == YGAlignFlexEnd) {
    child->layout.position[leading[crossAxis]] = (node->layout.measuredDimensions[dim[crossAxis]] -
                                                  child->layout.measuredDimensions[dim[crossAxis]]);
  }
}

static void YGNodeWithMeasureFuncSetMeasuredDimensions(const YGNodeRef node,
                                                       const float availableWidth,
                                                       const float availableHeight,
                                                       const YGMeasureMode widthMeasureMode,
                                                       const YGMeasureMode heightMeasureMode,
                                                       const float parentWidth,
                                                       const float parentHeight) {
  YG_ASSERT(node->measure, "Expected node to have custom measure function");

  const float paddingAndBorderAxisRow =
      YGNodePaddingAndBorderForAxis(node, YGFlexDirectionRow, availableWidth);
  const float paddingAndBorderAxisColumn =
      YGNodePaddingAndBorderForAxis(node, YGFlexDirectionColumn, availableWidth);
  const float marginAxisRow = YGNodeMarginForAxis(node, YGFlexDirectionRow, availableWidth);
  const float marginAxisColumn = YGNodeMarginForAxis(node, YGFlexDirectionColumn, availableWidth);

  const float innerWidth = availableWidth - marginAxisRow - paddingAndBorderAxisRow;
  const float innerHeight = availableHeight - marginAxisColumn - paddingAndBorderAxisColumn;

  if (widthMeasureMode == YGMeasureModeExactly && heightMeasureMode == YGMeasureModeExactly) {
    // Don't bother sizing the text if both dimensions are already defined.
    node->layout.measuredDimensions[YGDimensionWidth] = YGNodeBoundAxis(
        node, YGFlexDirectionRow, availableWidth - marginAxisRow, parentWidth, parentWidth);
    node->layout.measuredDimensions[YGDimensionHeight] = YGNodeBoundAxis(
        node, YGFlexDirectionColumn, availableHeight - marginAxisColumn, parentHeight, parentWidth);
  } else if (innerWidth <= 0.0f || innerHeight <= 0.0f) {
    // Don't bother sizing the text if there's no horizontal or vertical
    // space.
    node->layout.measuredDimensions[YGDimensionWidth] =
        YGNodeBoundAxis(node, YGFlexDirectionRow, 0.0f, availableWidth, availableWidth);
    node->layout.measuredDimensions[YGDimensionHeight] =
        YGNodeBoundAxis(node, YGFlexDirectionColumn, 0.0f, availableHeight, availableWidth);
  } else {
    // Measure the text under the current constraints.
    const YGSize measuredSize =
        node->measure(node, innerWidth, widthMeasureMode, innerHeight, heightMeasureMode);

    node->layout.measuredDimensions[YGDimensionWidth] =
        YGNodeBoundAxis(node,
                        YGFlexDirectionRow,
                        (widthMeasureMode == YGMeasureModeUndefined ||
                         widthMeasureMode == YGMeasureModeAtMost)
                            ? measuredSize.width + paddingAndBorderAxisRow
                            : availableWidth - marginAxisRow,
                        availableWidth,
                        availableWidth);
    node->layout.measuredDimensions[YGDimensionHeight] =
        YGNodeBoundAxis(node,
                        YGFlexDirectionColumn,
                        (heightMeasureMode == YGMeasureModeUndefined ||
                         heightMeasureMode == YGMeasureModeAtMost)
                            ? measuredSize.height + paddingAndBorderAxisColumn
                            : availableHeight - marginAxisColumn,
                        availableHeight,
                        availableWidth);
  }
}

// For nodes with no children, use the available values if they were provided,
// or the minimum size as indicated by the padding and border sizes.
static void YGNodeEmptyContainerSetMeasuredDimensions(const YGNodeRef node,
                                                      const float availableWidth,
                                                      const float availableHeight,
                                                      const YGMeasureMode widthMeasureMode,
                                                      const YGMeasureMode heightMeasureMode,
                                                      const float parentWidth,
                                                      const float parentHeight) {
  const float paddingAndBorderAxisRow =
      YGNodePaddingAndBorderForAxis(node, YGFlexDirectionRow, parentWidth);
  const float paddingAndBorderAxisColumn =
      YGNodePaddingAndBorderForAxis(node, YGFlexDirectionColumn, parentWidth);
  const float marginAxisRow = YGNodeMarginForAxis(node, YGFlexDirectionRow, parentWidth);
  const float marginAxisColumn = YGNodeMarginForAxis(node, YGFlexDirectionColumn, parentWidth);

  node->layout.measuredDimensions[YGDimensionWidth] =
      YGNodeBoundAxis(node,
                      YGFlexDirectionRow,
                      (widthMeasureMode == YGMeasureModeUndefined ||
                       widthMeasureMode == YGMeasureModeAtMost)
                          ? paddingAndBorderAxisRow
                          : availableWidth - marginAxisRow,
                      parentWidth,
                      parentWidth);
  node->layout.measuredDimensions[YGDimensionHeight] =
      YGNodeBoundAxis(node,
                      YGFlexDirectionColumn,
                      (heightMeasureMode == YGMeasureModeUndefined ||
                       heightMeasureMode == YGMeasureModeAtMost)
                          ? paddingAndBorderAxisColumn
                          : availableHeight - marginAxisColumn,
                      parentHeight,
                      parentWidth);
}

static bool YGNodeFixedSizeSetMeasuredDimensions(const YGNodeRef node,
                                                 const float availableWidth,
                                                 const float availableHeight,
                                                 const YGMeasureMode widthMeasureMode,
                                                 const YGMeasureMode heightMeasureMode,
                                                 const float parentWidth,
                                                 const float parentHeight) {
  if ((widthMeasureMode == YGMeasureModeAtMost && availableWidth <= 0.0f) ||
      (heightMeasureMode == YGMeasureModeAtMost && availableHeight <= 0.0f) ||
      (widthMeasureMode == YGMeasureModeExactly && heightMeasureMode == YGMeasureModeExactly)) {
    const float marginAxisColumn = YGNodeMarginForAxis(node, YGFlexDirectionColumn, parentWidth);
    const float marginAxisRow = YGNodeMarginForAxis(node, YGFlexDirectionRow, parentWidth);

    node->layout.measuredDimensions[YGDimensionWidth] =
        YGNodeBoundAxis(node,
                        YGFlexDirectionRow,
                        YGFloatIsUndefined(availableWidth) ||
                                (widthMeasureMode == YGMeasureModeAtMost && availableWidth < 0.0f)
                            ? 0.0f
                            : availableWidth - marginAxisRow,
                        parentWidth,
                        parentWidth);

    node->layout.measuredDimensions[YGDimensionHeight] =
        YGNodeBoundAxis(node,
                        YGFlexDirectionColumn,
                        YGFloatIsUndefined(availableHeight) ||
                                (heightMeasureMode == YGMeasureModeAtMost && availableHeight < 0.0f)
                            ? 0.0f
                            : availableHeight - marginAxisColumn,
                        parentHeight,
                        parentWidth);

    return true;
  }

  return false;
}

static void YGZeroOutLayoutRecursivly(const YGNodeRef node) {
  node->layout.dimensions[YGDimensionHeight] = 0;
  node->layout.dimensions[YGDimensionWidth] = 0;
  node->layout.position[YGEdgeTop] = 0;
  node->layout.position[YGEdgeBottom] = 0;
  node->layout.position[YGEdgeLeft] = 0;
  node->layout.position[YGEdgeRight] = 0;
  node->layout.cachedLayout.availableHeight = 0;
  node->layout.cachedLayout.availableWidth = 0;
  node->layout.cachedLayout.heightMeasureMode = YGMeasureModeExactly;
  node->layout.cachedLayout.widthMeasureMode = YGMeasureModeExactly;
  node->layout.cachedLayout.computedWidth = 0;
  node->layout.cachedLayout.computedHeight = 0;
  node->hasNewLayout = true;
  const uint32_t childCount = YGNodeGetChildCount(node);
  for (uint32_t i = 0; i < childCount; i++) {
    const YGNodeRef child = YGNodeListGet(node->children, i);
    YGZeroOutLayoutRecursivly(child);
  }
}

//
// This is the main routine that implements a subset of the flexbox layout
// algorithm
// described in the W3C YG documentation: https://www.w3.org/TR/YG3-flexbox/.
//
// Limitations of this algorithm, compared to the full standard:
//  * Display property is always assumed to be 'flex' except for Text nodes,
//  which
//    are assumed to be 'inline-flex'.
//  * The 'zIndex' property (or any form of z ordering) is not supported. Nodes
//  are
//    stacked in document order.
//  * The 'order' property is not supported. The order of flex items is always
//  defined
//    by document order.
//  * The 'visibility' property is always assumed to be 'visible'. Values of
//  'collapse'
//    and 'hidden' are not supported.
//  * There is no support for forced breaks.
//  * It does not support vertical inline directions (top-to-bottom or
//  bottom-to-top text).
//
// Deviations from standard:
//  * Section 4.5 of the spec indicates that all flex items have a default
//  minimum
//    main size. For text blocks, for example, this is the width of the widest
//    word.
//    Calculating the minimum width is expensive, so we forego it and assume a
//    default
//    minimum main size of 0.
//  * Min/Max sizes in the main axis are not honored when resolving flexible
//  lengths.
//  * The spec indicates that the default value for 'flexDirection' is 'row',
//  but
//    the algorithm below assumes a default of 'column'.
//
// Input parameters:
//    - node: current node to be sized and layed out
//    - availableWidth & availableHeight: available size to be used for sizing
//    the node
//      or YGUndefined if the size is not available; interpretation depends on
//      layout
//      flags
//    - parentDirection: the inline (text) direction within the parent
//    (left-to-right or
//      right-to-left)
//    - widthMeasureMode: indicates the sizing rules for the width (see below
//    for explanation)
//    - heightMeasureMode: indicates the sizing rules for the height (see below
//    for explanation)
//    - performLayout: specifies whether the caller is interested in just the
//    dimensions
//      of the node or it requires the entire node and its subtree to be layed
//      out
//      (with final positions)
//
// Details:
//    This routine is called recursively to lay out subtrees of flexbox
//    elements. It uses the
//    information in node.style, which is treated as a read-only input. It is
//    responsible for
//    setting the layout.direction and layout.measuredDimensions fields for the
//    input node as well
//    as the layout.position and layout.lineIndex fields for its child nodes.
//    The
//    layout.measuredDimensions field includes any border or padding for the
//    node but does
//    not include margins.
//
//    The spec describes four different layout modes: "fill available", "max
//    content", "min
//    content",
//    and "fit content". Of these, we don't use "min content" because we don't
//    support default
//    minimum main sizes (see above for details). Each of our measure modes maps
//    to a layout mode
//    from the spec (https://www.w3.org/TR/YG3-sizing/#terms):
//      - YGMeasureModeUndefined: max content
//      - YGMeasureModeExactly: fill available
//      - YGMeasureModeAtMost: fit content
//
//    When calling YGNodelayoutImpl and YGLayoutNodeInternal, if the caller passes
//    an available size of
//    undefined then it must also pass a measure mode of YGMeasureModeUndefined
//    in that dimension.
//
static void YGNodelayoutImpl(const YGNodeRef node,
                             const float availableWidth,
                             const float availableHeight,
                             const YGDirection parentDirection,
                             const YGMeasureMode widthMeasureMode,
                             const YGMeasureMode heightMeasureMode,
                             const float parentWidth,
                             const float parentHeight,
                             const bool performLayout,
                             const YGConfigRef config) {
  YG_ASSERT(YGFloatIsUndefined(availableWidth) ? widthMeasureMode == YGMeasureModeUndefined : true,
            "availableWidth is indefinite so widthMeasureMode must be "
            "YGMeasureModeUndefined");
  YG_ASSERT(YGFloatIsUndefined(availableHeight) ? heightMeasureMode == YGMeasureModeUndefined
                                                : true,
            "availableHeight is indefinite so heightMeasureMode must be "
            "YGMeasureModeUndefined");

  // Set the resolved resolution in the node's layout.
  const YGDirection direction = YGNodeResolveDirection(node, parentDirection);
  node->layout.direction = direction;

  const YGFlexDirection flexRowDirection = YGResolveFlexDirection(YGFlexDirectionRow, direction);
  const YGFlexDirection flexColumnDirection =
      YGResolveFlexDirection(YGFlexDirectionColumn, direction);

  node->layout.margin[YGEdgeStart] = YGNodeLeadingMargin(node, flexRowDirection, parentWidth);
  node->layout.margin[YGEdgeEnd] = YGNodeTrailingMargin(node, flexRowDirection, parentWidth);
  node->layout.margin[YGEdgeTop] = YGNodeLeadingMargin(node, flexColumnDirection, parentWidth);
  node->layout.margin[YGEdgeBottom] = YGNodeTrailingMargin(node, flexColumnDirection, parentWidth);

  node->layout.border[YGEdgeStart] = YGNodeLeadingBorder(node, flexRowDirection);
  node->layout.border[YGEdgeEnd] = YGNodeTrailingBorder(node, flexRowDirection);
  node->layout.border[YGEdgeTop] = YGNodeLeadingBorder(node, flexColumnDirection);
  node->layout.border[YGEdgeBottom] = YGNodeTrailingBorder(node, flexColumnDirection);

  node->layout.padding[YGEdgeStart] = YGNodeLeadingPadding(node, flexRowDirection, parentWidth);
  node->layout.padding[YGEdgeEnd] = YGNodeTrailingPadding(node, flexRowDirection, parentWidth);
  node->layout.padding[YGEdgeTop] = YGNodeLeadingPadding(node, flexColumnDirection, parentWidth);
  node->layout.padding[YGEdgeBottom] =
      YGNodeTrailingPadding(node, flexColumnDirection, parentWidth);

  if (node->measure) {
    YGNodeWithMeasureFuncSetMeasuredDimensions(node,
                                               availableWidth,
                                               availableHeight,
                                               widthMeasureMode,
                                               heightMeasureMode,
                                               parentWidth,
                                               parentHeight);
    return;
  }

  const uint32_t childCount = YGNodeListCount(node->children);
  if (childCount == 0) {
    YGNodeEmptyContainerSetMeasuredDimensions(node,
                                              availableWidth,
                                              availableHeight,
                                              widthMeasureMode,
                                              heightMeasureMode,
                                              parentWidth,
                                              parentHeight);
    return;
  }

  // If we're not being asked to perform a full layout we can skip the algorithm if we already know
  // the size
  if (!performLayout && YGNodeFixedSizeSetMeasuredDimensions(node,
                                                             availableWidth,
                                                             availableHeight,
                                                             widthMeasureMode,
                                                             heightMeasureMode,
                                                             parentWidth,
                                                             parentHeight)) {
    return;
  }

  // STEP 1: CALCULATE VALUES FOR REMAINDER OF ALGORITHM
  const YGFlexDirection mainAxis = YGResolveFlexDirection(node->style.flexDirection, direction);
  const YGFlexDirection crossAxis = YGFlexDirectionCross(mainAxis, direction);
  const bool isMainAxisRow = YGFlexDirectionIsRow(mainAxis);
  const YGJustify justifyContent = node->style.justifyContent;
  const bool isNodeFlexWrap = node->style.flexWrap != YGWrapNoWrap;

  const float mainAxisParentSize = isMainAxisRow ? parentWidth : parentHeight;
  const float crossAxisParentSize = isMainAxisRow ? parentHeight : parentWidth;

  YGNodeRef firstAbsoluteChild = NULL;
  YGNodeRef currentAbsoluteChild = NULL;

  const float leadingPaddingAndBorderMain =
      YGNodeLeadingPaddingAndBorder(node, mainAxis, parentWidth);
  const float trailingPaddingAndBorderMain =
      YGNodeTrailingPaddingAndBorder(node, mainAxis, parentWidth);
  const float leadingPaddingAndBorderCross =
      YGNodeLeadingPaddingAndBorder(node, crossAxis, parentWidth);
  const float paddingAndBorderAxisMain = YGNodePaddingAndBorderForAxis(node, mainAxis, parentWidth);
  const float paddingAndBorderAxisCross =
      YGNodePaddingAndBorderForAxis(node, crossAxis, parentWidth);

  YGMeasureMode measureModeMainDim = isMainAxisRow ? widthMeasureMode : heightMeasureMode;
  YGMeasureMode measureModeCrossDim = isMainAxisRow ? heightMeasureMode : widthMeasureMode;

  const float paddingAndBorderAxisRow =
      isMainAxisRow ? paddingAndBorderAxisMain : paddingAndBorderAxisCross;
  const float paddingAndBorderAxisColumn =
      isMainAxisRow ? paddingAndBorderAxisCross : paddingAndBorderAxisMain;

  const float marginAxisRow = YGNodeMarginForAxis(node, YGFlexDirectionRow, parentWidth);
  const float marginAxisColumn = YGNodeMarginForAxis(node, YGFlexDirectionColumn, parentWidth);

  // STEP 2: DETERMINE AVAILABLE SIZE IN MAIN AND CROSS DIRECTIONS
  const float minInnerWidth =
      YGResolveValue(&node->style.minDimensions[YGDimensionWidth], parentWidth) - marginAxisRow -
      paddingAndBorderAxisRow;
  const float maxInnerWidth =
      YGResolveValue(&node->style.maxDimensions[YGDimensionWidth], parentWidth) - marginAxisRow -
      paddingAndBorderAxisRow;
  const float minInnerHeight =
      YGResolveValue(&node->style.minDimensions[YGDimensionHeight], parentHeight) -
      marginAxisColumn - paddingAndBorderAxisColumn;
  const float maxInnerHeight =
      YGResolveValue(&node->style.maxDimensions[YGDimensionHeight], parentHeight) -
      marginAxisColumn - paddingAndBorderAxisColumn;
  const float minInnerMainDim = isMainAxisRow ? minInnerWidth : minInnerHeight;
  const float maxInnerMainDim = isMainAxisRow ? maxInnerWidth : maxInnerHeight;

  // Max dimension overrides predefined dimension value; Min dimension in turn overrides both of the
  // above
  float availableInnerWidth = availableWidth - marginAxisRow - paddingAndBorderAxisRow;
  if (!YGFloatIsUndefined(availableInnerWidth)) {
    // We want to make sure our available width does not violate min and max constraints
    availableInnerWidth = fmaxf(fminf(availableInnerWidth, maxInnerWidth), minInnerWidth);
  }

  float availableInnerHeight = availableHeight - marginAxisColumn - paddingAndBorderAxisColumn;
  if (!YGFloatIsUndefined(availableInnerHeight)) {
    // We want to make sure our available height does not violate min and max constraints
    availableInnerHeight = fmaxf(fminf(availableInnerHeight, maxInnerHeight), minInnerHeight);
  }

  float availableInnerMainDim = isMainAxisRow ? availableInnerWidth : availableInnerHeight;
  const float availableInnerCrossDim = isMainAxisRow ? availableInnerHeight : availableInnerWidth;

  // If there is only one child with flexGrow + flexShrink it means we can set the
  // computedFlexBasis to 0 instead of measuring and shrinking / flexing the child to exactly
  // match the remaining space
  YGNodeRef singleFlexChild = NULL;
  if (measureModeMainDim == YGMeasureModeExactly) {
    for (uint32_t i = 0; i < childCount; i++) {
      const YGNodeRef child = YGNodeGetChild(node, i);
      if (singleFlexChild) {
        if (YGNodeIsFlex(child)) {
          // There is already a flexible child, abort.
          singleFlexChild = NULL;
          break;
        }
      } else if (YGResolveFlexGrow(child) > 0.0f && YGNodeResolveFlexShrink(child) > 0.0f) {
        singleFlexChild = child;
      }
    }
  }

  float totalFlexBasis = 0;

  // STEP 3: DETERMINE FLEX BASIS FOR EACH ITEM
  for (uint32_t i = 0; i < childCount; i++) {
    const YGNodeRef child = YGNodeListGet(node->children, i);
    if (child->style.display == YGDisplayNone) {
      YGZeroOutLayoutRecursivly(child);
      child->hasNewLayout = true;
      child->isDirty = false;
      continue;
    }
    YGResolveDimensions(child);
    if (performLayout) {
      // Set the initial position (relative to the parent).
      const YGDirection childDirection = YGNodeResolveDirection(child, direction);
      YGNodeSetPosition(child,
                        childDirection,
                        availableInnerMainDim,
                        availableInnerCrossDim,
                        availableInnerWidth);
    }

    // Absolute-positioned children don't participate in flex layout. Add them
    // to a list that we can process later.
    if (child->style.positionType == YGPositionTypeAbsolute) {
      // Store a private linked list of absolutely positioned children
      // so that we can efficiently traverse them later.
      if (firstAbsoluteChild == NULL) {
        firstAbsoluteChild = child;
      }
      if (currentAbsoluteChild != NULL) {
        currentAbsoluteChild->nextChild = child;
      }
      currentAbsoluteChild = child;
      child->nextChild = NULL;
    } else {
      if (child == singleFlexChild) {
        child->layout.computedFlexBasisGeneration = gCurrentGenerationCount;
        child->layout.computedFlexBasis = 0;
      } else {
        YGNodeComputeFlexBasisForChild(node,
                                       child,
                                       availableInnerWidth,
                                       widthMeasureMode,
                                       availableInnerHeight,
                                       availableInnerWidth,
                                       availableInnerHeight,
                                       heightMeasureMode,
                                       direction,
                                       config);
      }
    }

    totalFlexBasis += child->layout.computedFlexBasis;
  }

  const bool flexBasisOverflows =
      measureModeMainDim == YGMeasureModeUndefined ? false : totalFlexBasis > availableInnerMainDim;
  if (isNodeFlexWrap && flexBasisOverflows && measureModeMainDim == YGMeasureModeAtMost) {
    measureModeMainDim = YGMeasureModeExactly;
  }

  // STEP 4: COLLECT FLEX ITEMS INTO FLEX LINES

  // Indexes of children that represent the first and last items in the line.
  uint32_t startOfLineIndex = 0;
  uint32_t endOfLineIndex = 0;

  // Number of lines.
  uint32_t lineCount = 0;

  // Accumulated cross dimensions of all lines so far.
  float totalLineCrossDim = 0;

  // Max main dimension of all the lines.
  float maxLineMainDim = 0;

  for (; endOfLineIndex < childCount; lineCount++, startOfLineIndex = endOfLineIndex) {
    // Number of items on the currently line. May be different than the
    // difference
    // between start and end indicates because we skip over absolute-positioned
    // items.
    uint32_t itemsOnLine = 0;

    // sizeConsumedOnCurrentLine is accumulation of the dimensions and margin
    // of all the children on the current line. This will be used in order to
    // either set the dimensions of the node if none already exist or to compute
    // the remaining space left for the flexible children.
    float sizeConsumedOnCurrentLine = 0;

    float totalFlexGrowFactors = 0;
    float totalFlexShrinkScaledFactors = 0;

    // Maintain a linked list of the child nodes that can shrink and/or grow.
    YGNodeRef firstRelativeChild = NULL;
    YGNodeRef currentRelativeChild = NULL;

    // Add items to the current line until it's full or we run out of items.
    for (uint32_t i = startOfLineIndex; i < childCount; i++, endOfLineIndex++) {
      const YGNodeRef child = YGNodeListGet(node->children, i);
      if (child->style.display == YGDisplayNone) {
        continue;
      }
      child->lineIndex = lineCount;

      if (child->style.positionType != YGPositionTypeAbsolute) {
        const float outerFlexBasis =
            fmaxf(YGResolveValue(&child->style.minDimensions[dim[mainAxis]], mainAxisParentSize),
                  child->layout.computedFlexBasis) +
            YGNodeMarginForAxis(child, mainAxis, availableInnerWidth);

        // If this is a multi-line flow and this item pushes us over the
        // available size, we've
        // hit the end of the current line. Break out of the loop and lay out
        // the current line.
        if (sizeConsumedOnCurrentLine + outerFlexBasis > availableInnerMainDim && isNodeFlexWrap &&
            itemsOnLine > 0) {
          break;
        }

        sizeConsumedOnCurrentLine += outerFlexBasis;
        itemsOnLine++;

        if (YGNodeIsFlex(child)) {
          totalFlexGrowFactors += YGResolveFlexGrow(child);

          // Unlike the grow factor, the shrink factor is scaled relative to the
          // child
          // dimension.
          totalFlexShrinkScaledFactors +=
              -YGNodeResolveFlexShrink(child) * child->layout.computedFlexBasis;
        }

        // Store a private linked list of children that need to be layed out.
        if (firstRelativeChild == NULL) {
          firstRelativeChild = child;
        }
        if (currentRelativeChild != NULL) {
          currentRelativeChild->nextChild = child;
        }
        currentRelativeChild = child;
        child->nextChild = NULL;
      }
    }

    // If we don't need to measure the cross axis, we can skip the entire flex
    // step.
    const bool canSkipFlex = !performLayout && measureModeCrossDim == YGMeasureModeExactly;

    // In order to position the elements in the main axis, we have two
    // controls. The space between the beginning and the first element
    // and the space between each two elements.
    float leadingMainDim = 0;
    float betweenMainDim = 0;

    // STEP 5: RESOLVING FLEXIBLE LENGTHS ON MAIN AXIS
    // Calculate the remaining available space that needs to be allocated.
    // If the main dimension size isn't known, it is computed based on
    // the line length, so there's no more space left to distribute.

