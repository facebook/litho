/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.PropDefaultModel;
import com.squareup.javapoet.TypeName;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

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
      final Annotation propDefaultAnnotation = variableElement.getAnnotation(PropDefault.class);
      if (propDefaultAnnotation == null) {
        continue;
      }

      final ResType propDefaultResType = ((PropDefault) propDefaultAnnotation).resType();
      final int propDefaultResId = ((PropDefault) propDefaultAnnotation).resId();

      propDefaults.add(
          new PropDefaultModel(
              TypeName.get(variableElement.asType()),
              variableElement.getSimpleName().toString(),
              ImmutableList.copyOf(new ArrayList<>(variableElement.getModifiers())),
              variableElement,
              propDefaultResType,
              propDefaultResId));
    }

    return ImmutableList.copyOf(propDefaults);
  }
}
