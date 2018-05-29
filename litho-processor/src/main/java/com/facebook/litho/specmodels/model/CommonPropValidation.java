/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.annotations.CommonProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.Modifier;

/** Class for validating that the common props within a {@link SpecModel} are well-formed. */
public class CommonPropValidation {

  public static final List<CommonPropModel> VALID_COMMON_PROPS =
      Arrays.asList(
          new CommonPropModel(
              "positionType", ClassName.bestGuess("com.facebook.yoga.YogaPositionType")),
          new CommonPropModel("widthPx", TypeName.INT),
          new CommonPropModel("heightPx", TypeName.INT),
          new CommonPropModel(
              "background",
              ParameterizedTypeName.get(
                  ClassNames.REFERENCE, WildcardTypeName.subtypeOf(ClassNames.DRAWABLE))),
          new CommonPropModel("testKey", ClassNames.STRING),
          new CommonPropModel(
              "layoutDirection", ClassName.bestGuess("com.facebook.yoga.YogaDirection")),
          new CommonPropModel("alignSelf", ClassName.bestGuess("com.facebook.yoga.YogaAlign")),
          new CommonPropModel("flex", TypeName.FLOAT),
          new CommonPropModel("flexGrow", TypeName.FLOAT),
          new CommonPropModel("flexShrink", TypeName.FLOAT),
          new CommonPropModel("flexBasisPx", TypeName.INT),
          new CommonPropModel("duplicateParentState", TypeName.BOOLEAN),
          new CommonPropModel("importantForAccessibility", TypeName.INT),
          new CommonPropModel("border", ClassName.bestGuess("com.facebook.litho.Border")),
          new CommonPropModel(
              "stateListAnimator", ClassName.bestGuess("android.animation.StateListAnimator")),
          new CommonPropModel("aspectRatio", TypeName.FLOAT),
          new CommonPropModel("foreground", ClassNames.DRAWABLE),
          new CommonPropModel(
              "clickHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.ClickEvent"))),
          new CommonPropModel(
              "longClickHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.LongClickEvent"))),
          new CommonPropModel(
              "focusChangeHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.FocusChangedEvent"))),
          new CommonPropModel(
              "touchHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.TouchEvent"))),
          new CommonPropModel(
              "interceptTouchHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.InterceptTouchEvent"))),
          new CommonPropModel("focusable", TypeName.BOOLEAN),
          new CommonPropModel("enabled", TypeName.BOOLEAN),
          new CommonPropModel("selected", TypeName.BOOLEAN),
          new CommonPropModel("visibleHeightRatio", TypeName.FLOAT),
          new CommonPropModel("visibleWidthRatio", TypeName.FLOAT),
          new CommonPropModel(
              "visibleHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.VisibleEvent"))),
          new CommonPropModel(
              "focusedHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.FocusedVisibleEvent"))),
          new CommonPropModel(
              "unfocusedHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.UnfocusedVisibleEvent"))),
          new CommonPropModel(
              "fullImpressionHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.FullImpressionVisibleEvent"))),
          new CommonPropModel(
              "invisibleHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.InvisibleEvent"))),
          new CommonPropModel("contentDescription", ClassName.bestGuess("java.lang.CharSequence")),
          new CommonPropModel("viewTag", TypeName.OBJECT),
          new CommonPropModel("viewTags", ClassName.bestGuess("android.util.SparseArray")),
          new CommonPropModel("shadowElevationPx", TypeName.FLOAT),
          new CommonPropModel(
              "outlineProvider", ClassName.bestGuess("android.view.ViewOutlineProvider")),
          new CommonPropModel("clipToOutline", TypeName.BOOLEAN),
          new CommonPropModel("accessibilityRole", ClassNames.STRING),
          new CommonPropModel(
              "dispatchPopulateAccessibilityEventHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess(
                      "com.facebook.litho.DispatchPopulateAccessibilityEventEvent"))),
          new CommonPropModel(
              "onInitializeAccessibilityEventHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.OnInitializeAccessibilityEventEvent"))),
          new CommonPropModel(
              "onInitializeAccessibilityNodeInfoHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess(
                      "com.facebook.litho.OnInitializeAccessibilityNodeInfoEvent"))),
          new CommonPropModel(
              "onPopulateAccessibilityEventHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.OnPopulateAccessibilityEventEvent"))),
          new CommonPropModel(
              "onRequestSendAccessibilityEventHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.OnRequestSendAccessibilityEventEvent"))),
          new CommonPropModel(
              "performAccessibilityActionHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.PerformAccessibilityActionEvent"))),
          new CommonPropModel(
              "sendAccessibilityEventHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.SendAccessibilityEventEvent"))),
          new CommonPropModel(
              "sendAccessibilityEventUncheckedHandler",
              ParameterizedTypeName.get(
                  ClassName.bestGuess("com.facebook.litho.EventHandler"),
                  ClassName.bestGuess("com.facebook.litho.SendAccessibilityEventUncheckedEvent"))),
          new CommonPropModel("transitionKey", ClassNames.STRING),
          new CommonPropModel("scale", TypeName.FLOAT),
          new CommonPropModel("alpha", TypeName.FLOAT),
          new CommonPropModel("rotation", TypeName.FLOAT));

  private static final List<Modifier> REQUIRED_COMMON_PROPS_MODIFIERS =
      Arrays.asList(Modifier.PROTECTED, Modifier.STATIC, Modifier.FINAL);

  static List<SpecModelValidationError> validate(
      SpecModel specModel, List<CommonPropModel> permittedCommonProps) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    validationErrors.addAll(validateCommonProps(specModel, permittedCommonProps));
    validationErrors.addAll(validateCommonPropDefaults(specModel));

    return validationErrors;
  }

  static List<SpecModelValidationError> validateCommonProps(
      SpecModel specModel, List<CommonPropModel> permittedCommonProps) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final ImmutableList<SpecMethodModel<DelegateMethod, Void>> delegateMethods =
        specModel.getDelegateMethods();

    for (SpecMethodModel<DelegateMethod, Void> delegateMethod : delegateMethods) {
      for (MethodParamModel methodParamModel : delegateMethod.methodParams) {
        if (MethodParamModelUtils.isAnnotatedWith(methodParamModel, CommonProp.class)) {
          boolean validName = false;
          for (CommonPropModel commonPropModel : permittedCommonProps) {
            if (commonPropModel.name.equals(methodParamModel.getName())) {
              validName = true;
              if (!commonPropModel.type.equals(methodParamModel.getTypeName())) {
                validationErrors.add(
                    new SpecModelValidationError(
                        methodParamModel.getRepresentedObject(),
                        "A common prop with name "
                            + commonPropModel.name
                            + " must have type of: "
                            + commonPropModel.type));
              }
            }
          }

          if (!validName) {
            validationErrors.add(
                new SpecModelValidationError(
                    methodParamModel.getRepresentedObject(),
                    "Common prop with name "
                        + methodParamModel.getName()
                        + " is incorrectly defined - see CommonPropValidation.java for a "
                        + "list of common props that may be used."));
          }
        }
      }
    }

    return validationErrors;
  }

  static List<SpecModelValidationError> validateCommonPropDefaults(SpecModel specModel) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final ImmutableList<CommonPropDefaultModel> commonPropDefaults =
        specModel.getCommonPropDefaults();

    for (CommonPropDefaultModel commonPropDefault : commonPropDefaults) {
      boolean validName = false;
      for (CommonPropModel commonPropModel : VALID_COMMON_PROPS) {
        if (commonPropModel.name.equals(commonPropDefault.mName)) {
          validName = true;
          if (!commonPropModel.type.equals(commonPropDefault.mType)) {
            validationErrors.add(
                new SpecModelValidationError(
                    commonPropDefault.mRepresentedObject,
                    "A common prop default with name "
                        + commonPropModel.name
                        + " must have type of: "
                        + commonPropModel.type));
          }
        }
      }

      if (!validName) {
        validationErrors.add(
            new SpecModelValidationError(
                commonPropDefault.mRepresentedObject,
                "Common prop default with name "
                    + commonPropDefault.mName
                    + " is incorrectly defined - see CommonPropValidation.java for a "
                    + "list of common prop defaults that may be used."));
      }

      if (!(commonPropDefault.mModifiers.containsAll(REQUIRED_COMMON_PROPS_MODIFIERS))) {
        validationErrors.add(
            new SpecModelValidationError(
                commonPropDefault.mRepresentedObject,
                "Common prop defaults must be defined as protected, static and final"));
      }
    }

    return validationErrors;
  }

  private static class CommonPropModel {
    private final String name;
    private final TypeName type;

    private CommonPropModel(String name, TypeName type) {
      this.name = name;
      this.type = type;
    }
  }
}
