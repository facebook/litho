/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.ResType;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

/**
 * Class for validating that the state models within a  {@link SpecModel} are well-formed.
 */
public class PropValidation {
  // Using these names in props might cause conflicts with the method names in the
  // component's generated layout builder class so we trigger a more user-friendly
  // error if the component tries to use them.
  private static final List<String> RESERVED_PROP_NAMES = Arrays.asList(
      "withLayout",
      "key",
      "loadingEventHandler");

  private static final List<TypeName> ILLEGAL_PROP_TYPES = Arrays.<TypeName>asList(
      ClassNames.COMPONENT_LAYOUT,
      ClassNames.COMPONENT_LAYOUT_BUILDER,
      ClassNames.COMPONENT_LAYOUT_CONTAINER_BUILDER,
      ClassNames.COMPONENT_BUILDER,
      ClassNames.COMPONENT_BUILDER_WITH_LAYOUT,
      ClassNames.REFERENCE_BUILDER);

  static List<SpecModelValidationError> validate(SpecModel specModel) {
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
      if (RESERVED_PROP_NAMES.contains(prop.getName())) {
        validationErrors.add(
            new SpecModelValidationError(
                prop.getRepresentedObject(),
                "'" + prop.getName() + "' is a reserved prop name used by the component's " +
                    "layout builder. Please use another name."));
      }

      if (ILLEGAL_PROP_TYPES.contains(prop.getType())) {
        validationErrors.add(
            new SpecModelValidationError(
                prop.getRepresentedObject(),
                "Props may not be declared with the following types: " + ILLEGAL_PROP_TYPES + "."));
      }

      if (!prop.isOptional() && prop.hasDefault(specModel.getPropDefaults())) {
        validationErrors.add(
            new SpecModelValidationError(
                prop.getRepresentedObject(),
                prop.getName() + " is not optional so it should not be declared with a default " +
                    "value."));
      }

      validationErrors.addAll(validateResType(prop));
    }

    return validationErrors;
  }

  private static List<SpecModelValidationError> validateResType(PropModel prop) {
    List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final ResType resType = prop.getResType();
    if (resType.equals(ResType.NONE)) {
      return validationErrors;
    }

    final List<TypeName> validResTypes = new ArrayList<>();
    switch (resType) {
      case STRING:
        validResTypes.add(TypeName.get(String.class));
        validResTypes.add(TypeName.get(CharSequence.class));
        break;
      case STRING_ARRAY:
        validResTypes.add(TypeName.get(String[].class));
        break;
      case INT:
      case COLOR:
        validResTypes.add(TypeName.get(int.class));
        validResTypes.add(TypeName.get(Integer.class));
        break;
      case INT_ARRAY:
        validResTypes.add(TypeName.get(int[].class));
        break;
      case BOOL:
        validResTypes.add(TypeName.get(boolean.class));
        validResTypes.add(TypeName.get(Boolean.class));
        break;
      case DIMEN_SIZE:
      case DIMEN_TEXT:
      case DIMEN_OFFSET:
        validResTypes.add(TypeName.get(int.class));
        validResTypes.add(TypeName.get(Integer.class));
        validResTypes.add(TypeName.get(float.class));
        validResTypes.add(TypeName.get(Float.class));
        break;
      case FLOAT:
        validResTypes.add(TypeName.get(float.class));
        validResTypes.add(TypeName.get(Float.class));
        break;
      case DRAWABLE:
        validResTypes.add(ParameterizedTypeName.get(ClassNames.REFERENCE, ClassNames.DRAWABLE));
        break;
    }

    if (!validResTypes.contains(prop.getType())) {
      validationErrors.add(
          new SpecModelValidationError(
              prop.getRepresentedObject(),
              "A prop declared with resType " + prop.getResType() + " must be one of the " +
                  "following types: " + Arrays.toString(validResTypes.toArray()) + "."));
    }

    return validationErrors;
  }
}
