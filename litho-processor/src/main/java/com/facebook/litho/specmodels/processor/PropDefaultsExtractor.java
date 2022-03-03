/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.base.Preconditions;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
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
      propDefaults.addAll(extractFromMethod(enclosedElement, enclosedElements));
      propDefaults.addAll(extractFromCompanionClass(enclosedElement, enclosedElements));
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
            propDefaultResId,
            PropDefaultModel.AccessorType.FIELD));
  }

  /**
   * This attempts to extract a prop-default from a <em>method</em>. This is only necessary for
   * Kotlin KAPT generated code, which currently does 2 things. It either 1) generates a method
   * <code>void getField_name$annotations()</code> for every <code>field_name</code> that has all
   * annotations for said field or 2) generates a getter method <code> type getField_name()</code>
   * that has all the annotations and returns the field.
   *
   * <p>So, if we find a method that matches one of those formats and contains a <code>PropDefault
   * </code> annotation, we will try to find a matching field or getter method that provides the
   * prop default.
   */
  private static ImmutableList<PropDefaultModel> extractFromMethod(
      Element enclosedElement, List<? extends Element> elementsToSearch) {
    if (enclosedElement.getKind() != ElementKind.METHOD) {
      return ImmutableList.of();
    }

    final ExecutableElement methodElement = (ExecutableElement) enclosedElement;

    final Annotation propDefaultAnnotation = methodElement.getAnnotation(PropDefault.class);
    if (propDefaultAnnotation == null) {
      return ImmutableList.of();
    }

    final String methodName = methodElement.getSimpleName().toString();

    final KotlinAccessorExtractionType kotlinAccessorType =
        getKotlinAccessorExtractionType(methodElement, elementsToSearch);

    final String baseName;
    final PropDefaultModel.AccessorType accessorType;

    switch (kotlinAccessorType) {
      case UNANNOTATED_GETTER_METHOD:
        /**
         * The PropDefault was defined via the form: <code>@PropDefault const val ...</code> or
         * <code>
         * @PropDefault @JvmField val ...</code>
         *
         * <p>In case an [@PropDefault] annotated variable does not include `get` on the Kotlin
         * annotation, we fallback to the previous method of identifying `PropDefault` values. Note
         * here that this method is deprecated and might be removed from KAPT some time in future.
         *
         * <p>The method name is akin to: getSomePropDefault$annotations(). Therefore, we want to
         * find the field: somePropDefault.
         */
        baseName = getBaseNameFrom$AnnotationsMethod(methodName);
        accessorType = PropDefaultModel.AccessorType.GETTER_METHOD;
        break;
      case UNANNOTATED_FIELD:
        /**
         * The PropDefault was defined via the form: <code>@PropDefault const val ...</code> or
         * <code>@PropDefault @JvmField val ...</code>
         *
         * <p>In case an [@PropDefault] annotated variable does not include `get` on the Kotlin
         * annotation, we fallback to the previous method of identifying `PropDefault` values. Note
         * here that this method is deprecated and might be removed from KAPT some time in future.
         *
         * <p>The method name is akin to: getSomePropDefault$annotations(). Therefore, we want to
         * find the field: somePropDefault.
         */
        baseName = getBaseNameFrom$AnnotationsMethod(methodName);
        accessorType = PropDefaultModel.AccessorType.FIELD;
        break;
      case ANNOTATED_GETTER_METHOD:
      default:
        /**
         * The method name is akin to: getSomePropDefault(). Therefore, we want to find the field:
         * somePropDefault.
         */
        baseName =
            methodName.replaceFirst("get", "").substring(0, 1).toLowerCase()
                + methodName.replaceFirst("get", "").substring(1);
        accessorType = PropDefaultModel.AccessorType.GETTER_METHOD;
        break;
    }

    final Optional<? extends Element> element =
        elementsToSearch.stream()
            .filter(e -> e.getSimpleName().toString().equals(baseName))
            .findFirst();

    final ResType propDefaultResType = ((PropDefault) propDefaultAnnotation).resType();
    final int propDefaultResId = ((PropDefault) propDefaultAnnotation).resId();

    return element
        .map(
            e -> {
              /**
               * If the PropDefault does not have a getter method, we should access it via the field
               * element. Otherwise, we should access it via the getter method.
               */
              final Element propDefaultAccessorElement;
              switch (kotlinAccessorType) {
                case UNANNOTATED_FIELD:
                  propDefaultAccessorElement = e;
                  break;
                case UNANNOTATED_GETTER_METHOD:
                  propDefaultAccessorElement =
                      Preconditions.checkNotNull(
                          getUnannotatedGetterMethodElement(methodName, elementsToSearch));
                  break;
                case ANNOTATED_GETTER_METHOD:
                default:
                  propDefaultAccessorElement = methodElement;
                  break;
              }

              return ImmutableList.of(
                  new PropDefaultModel(
                      TypeName.get(e.asType()),
                      baseName,
                      ImmutableList.copyOf(
                          new ArrayList<>(propDefaultAccessorElement.getModifiers())),
                      propDefaultAccessorElement,
                      propDefaultResType,
                      propDefaultResId,
                      accessorType));
            })
        .orElseGet(ImmutableList::of);
  }

  private static Collection<? extends PropDefaultModel> extractFromCompanionClass(
      Element element, final List<? extends Element> specLevelElements) {
    if (element.getKind() != ElementKind.CLASS
        || !element.getModifiers().contains(Modifier.PUBLIC)
        || !element.getModifiers().contains(Modifier.STATIC)
        || !element.getModifiers().contains(Modifier.FINAL)
        || !element.getSimpleName().toString().equals("Companion")) {
      return ImmutableList.of();
    }

    List<PropDefaultModel> models = new ArrayList<>();

    final List<? extends Element> enclosedCompanionElements = element.getEnclosedElements();
    /**
     * It's possible that we need to extract a method/field that's either: 1. Within the
     * over-arching spec class (specLevelElements) 2. Within the Companion class
     * (enclosedCompanionElements)
     *
     * <p>Therefore, we want to combine the 2 when providing the elements to search through in the
     * extraction method.
     */
    final List<? extends Element> allElementsToMatchAgainst =
        Stream.of(enclosedCompanionElements, specLevelElements)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    for (Element enclosedElement : enclosedCompanionElements) {
      models.addAll(extractFromMethod(enclosedElement, allElementsToMatchAgainst));
    }

    return ImmutableList.copyOf(models);
  }

  private static KotlinAccessorExtractionType getKotlinAccessorExtractionType(
      final ExecutableElement methodElement, final List<? extends Element> elementsToSearch) {
    final String methodName = methodElement.getSimpleName().toString();

    /**
     * If we have a method such as: <code>
     *  @PropDefault
     *  public static void getSomePropDefault$annotations()
     * </code> we know that the actual accessor (or element that returns the PropDefault) is some
     * other element that is not annotated with PropDefault.
     *
     * <p>Otherwise, we already found the method that returns the PropDefault and has the
     * annotation.
     */
    final boolean isPropDefaultWithoutAnnotatedGetter =
        methodName.endsWith("$annotations")
            && methodElement.getReturnType().getKind() == TypeKind.VOID;
    if (!isPropDefaultWithoutAnnotatedGetter) {
      return KotlinAccessorExtractionType.ANNOTATED_GETTER_METHOD;
    }

    /**
     * Attempt to find if there is a getter method with the proper name that just lacks
     * the @PropDefault annotation.
     */
    @Nullable
    final Element propDefaultFoundMethod =
        getUnannotatedGetterMethodElement(methodName, elementsToSearch);

    return propDefaultFoundMethod != null
        ? KotlinAccessorExtractionType.UNANNOTATED_GETTER_METHOD
        : KotlinAccessorExtractionType.UNANNOTATED_FIELD;
  }

  /**
   * The method name is akin to: getSomePropDefault$annotations(). Therefore, we want return the
   * base name somePropDefault.
   *
   * @param methodName a method name akin to getSomePropDefault$annotations()
   * @return the base name for the prop, such as somePropDefault
   */
  private static String getBaseNameFrom$AnnotationsMethod(final String methodName) {
    final String strippedConstantName =
        methodName.substring(0, methodName.indexOf('$')).replaceFirst("get", "");
    return strippedConstantName.substring(0, 1).toLowerCase() + strippedConstantName.substring(1);
  }

  /**
   * The expectation is this method is used to extract an unannotated getter method (but it would
   * also return an annotated getter method, should one exist).
   *
   * @return a getter method for the PropDefault if one exists, otherwise null.
   */
  @Nullable
  private static Element getUnannotatedGetterMethodElement(
      final String methodName, final List<? extends Element> elementsToSearch) {
    return elementsToSearch.stream()
        .filter(
            e ->
                e instanceof ExecutableElement
                    && e.getSimpleName()
                        .toString()
                        .equals(methodName.substring(0, methodName.indexOf('$'))))
        .limit(1)
        .reduce(null, (first, second) -> second);
  }

  /**
   * Determines the type of accessors we want to extract when extracting from a Kotlin method
   * annotated with PropDefault. For Kotlin methods annotated with PropDefault, these are not always
   * the accessors for getting the PropDefault value.
   */
  private enum KotlinAccessorExtractionType {
    /**
     * The accessor is a getter method which is annotated with PropDefault.
     *
     * <p><code>
     * @PropDefault
     * public static String getSomePropDefault() { return someProp; }
     * </code>
     */
    ANNOTATED_GETTER_METHOD,
    /**
     * The accessor is a getter method which is not annotated with PropDefault.
     *
     * <p><code>
     * // accessor
     * public static String getSomePropDefault() { return someProp; }
     *
     * // annotated element
     * @PropDefault
     * public static void getSomePropDefault$annotations() {}
     * </code>
     */
    UNANNOTATED_GETTER_METHOD,
    /**
     * The accessor is a field (which is not annotated with PropDefault).
     *
     * <p><code>
     * // accessor
     * public static final String someProp = ""
     *
     * // annotated element
     * @PropDefault
     * public static void getSomePropDefault$annotations() {}
     * </code>
     */
    UNANNOTATED_FIELD;
  }
}
