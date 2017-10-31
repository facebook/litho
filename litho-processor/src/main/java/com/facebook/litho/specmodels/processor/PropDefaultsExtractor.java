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
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
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
      propDefaults.addAll(extractFromField(enclosedElement));
      propDefaults.addAll(extractFromMethod(enclosedElement));
    }

    return ImmutableList.copyOf(propDefaults);
  }

  private static ImmutableList<PropDefaultModel> extractFromField(Element enclosedElement) {
    if (enclosedElement.getKind() != ElementKind.FIELD) {
      return ImmutableList.of();
    }

    final VariableElement variableElement = (VariableElement) enclosedElement;
    final Annotation propDefaultAnnotation = variableElement.getAnnotation(PropDefault.class);
    if (propDefaultAnnotation == null) {
      return ImmutableList.of();
    }

    final ResType propDefaultResType = ((PropDefault) propDefaultAnnotation).resType();
    final int propDefaultResId = ((PropDefault) propDefaultAnnotation).resId();

    return ImmutableList.of(
        new PropDefaultModel(
            TypeName.get(variableElement.asType()),
            variableElement.getSimpleName().toString(),
            ImmutableList.copyOf(new ArrayList<>(variableElement.getModifiers())),
            variableElement,
            propDefaultResType,
            propDefaultResId));
  }

  /**
   * This attempts to extract a prop-default from a <em>method</em>. This is only necessary for
   * Kotlin KAPT generated code, which currently does a rather strange thing where it generates a
   * method <code>void field_name$annotations()</code> for every <code>field_name</code> that has
   * all annotations for said field.
   *
   * <p>So, if we find a method that looks like this, and it contains a <code>PropDefault</code>
   * annotation, we will try to find a matching field of this name and add use it as basis for our
   * prop-default.
   */
  private static ImmutableList<PropDefaultModel> extractFromMethod(Element enclosedElement) {
    if (enclosedElement.getKind() != ElementKind.METHOD) {
      return ImmutableList.of();
    }

    final ExecutableElement methodElement = (ExecutableElement) enclosedElement;

    final Annotation propDefaultAnnotation = methodElement.getAnnotation(PropDefault.class);
    if (propDefaultAnnotation == null) {
      return ImmutableList.of();
    }

    final ResType propDefaultResType = ((PropDefault) propDefaultAnnotation).resType();
    final int propDefaultResId = ((PropDefault) propDefaultAnnotation).resId();

    final String methodName = methodElement.getSimpleName().toString();

    // If it looks like a KAPT and quacks like a KAPT ...
    if (!methodName.endsWith("$annotations")
        || !methodElement.getReturnType().toString().equals("void")) {
      return ImmutableList.of();
    }

    final String baseName = methodName.subSequence(0, methodName.indexOf('$')).toString();
    final Optional<? extends Element> element =
        enclosedElement
            .getEnclosingElement()
            .getEnclosedElements()
            .stream()
            .filter(e -> e.getSimpleName().toString().equals(baseName))
            .findFirst();

    return element
        .map(
            e ->
                ImmutableList.of(
                    new PropDefaultModel(
                        TypeName.get(e.asType()),
                        baseName,
                        ImmutableList.copyOf(new ArrayList<>(methodElement.getModifiers())),
                        methodElement,
                        propDefaultResType,
                        propDefaultResId)))
        .orElseGet(ImmutableList::of);
  }
}
