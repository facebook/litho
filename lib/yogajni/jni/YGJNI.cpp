/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <fb/fbjni.h>
#include <iostream>
#include <yoga/Yoga.h>

using namespace facebook::jni;
using namespace std;

static inline weak_ref<jobject> *YGNodeJobject(YGNodeRef node) {
  return reinterpret_cast<weak_ref<jobject> *>(YGNodeGetContext(node));
}

static void YGTransferLayoutDirection(YGNodeRef node, alias_ref<jobject> javaNode) {
  static auto layoutDirectionField = javaNode->getClass()->getField<jint>("mLayoutDirection");
  javaNode->setFieldValue(layoutDirectionField, static_cast<jint>(YGNodeLayoutGetDirection(node)));
}

static void YGTransferLayoutOutputsRecursive(YGNodeRef root) {
  if (auto obj = YGNodeJobject(root)->lockLocal()) {
    static auto widthField = obj->getClass()->getField<jfloat>("mWidth");
    static auto heightField = obj->getClass()->getField<jfloat>("mHeight");
    static auto leftField = obj->getClass()->getField<jfloat>("mLeft");
    static auto topField = obj->getClass()->getField<jfloat>("mTop");

    static auto marginLeftField = obj->getClass()->getField<jfloat>("mMarginLeft");
    static auto marginTopField = obj->getClass()->getField<jfloat>("mMarginTop");
    static auto marginRightField = obj->getClass()->getField<jfloat>("mMarginRight");
    static auto marginBottomField = obj->getClass()->getField<jfloat>("mMarginBottom");

    static auto paddingLeftField = obj->getClass()->getField<jfloat>("mPaddingLeft");
    static auto paddingTopField = obj->getClass()->getField<jfloat>("mPaddingTop");
    static auto paddingRightField = obj->getClass()->getField<jfloat>("mPaddingRight");
    static auto paddingBottomField = obj->getClass()->getField<jfloat>("mPaddingBottom");

    static auto borderLeftField = obj->getClass()->getField<jfloat>("mBorderLeft");
    static auto borderTopField = obj->getClass()->getField<jfloat>("mBorderTop");
    static auto borderRightField = obj->getClass()->getField<jfloat>("mBorderRight");
    static auto borderBottomField = obj->getClass()->getField<jfloat>("mBorderBottom");

    obj->setFieldValue(widthField, YGNodeLayoutGetWidth(root));
    obj->setFieldValue(heightField, YGNodeLayoutGetHeight(root));
    obj->setFieldValue(leftField, YGNodeLayoutGetLeft(root));
    obj->setFieldValue(topField, YGNodeLayoutGetTop(root));

    obj->setFieldValue(marginLeftField, YGNodeLayoutGetMargin(root, YGEdgeLeft));
    obj->setFieldValue(marginTopField, YGNodeLayoutGetMargin(root, YGEdgeTop));
    obj->setFieldValue(marginRightField, YGNodeLayoutGetMargin(root, YGEdgeRight));
    obj->setFieldValue(marginBottomField, YGNodeLayoutGetMargin(root, YGEdgeBottom));

    obj->setFieldValue(paddingLeftField, YGNodeLayoutGetPadding(root, YGEdgeLeft));
    obj->setFieldValue(paddingTopField, YGNodeLayoutGetPadding(root, YGEdgeTop));
    obj->setFieldValue(paddingRightField, YGNodeLayoutGetPadding(root, YGEdgeRight));
    obj->setFieldValue(paddingBottomField, YGNodeLayoutGetPadding(root, YGEdgeBottom));

    obj->setFieldValue(borderLeftField, YGNodeLayoutGetBorder(root, YGEdgeLeft));
    obj->setFieldValue(borderTopField, YGNodeLayoutGetBorder(root, YGEdgeTop));
    obj->setFieldValue(borderRightField, YGNodeLayoutGetBorder(root, YGEdgeRight));
    obj->setFieldValue(borderBottomField, YGNodeLayoutGetBorder(root, YGEdgeBottom));

    YGTransferLayoutDirection(root, obj);

    for (uint32_t i = 0; i < YGNodeGetChildCount(root); i++) {
      YGTransferLayoutOutputsRecursive(YGNodeGetChild(root, i));
    }
  } else {
    YGLog(YGLogLevelError, "Java YGNode was GCed during layout calculation\n");
  }
}

static void YGPrint(YGNodeRef node) {
  if (auto obj = YGNodeJobject(node)->lockLocal()) {
    cout << obj->toString() << endl;
  } else {
    YGLog(YGLogLevelError, "Java YGNode was GCed during layout calculation\n");
  }
}

static float YGJNIBaselineFunc(YGNodeRef node, float width, float height) {
  if (auto obj = YGNodeJobject(node)->lockLocal()) {
    static auto baselineFunc = findClassStatic("com/facebook/yoga/YogaNode")
                                   ->getMethod<jfloat(jfloat, jfloat)>("baseline");
    return baselineFunc(obj, width, height);
  } else {
    return height;
  }
}

static YGSize YGJNIMeasureFunc(YGNodeRef node,
                               float width,
                               YGMeasureMode widthMode,
                               float height,
                               YGMeasureMode heightMode) {
  if (auto obj = YGNodeJobject(node)->lockLocal()) {
    static auto measureFunc = findClassStatic("com/facebook/yoga/YogaNode")
                                  ->getMethod<jlong(jfloat, jint, jfloat, jint)>("measure");

    YGTransferLayoutDirection(node, obj);
    const auto measureResult = measureFunc(obj, width, widthMode, height, heightMode);

    static_assert(sizeof(measureResult) == 8,
                  "Expected measureResult to be 8 bytes, or two 32 bit ints");

    int32_t wBits = 0xFFFFFFFF & (measureResult >> 32);
    int32_t hBits = 0xFFFFFFFF & measureResult;

    const float *measuredWidth = reinterpret_cast<float *>(&wBits);
    const float *measuredHeight = reinterpret_cast<float *>(&hBits);

    return YGSize{*measuredWidth, *measuredHeight};
  } else {
    YGLog(YGLogLevelError, "Java YGNode was GCed during layout calculation\n");
    return YGSize{
        widthMode == YGMeasureModeUndefined ? 0 : width,
        heightMode == YGMeasureModeUndefined ? 0 : height,
    };
  }
}

struct JYogaLogLevel : public JavaClass<JYogaLogLevel> {
  static constexpr auto kJavaDescriptor = "Lcom/facebook/yoga/YogaLogLevel;";
};

static global_ref<jobject> *jLogger;
static int YGLog(YGLogLevel level, const char *format, va_list args) {
  char buffer[256];
  int result = vsnprintf(buffer, sizeof(buffer), format, args);

  static auto logFunc = findClassStatic("com/facebook/yoga/YogaLogger")
                            ->getMethod<void(local_ref<JYogaLogLevel>, jstring)>("log");

  static auto logLevelFromInt =
      JYogaLogLevel::javaClassStatic()->getStaticMethod<JYogaLogLevel::javaobject(jint)>("fromInt");

  logFunc(jLogger->get(),
          logLevelFromInt(JYogaLogLevel::javaClassStatic(), static_cast<jint>(level)),
          Environment::current()->NewStringUTF(buffer));

  return result;
}

static inline YGNodeRef _jlong2YGNodeRef(jlong addr) {
  return reinterpret_cast<YGNodeRef>(static_cast<intptr_t>(addr));
}

static inline YGConfigRef _jlong2YGConfigRef(jlong addr) {
  return reinterpret_cast<YGConfigRef>(static_cast<intptr_t>(addr));
}

void jni_YGSetLogger(alias_ref<jclass> clazz, alias_ref<jobject> logger) {
  if (jLogger) {
    jLogger->releaseAlias();
    delete jLogger;
  }

  if (logger) {
    jLogger = new global_ref<jobject>(make_global(logger));
    YGSetLogger(YGLog);
  } else {
    jLogger = NULL;
    YGSetLogger(NULL);
  }
}

void jni_YGLog(alias_ref<jclass> clazz, jint level, jstring message) {
  const char *nMessage = Environment::current()->GetStringUTFChars(message, 0);
  YGLog(static_cast<YGLogLevel>(level), "%s", nMessage);
  Environment::current()->ReleaseStringUTFChars(message, nMessage);
}

jlong jni_YGNodeNew(alias_ref<jobject> thiz) {
  const YGNodeRef node = YGNodeNew();
  YGNodeSetContext(node, new weak_ref<jobject>(make_weak(thiz)));
  YGNodeSetPrintFunc(node, YGPrint);
  return reinterpret_cast<jlong>(node);
}

jlong jni_YGNodeNewWithConfig(alias_ref<jobject> thiz, jlong configPointer) {
  const YGNodeRef node = YGNodeNewWithConfig(_jlong2YGConfigRef(configPointer));
  YGNodeSetContext(node, new weak_ref<jobject>(make_weak(thiz)));
  YGNodeSetPrintFunc(node, YGPrint);
  return reinterpret_cast<jlong>(node);
}

void jni_YGNodeFree(alias_ref<jobject> thiz, jlong nativePointer) {
  const YGNodeRef node = _jlong2YGNodeRef(nativePointer);
  delete YGNodeJobject(node);
  YGNodeFree(node);
}

void jni_YGNodeReset(alias_ref<jobject> thiz, jlong nativePointer) {
  const YGNodeRef node = _jlong2YGNodeRef(nativePointer);
  void *context = YGNodeGetContext(node);
  YGNodeReset(node);
  YGNodeSetContext(node, context);
  YGNodeSetPrintFunc(node, YGPrint);
}

void jni_YGNodeInsertChild(alias_ref<jobject>, jlong nativePointer, jlong childPointer, jint index) {
  YGNodeInsertChild(_jlong2YGNodeRef(nativePointer), _jlong2YGNodeRef(childPointer), index);
}

void jni_YGNodeRemoveChild(alias_ref<jobject>, jlong nativePointer, jlong childPointer) {
  YGNodeRemoveChild(_jlong2YGNodeRef(nativePointer), _jlong2YGNodeRef(childPointer));
}

