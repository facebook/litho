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
import javax.lang.model.type.TypeKind;

/** Extracts prop defaults from the given input. */
public class PropDefaultsExtractor {

  /** Get the prop defaults from the given {@link TypeElement}. */
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

    final String methodName = methodElement.getSimpleName().toString();

    boolean isPropDefaultWithoutGet =
        methodName.endsWith("$annotations")
            && methodElement.getReturnType().getKind() == TypeKind.VOID;

    final String baseName;

    /*
     * In case an [@PropDefault] annotated variable does not include `get` on the Kotlin
     * annotation, we fallback to the previous method of identifying `PropDefault` values.
     * Note here that this method is deprecated and might be removed from KAPT some time in
     * future.
     *
     * If a user annotates that variable with `@get:PropDefault` we identify the
     * `PropDefault` values through the accompanying `get` method of that variable.
     * */
    if (isPropDefaultWithoutGet) {
      baseName = methodName.subSequence(0, methodName.indexOf('$')).toString();
    } else {
      baseName =
          methodName.replaceFirst("get", "").substring(0, 1).toLowerCase()
              + methodName.replaceFirst("get", "").substring(1);
    }

    final Optional<? extends Element> element =
        enclosedElement.getEnclosingElement().getEnclosedElements().stream()
            .filter(e -> e.getSimpleName().toString().equals(baseName))
            .findFirst();

    final ResType propDefaultResType = ((PropDefault) propDefaultAnnotation).resType();
    final int propDefaultResId = ((PropDefault) propDefaultAnnotation).resId();

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
