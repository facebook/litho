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

import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for validating that the state models within a  {@link SpecModel} are well-formed.
 */
public class PropValidation {
  // Using these names in props will cause conflicts with the method names for common props in
  // Component.Builder so we trigger a more user-friendly error if the component tries to use them.
  public static final List<String> COMMON_PROP_NAMES =
      Arrays.asList(
          "layoutDirection",
          "key",
          "alignSelf",
          "positionType",
          "flex",
          "flexGrow",
          "flexShrink",
          "flexBasisPx",
          "flexBasisPercent",
          "flexBasisAttr",
          "flexBasisRes",
          "flexBasisDip",
          "importantForAccessibility",
          "duplicateParentState",
          "border",
          "padding",
          "paddingPx",
          "paddingPercent",
          "paddingAttr",
          "paddingRes",
          "paddingDip",
          "margin",
          "marginPx",
          "marginPercent",
          "marginAuto",
          "marginAttr",
          "marginRes",
          "marginDip",
          "position",
          "positionPx",
          "positionPercent",
          "positionAttr",
          "positionRes",
          "positionDip",
          "width",
          "widthPx",
          "widthPercent",
          "widthRes",
          "widthAttr",
          "widthDip",
          "minWidth",
          "minWidthPx",
          "minWidthPercent",
          "minWidthAttr",
          "minWidthRes",
          "minWidthDip",
          "maxWidth",
          "maxWidthPx",
          "maxWidthPercent",
          "maxWidthAttr",
          "maxWidthRes",
          "maxWidthDip",
          "height",
          "heightPx",
          "heightPercent",
          "heightRes",
          "heightAttr",
          "heightDip",
          "minHeight",
          "minHeightPx",
          "minHeightPercent",
          "minHeightAttr",
          "minHeightRes",
          "minHeightDip",
          "maxHeight",
          "maxHeightPx",
          "maxHeightPercent",
          "maxHeightAttr",
          "maxHeightRes",
          "maxHeightDip",
          "touchExpansion",
          "touchExpansionPx",
          "touchExpansionAttr",
          "touchExpansionRes",
          "touchExpansionDip",
          "background",
          "backgroundAttr",
          "backgroundRes",
          "backgroundColor",
          "foreground",
          "foregroundAttr",
          "foregroundRes",
          "foregroundColor",
          "aspectRatio",
          "wrapInView",
          "clickHandler",
          "longClickHandler",
          "focusChangeHandler",
          "touchHandler",
          "interceptTouchHandler",
          "focusable",
          "enabled",
          "visibleHeightRatio",
          "visibleWidthRatio",
          "visibleHandler",
          "focusedHandler",
          "unfocusedHandler",
          "fullImpressionHandler",
          "invisibleHandler",
          "contentDescription",
          "viewTag",
          "viewTags",
          "shadowElevationPx",
          "shadowElevationAttr",
          "shadowElevationRes",
          "shadowElevationDip",
          "outlineProvider",
          "clipToOutline",
          "testKey",
          "accessibilityRole",
          "dispatchPopulateAccessibilityEventHandler",
          "onInitializeAccessibilityEventHandler",
          "onInitializeAccessibilityNodeInfoHandler",
          "onPopulateAccessibilityEventHandler",
          "onRequestSendAccessibilityEventHandler",
          "performAccessibilityActionHandler",
          "sendAccessibilityEventHandler",
          "sendAccessibilityEventUncheckedHandler",
          "transitionKey",
          "alpha",
          "scale",
          "selected");

  // This is a subset of the common props defined above that maybe be used as props that set the
  // isCommonProp param to true.
  public static final List<CommonPropModel> VALID_COMMON_PROPS =
      Arrays.asList(
          new CommonPropModel(
              "positionType", ClassName.bestGuess("com.facebook.yoga.YogaPositionType")),
          new CommonPropModel("widthPx", TypeName.INT),
          new CommonPropModel("heightPx", TypeName.INT),
          new CommonPropModel(
              "background",
              ParameterizedTypeName.get(
                  ClassNames.COMPARABLE_DRAWABLE, WildcardTypeName.subtypeOf(ClassNames.DRAWABLE))),
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

  private static final List<TypeName> ILLEGAL_PROP_TYPES =
      Arrays.<TypeName>asList(ClassNames.COMPONENT_LAYOUT, ClassNames.COMPONENT_BUILDER);

  static List<SpecModelValidationError> validate(
      SpecModel specModel,
      List<String> reservedPropNames,
      List<CommonPropModel> permittedCommonProps) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final ImmutableList<PropModel> props = specModel.getProps();
    for (int i = 0, size = props.size(); i < size - 1; i++) {
      for (int j = i + 1; j < size; j++) {
        if (props.get(i).getName().equals(props.get(j).getName())) {
          validationErrors.add(
              new SpecModelValidationError(
                  props.get(i).getRepresentedObject(),
                  "The prop "
                      + props.get(i).getName()
                      + " is defined differently in different "
                      + "methods. Ensure that each instance of this prop is declared in the same "
                      + "way (this means having the same type, resType and values for isOptional, isCommonProp and overrideCommonPropBehavior)."));
        }
      }
    }

    for (PropModel prop : props) {
      if (!prop.isCommonProp() && prop.overrideCommonPropBehavior()) {
        validationErrors.add(
            new SpecModelValidationError(
                prop.getRepresentedObject(),
                "overrideCommonPropBehavior may only be true is isCommonProp is true."));
      }

      if (reservedPropNames.contains(prop.getName()) && !prop.isCommonProp()) {
        validationErrors.add(
            new SpecModelValidationError(
                prop.getRepresentedObject(),
                "'"
                    + prop.getName()
                    + "' is a reserved prop name used by the component's "
                    + "builder. Please use another name or add \"isCommonProp\" to the "
                    + "Prop's definition."));
      } else if (prop.isCommonProp()) {
        boolean validName = false;
        for (CommonPropModel commonPropModel : permittedCommonProps) {
          if (commonPropModel.name.equals(prop.getName())) {
            validName = true;
            if (!commonPropModel.type.equals(prop.getTypeName())) {
              validationErrors.add(
                  new SpecModelValidationError(
                      prop.getRepresentedObject(),
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
                  prop.getRepresentedObject(),
                  "Prop with isCommonProp and name "
                      + prop.getName()
                      + " is incorrectly defined - see PropValidation.java for a "
                      + "list of common props that may be used."));
        }
      }

      TypeName argumentType = null;
      if (prop.hasVarArgs()) {
        TypeName typeName = prop.getTypeName();
        if (typeName instanceof ParameterizedTypeName) {
          ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) typeName;
          if (!parameterizedTypeName.rawType.equals(ClassNames.LIST)) {
            validationErrors.add(
                new SpecModelValidationError(
                    prop.getRepresentedObject(),
                    prop.getName() + " is a variable argument, and thus should be a List<> type."));
          }
          argumentType = parameterizedTypeName.typeArguments.get(0);
        } else {
          validationErrors.add(
              new SpecModelValidationError(
                  prop.getRepresentedObject(),
                  prop.getName()
                      + " is a variable argument, and thus requires a parameterized List type."));
        }
      } else {
        argumentType = prop.getTypeName();
      }

      if (ILLEGAL_PROP_TYPES.contains(argumentType)) {
        validationErrors.add(
            new SpecModelValidationError(
                prop.getRepresentedObject(),
                "Props may not be declared with the following argument types: "
                    + ILLEGAL_PROP_TYPES
                    + "."));
      }

      if (!prop.isOptional() && prop.hasDefault(specModel.getPropDefaults())) {
        validationErrors.add(
            new SpecModelValidationError(
                prop.getRepresentedObject(),
                prop.getName() + " is not optional so it should not be declared with a default " +
                    "value."));
      }

      if ((prop.getResType() == ResType.DIMEN_SIZE
              || prop.getResType() == ResType.DIMEN_TEXT
              || prop.getResType() == ResType.DIMEN_OFFSET)
          && (MethodParamModelUtils.isAnnotatedWithExternalAnnotation(prop, ClassNames.PX)
              || MethodParamModelUtils.isAnnotatedWithExternalAnnotation(
                  prop, ClassNames.DIMENSION))) {
        validationErrors.add(
            new SpecModelValidationError(
                prop.getRepresentedObject(),
                "Props with resType "
                    + prop.getResType()
                    + " should not be annotated with "
                    + ClassNames.PX
                    + " or "
                    + ClassNames.DIMENSION
                    + ", since these annotations "
                    + "will automatically be added to the relevant builder methods in the "
                    + "generated code."));
      }

      validationErrors.addAll(validateResType(prop));
    }

    for (PropDefaultModel propDefault : specModel.getPropDefaults()) {
      final PropModel prop = SpecModelUtils.getPropWithName(specModel, propDefault.mName);
      if (prop == null) {
        validationErrors.add(
            new SpecModelValidationError(
                propDefault.mRepresentedObject,
                "PropDefault " + propDefault.mName + " of type " + propDefault.mType +
                    " does not correspond to any defined prop"));
      } else if (!(propDefault.mType.box()).equals(prop.getTypeName().box())) {
        validationErrors.add(
            new SpecModelValidationError(
                propDefault.mRepresentedObject,
                "PropDefault " + propDefault.mName + " of type " + propDefault.mType +
                    " should be of type " + prop.getTypeName()));
      }
    }

    return validationErrors;
  }

  private static List<SpecModelValidationError> validateResType(PropModel prop) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final ResType resType = prop.getResType();
    if (resType.equals(ResType.NONE)) {
      return validationErrors;
    }

    final boolean hasVarArgs = prop.hasVarArgs();
    final List<TypeName> validResTypes = new ArrayList<>();
    switch (resType) {
      case STRING:
        validResTypes.add(
            hasVarArgs
                ? ParameterizedTypeName.get(List.class, String.class)
                : TypeName.get(String.class));
        validResTypes.add(
            hasVarArgs
                ? ParameterizedTypeName.get(List.class, CharSequence.class)
                : TypeName.get(CharSequence.class));
        break;
      case STRING_ARRAY:
        validResTypes.add(
            hasVarArgs
                ? ParameterizedTypeName.get(List.class, String[].class)
                : TypeName.get(String[].class));
        break;
      case INT:
      case COLOR:
        if (hasVarArgs) {
          validResTypes.add(ParameterizedTypeName.get(List.class, Integer.class));
        } else {
          validResTypes.add(TypeName.get(int.class));
          validResTypes.add(TypeName.get(Integer.class));
        }
        break;
      case INT_ARRAY:
        validResTypes.add(
            hasVarArgs
                ? ParameterizedTypeName.get(List.class, Integer[].class)
                : TypeName.get(int[].class));
        break;
      case BOOL:
        if (hasVarArgs) {
          validResTypes.add(ParameterizedTypeName.get(List.class, Boolean.class));
        } else {
          validResTypes.add(TypeName.get(boolean.class));
          validResTypes.add(TypeName.get(Boolean.class));
        }
        break;
      case DIMEN_SIZE:
      case DIMEN_TEXT:
      case DIMEN_OFFSET:
        if (hasVarArgs) {
          validResTypes.add(ParameterizedTypeName.get(List.class, Integer.class));
          validResTypes.add(ParameterizedTypeName.get(List.class, Float.class));
        } else {
          validResTypes.add(TypeName.get(int.class));
          validResTypes.add(TypeName.get(Integer.class));
          validResTypes.add(TypeName.get(float.class));
          validResTypes.add(TypeName.get(Float.class));
        }
        break;
      case FLOAT:
        if (hasVarArgs) {
          validResTypes.add(ParameterizedTypeName.get(List.class, Float.class));
        } else {
          validResTypes.add(TypeName.get(float.class));
          validResTypes.add(TypeName.get(Float.class));
        }
        break;
      case DRAWABLE:
        validResTypes.add(
            hasVarArgs
                ? ParameterizedTypeName.get(ClassNames.LIST, ClassNames.DRAWABLE)
                : ClassNames.DRAWABLE);
        break;
      case NONE:
        break;
    }

    if (!validResTypes.contains(prop.getTypeName())) {
      validationErrors.add(
          new SpecModelValidationError(
              prop.getRepresentedObject(),
              (prop.hasVarArgs() ? "A variable argument" : "A prop")
                  + " declared with resType "
                  + prop.getResType()
                  + " must be one of the following types: "
                  + Arrays.toString(validResTypes.toArray())
                  + "."));
    }

    return validationErrors;
  }

  public static class CommonPropModel {
    final String name;
    final TypeName type;

    CommonPropModel(String name, TypeName type) {
      this.name = name;
      this.type = type;
    }
  }
}
