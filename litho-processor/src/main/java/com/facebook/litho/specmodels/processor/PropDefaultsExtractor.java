/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import java.util.ArrayList;
import java.util.List;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.specmodels.model.PropDefaultModel;

import com.squareup.javapoet.TypeName;

/**
 * Extracts prop defaults from the given input.
 */
public class PropDefaultsExtractor {

  /**
   * Get the prop defaults from the given {@link TypeElement}.
   */
  public static ImmutableList<PropDefaultModel> getPropDefaults(TypeElement typeElement) {
    final List<PropDefaultModel> propDefaults = new ArrayList<>();

    final List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
    for (Element enclosedElement : enclosedElements) {
      if (enclosedElement.getKind() != ElementKind.FIELD) {
        continue;
      }

      final VariableElement variableElement = (VariableElement) enclosedElement;
      if (variableElement.getAnnotation(PropDefault.class) == null) {
        continue;
      }

      propDefaults.add(
          new PropDefaultModel(
              TypeName.get(variableElement.asType()),
              variableElement.getSimpleName().toString(),
              ImmutableList.copyOf(new ArrayList<>(variableElement.getModifiers()))));
    }

    return ImmutableList.copyOf(propDefaults);
  }
}
