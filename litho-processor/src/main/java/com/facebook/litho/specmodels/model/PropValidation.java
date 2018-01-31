/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
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
          "scale");

  private static final List<TypeName> ILLEGAL_PROP_TYPES = Arrays.<TypeName>asList(
      ClassNames.COMPONENT_LAYOUT,
      ClassNames.COMPONENT_BUILDER,
      ClassNames.REFERENCE_BUILDER);

  static List<SpecModelValidationError> validate(
      SpecModel specModel, List<String> reservedPropNames) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final ImmutableList<PropModel> props = specModel.getProps();
    for (int i = 0, size = props.size(); i < size - 1; i++) {
      for (int j = i + 1; j < size; j++) {
        if (props.get(i).getName().equals(props.get(j).getName())) {
          validationErrors.add(
              new SpecModelValidationError(
                  props.get(i).getRepresentedObject(),
                  "The prop " + props.get(i).getName() + " is defined differently in different " +
                      "methods. Ensure that each instance of this prop is declared in the same " +
                      "way (this means having the same type, resType and value for isOptional)."));
        }
      }
    }

    for (PropModel prop : props) {
      if (reservedPropNames.contains(prop.getName())) {
        validationErrors.add(
            new SpecModelValidationError(
                prop.getRepresentedObject(),
                "'"
                    + prop.getName()
                    + "' is a reserved prop name used by the component's "
                    + "builder. Please use another name."));
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
}
