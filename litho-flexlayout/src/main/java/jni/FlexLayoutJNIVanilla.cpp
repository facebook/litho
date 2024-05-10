// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#include "FlexLayoutJNIVanilla.h"
#include <flexlayout/FlexLayout.h>
#include "ConstFloatArray.h"
#include "FlexLayoutJNIEnums.h"
#include "FlexLayoutJniException.h"
#include "ScopedLocalRef.h"
#include "common.h"
#include "corefunctions.h"

#include <type_traits>

using namespace facebook::flexlayout::jni;
using namespace facebook::flexlayout::layoutoutput;
using namespace facebook::flexlayout::core;

struct JavaMeasureData {
  jobject callbackFunction;
  jint idx;
};

static auto decodeFlexBoxStyle(const ConstFloatArray& arr) -> FlexBoxStyle {
  FlexBoxStyle flexBoxStyle = FlexBoxStyle{};

  for (auto index = 0; index < arr.size();) {
    const auto key = static_cast<FlexBoxStyleKeys>(arr[index++]);
    switch (key) {
      case FlexBoxStyleKeys::PointScaleFactor:
        flexBoxStyle.pointScaleFactor = arr[index++];
        break;
      case FlexBoxStyleKeys::Direction:
        flexBoxStyle.direction = static_cast<Direction>(arr[index++]);
        break;
      case FlexBoxStyleKeys::FlexDirection:
        flexBoxStyle.flexDirection = static_cast<FlexDirection>(arr[index++]);
        break;
      case FlexBoxStyleKeys::JustifyContent:
        flexBoxStyle.justifyContent = static_cast<JustifyContent>(arr[index++]);
        break;
      case FlexBoxStyleKeys::AlignContent:
        flexBoxStyle.alignContent = static_cast<AlignContent>(arr[index++]);
        break;
      case FlexBoxStyleKeys::AlignItems:
        flexBoxStyle.alignItems = static_cast<AlignItems>(arr[index++]);
        break;
      case FlexBoxStyleKeys::FlexWrap:
        flexBoxStyle.flexWrap = static_cast<FlexWrap>(arr[index++]);
        break;
      case FlexBoxStyleKeys::Overflow:
        flexBoxStyle.overflow = static_cast<Overflow>(arr[index++]);
        break;
      case FlexBoxStyleKeys::Padding: {
        const auto edge = static_cast<Edge>(arr[index++]);
        const auto value = arr[index++];
        flexBoxStyle.setPadding(edge, value);
        break;
      }
      case FlexBoxStyleKeys::PaddingPercent: {
        const auto edge = static_cast<Edge>(arr[index++]);
        const auto value = arr[index++];
        flexBoxStyle.setPaddingPercent(edge, value);
        break;
      }
      case FlexBoxStyleKeys::Border: {
        const auto edge = static_cast<Edge>(arr[index++]);
        const auto value = arr[index++];
        flexBoxStyle.setBorder(edge, value);
        break;
      }
      default:
        break;
    }
  }

  return flexBoxStyle;
}

static auto FlexLayoutBaselineFunc(
    const JavaMeasureData& baselineData,
    const float width,
    const float height) -> float {
  JNIEnv* env = getCurrentEnv();

  static const jmethodID methodId = getMethodId(
      env,
      findClass(env, "com/facebook/flexlayout/FlexLayoutNativeMeasureCallback"),
      "baselineNative",
      "(IFF)F");

  return callMethod<jfloat>(
      env,
      baselineData.callbackFunction,
      methodId,
      baselineData.idx,
      width,
      height);
}

static auto FlexLayoutMeasureFunc(
    const JavaMeasureData& measureData,
    const float minWidth,
    const float maxWidth,
    const float minHeight,
    const float maxHeight,
    const float ownerWidth,
    const float ownerHeight) -> MeasureOutput<ScopedLocalRef<jobject>> {
  JNIEnv* env = getCurrentEnv();
  static const jmethodID methodId = getMethodId(
      env,
      findClass(env, "com/facebook/flexlayout/FlexLayoutNativeMeasureCallback"),
      "measureNative",
      "(IFFFFFF)Lcom/facebook/flexlayout/layoutoutput/MeasureOutput;");
  auto const javaMeasureOutput = make_local_ref(
      env,
      callMethod<jobject>(
          env,
          measureData.callbackFunction,
          methodId,
          measureData.idx,
          minWidth,
          maxWidth,
          minHeight,
          maxHeight,
          ownerWidth,
          ownerHeight));

  auto measureOutputClass =
      make_local_ref(env, env->GetObjectClass(javaMeasureOutput.get()));
  static const jfieldID arrField =
      getFieldId(env, measureOutputClass.get(), "arr", "[F");
  auto jary = make_local_ref(
      env, (jfloatArray)env->GetObjectField(javaMeasureOutput.get(), arrField));
  const auto arr = ConstFloatArray{env, std::move(jary)};

  static auto* const measureResultField = getFieldId(
      env, measureOutputClass.get(), "measureResult", "Ljava/lang/Object;");
  auto measureResult = make_local_ref(
      env, env->GetObjectField(javaMeasureOutput.get(), measureResultField));

  auto measureOutput = MeasureOutput<ScopedLocalRef<jobject>>{
      /* .width = */ arr[MEASURE_OUTPUT_WIDTH_POSITION],
      /* .height = */ arr[MEASURE_OUTPUT_HEIGHT_POSITION],
      /* .baseline = */ arr[MEASURE_OUTPUT_BASELINE_POSITION],
      /* .result = */ std::move(measureResult)};

  return measureOutput;
}

static auto decodeFlexItemStyle(const ConstFloatArray& arr)
    -> FlexItemStyle<JavaMeasureData, ScopedLocalRef<jobject>> {
  auto flexItemStyle =
      FlexItemStyle<JavaMeasureData, ScopedLocalRef<jobject>>();
  for (auto index = 0; index < arr.size();) {
    const auto key = static_cast<FlexItemStyleKeys>(arr[index++]);
    switch (key) {
      case FlexItemStyleKeys::Flex:
        flexItemStyle.flex = arr[index++];
        break;
      case FlexItemStyleKeys::FlexGrow:
        flexItemStyle.flexGrow = arr[index++];
        break;
      case FlexItemStyleKeys::FlexShrink:
        flexItemStyle.flexShrink = arr[index++];
        break;
      case FlexItemStyleKeys::FlexBasis:
        flexItemStyle.setFlexBasis(arr[index++]);
        break;
      case FlexItemStyleKeys::FlexBasisPercent:
        flexItemStyle.setFlexBasisPercent(arr[index++]);
        break;
      case FlexItemStyleKeys::FlexBasisAuto:
        flexItemStyle.setFlexBasisAuto();
        break;
      case FlexItemStyleKeys::Width:
        flexItemStyle.setWidth(arr[index++]);
        break;
      case FlexItemStyleKeys::WidthPercent:
        flexItemStyle.setWidthPercent(arr[index++]);
        break;
      case FlexItemStyleKeys::WidthAuto:
        flexItemStyle.setWidthAuto();
        break;
      case FlexItemStyleKeys::MinWidth:
        flexItemStyle.setMinWidth(arr[index++]);
        break;
      case FlexItemStyleKeys::MinWidthPercent:
        flexItemStyle.setMinWidthPercent(arr[index++]);
        break;
      case FlexItemStyleKeys::MaxWidth:
        flexItemStyle.setMaxWidth(arr[index++]);
        break;
      case FlexItemStyleKeys::MaxWidthPercent:
        flexItemStyle.setMaxWidthPercent(arr[index++]);
        break;
      case FlexItemStyleKeys::Height:
        flexItemStyle.setHeight(arr[index++]);
        break;
      case FlexItemStyleKeys::HeightPercent:
        flexItemStyle.setHeightPercent(arr[index++]);
        break;
      case FlexItemStyleKeys::HeightAuto:
        flexItemStyle.setHeightAuto();
        break;
      case FlexItemStyleKeys::MinHeight:
        flexItemStyle.setMinHeight(arr[index++]);
        break;
      case FlexItemStyleKeys::MinHeightPercent:
        flexItemStyle.setMinHeightPercent(arr[index++]);
        break;
      case FlexItemStyleKeys::MaxHeight:
        flexItemStyle.setMaxHeight(arr[index++]);
        break;
      case FlexItemStyleKeys::MaxHeightPercent:
        flexItemStyle.setMaxHeightPercent(arr[index++]);
        break;
      case FlexItemStyleKeys::AlignSelf:
        flexItemStyle.alignSelf = static_cast<AlignSelf>(arr[index++]);
        break;
      case FlexItemStyleKeys::PositionType:
        flexItemStyle.positionType = static_cast<PositionType>(arr[index++]);
        break;
      case FlexItemStyleKeys::AspectRatio:
        flexItemStyle.aspectRatio = arr[index++];
        break;
      case FlexItemStyleKeys::Display:
        flexItemStyle.display = static_cast<Display>(arr[index++]);
        break;
      case FlexItemStyleKeys::Margin: {
        const auto edge = static_cast<Edge>(arr[index++]);
        const auto value = arr[index++];
        flexItemStyle.setMargin(edge, value);
        break;
      }
      case FlexItemStyleKeys::MarginPercent: {
        const auto edge = static_cast<Edge>(arr[index++]);
        const auto value = arr[index++];
        flexItemStyle.setMarginPercent(edge, value);
        break;
      }
      case FlexItemStyleKeys::MarginAuto:
        flexItemStyle.setMarginAuto(static_cast<Edge>(arr[index++]));
        break;
      case FlexItemStyleKeys::Position: {
        const auto edge = static_cast<Edge>(arr[index++]);
        const auto value = arr[index++];
        flexItemStyle.setPosition(edge, value);
        break;
      }
      case FlexItemStyleKeys::PositionPercent: {
        const auto edge = static_cast<Edge>(arr[index++]);
        const auto value = arr[index++];
        flexItemStyle.setPositionPercent(edge, value);
        break;
      }
      case FlexItemStyleKeys::HasBaselineFunction:
        flexItemStyle.baselineFunction = FlexLayoutBaselineFunc;
        break;
      case FlexItemStyleKeys::EnableTextRounding:
        flexItemStyle.enableTextRounding = true;
        break;
      default:
        break;
    }
  }
  return flexItemStyle;
}

enum class LayoutOutputKeys { Width, Height, Baseline };
static constexpr auto NumLayoutOutputKeys = size_t(3);

enum class LayoutOutputChildKeys { Left, Top, Width, Height };
static constexpr auto NumLayoutOutputChildKeys = size_t(4);

template <typename Enum>
static auto rawValue(Enum e) {
  return static_cast<std::underlying_type_t<Enum>>(e);
}

static void TransferLayoutOutputDataToJavaObject(
    const LayoutOutput<ScopedLocalRef<jobject>>& layoutOutput,
    jobject obj) {
  if (!obj) {
    return;
  }
  JNIEnv* env = getCurrentEnv();
  auto objectClass = make_local_ref(env, env->GetObjectClass(obj));
  static const jfieldID arrField =
      getFieldId(env, objectClass.get(), "arr", "[F");
  auto* jary = (jfloatArray)env->GetObjectField(obj, arrField);
  jfloat* arr = env->GetFloatArrayElements(jary, nullptr);
  arr[rawValue(LayoutOutputKeys::Width)] = layoutOutput.width;
  arr[rawValue(LayoutOutputKeys::Height)] = layoutOutput.height;
  arr[rawValue(LayoutOutputKeys::Baseline)] = layoutOutput.baseline;

  static const jfieldID measureResultsField = getFieldId(
      env, objectClass.get(), "measureResults", "[Ljava/lang/Object;");
  auto measureResults = make_local_ref(
      env,
      static_cast<jobjectArray>(env->GetObjectField(obj, measureResultsField)));
  for (size_t i = 0; i < layoutOutput.children.size(); i++) {
    arr[NumLayoutOutputKeys + i * NumLayoutOutputChildKeys +
        rawValue(LayoutOutputChildKeys::Left)] = layoutOutput.children[i].left;
    arr[NumLayoutOutputKeys + i * NumLayoutOutputChildKeys +
        rawValue(LayoutOutputChildKeys::Top)] = layoutOutput.children[i].top;
    arr[NumLayoutOutputKeys + i * NumLayoutOutputChildKeys +
        rawValue(LayoutOutputChildKeys::Width)] =
        layoutOutput.children[i].width;
    arr[NumLayoutOutputKeys + i * NumLayoutOutputChildKeys +
        rawValue(LayoutOutputChildKeys::Height)] =
        layoutOutput.children[i].height;

    env->SetObjectArrayElement(
        measureResults.get(),
        static_cast<jsize>(i),
        layoutOutput.children[i].measureResult.get());
  }
  env->ReleaseFloatArrayElements(jary, arr, 0);
}

static void jni_calculateLayout(
    JNIEnv* env,
    jobject,
    jfloatArray flexBoxStyleArray,
    jobjectArray childrenFlexItemStyleArray,
    jfloat minWidth,
    jfloat maxWidth,
    jfloat minHeight,
    jfloat maxHeight,
    jfloat ownerWidth,
    jfloat,
    jobject layoutOutputJavaObject,
    jobject callbackFunction) {
  try {
    const auto flexBoxStyle = decodeFlexBoxStyle(ConstFloatArray{
        env, make_local_ref_from_unowned(env, flexBoxStyleArray)});

    std::vector<FlexItemStyle<JavaMeasureData, ScopedLocalRef<jobject>>>
        childrenVector;

    int size = env->GetArrayLength(childrenFlexItemStyleArray);

    for (int i = 0; i < size; i++) {
      auto* flexItemStyleArray = (jfloatArray)env->GetObjectArrayElement(
          childrenFlexItemStyleArray, i);
      auto flexItemStyle = decodeFlexItemStyle(
          ConstFloatArray{env, make_local_ref(env, flexItemStyleArray)});
      flexItemStyle.measureData = (JavaMeasureData){callbackFunction, i};
      flexItemStyle.measureFunction = FlexLayoutMeasureFunc;
      childrenVector.push_back(std::move(flexItemStyle));
    }

    auto layoutOutput = calculateLayout(
        flexBoxStyle,
        childrenVector,
        minWidth,
        maxWidth,
        minHeight,
        maxHeight,
        ownerWidth);

    TransferLayoutOutputDataToJavaObject(layoutOutput, layoutOutputJavaObject);
  } catch (const FlexLayoutJniException& jniException) {
    ScopedLocalRef<jthrowable> throwable = jniException.getThrowable();
    if (throwable.get() != nullptr) {
      env->Throw(throwable.get());
    }
  }
}

static JNINativeMethod methods[] = {
    {"jni_calculateLayout",
     "([F[[FFFFFFFLcom/facebook/flexlayout/layoutoutput/LayoutOutput;Lcom/facebook/flexlayout/FlexLayoutNativeMeasureCallback;)V",
     (void*)jni_calculateLayout},
};

void FlexLayoutJNIVanilla::registerNatives(JNIEnv* env) {
  facebook::flexlayout::jni::registerNatives(
      env,
      "com/facebook/flexlayout/FlexLayoutNative",
      methods,
      sizeof(methods) / sizeof(JNINativeMethod));
}
