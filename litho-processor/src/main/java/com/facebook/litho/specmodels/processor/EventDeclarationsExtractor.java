/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.specmodels.model.EventDeclarationModel;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

/**
 * Extracts event declarations from the given input.
 */
public class EventDeclarationsExtractor {

  public static ImmutableList<EventDeclarationModel> getEventDeclarations(
      Elements elements,
      TypeElement element,
      Class<?> annotationType) {
    final List<AnnotationValue> eventTypes =
        ProcessorUtils.getAnnotationParameter(elements, element, annotationType, "events");

    final List<EventDeclarationModel> eventDeclarations;
    if (eventTypes != null) {
      eventDeclarations = new ArrayList<>();
      for (AnnotationValue eventType : eventTypes) {
        final DeclaredType type = (DeclaredType) eventType.getValue();
        eventDeclarations.add(
            new EventDeclarationModel(
                ClassName.bestGuess(type.asElement().toString()),
                getReturnType(elements, type.asElement()),
                getFields(type.asElement()),
                type.asElement()));
      }
    } else {
      eventDeclarations = Collections.emptyList();
    }

    return ImmutableList.copyOf(eventDeclarations);
  }

  @Nullable
  static TypeName getReturnType(Elements elements, Element typeElement) {
    TypeMirror typeMirror = ProcessorUtils.getAnnotationParameter(
        elements,
        typeElement,
        Event.class,
        "returnType");

    return typeMirror != null ? TypeName.get(typeMirror) : null;
  }

  static ImmutableList<EventDeclarationModel.FieldModel> getFields(Element element) {
    final List<EventDeclarationModel.FieldModel> fieldModels = new ArrayList<>();
    for (Element enclosedElement : element.getEnclosedElements()) {
      if (enclosedElement.getKind().equals(ElementKind.FIELD)) {
        final Set<Modifier> modifiers = enclosedElement.getModifiers();
        fieldModels.add(
            new EventDeclarationModel.FieldModel(
                FieldSpec.builder(
                    TypeName.get(enclosedElement.asType()),
                    enclosedElement.getSimpleName().toString(),
                    modifiers.toArray(new Modifier[modifiers.size()]))
                    .build(),
                enclosedElement));
      }
    }

    return ImmutableList.copyOf(fieldModels);
  }
}
