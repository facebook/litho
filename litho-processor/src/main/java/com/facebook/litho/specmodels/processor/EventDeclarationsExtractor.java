/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import com.facebook.litho.annotations.Event;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.FieldModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

/** Extracts event declarations from the given input. */
public class EventDeclarationsExtractor {

  public static ImmutableList<EventDeclarationModel> getEventDeclarations(
      Elements elements, TypeElement element, Class<?> annotationType, EnumSet<RunMode> runMode) {
    final List<AnnotationValue> eventTypes =
        ProcessorUtils.getAnnotationParameter(
            elements, element, annotationType, "events", List.class);

    final List<EventDeclarationModel> eventDeclarations;
    if (eventTypes != null) {
      eventDeclarations = new ArrayList<>();
      for (AnnotationValue eventType : eventTypes) {
        final DeclaredType type = (DeclaredType) eventType.getValue();
        final TypeName returnType =
            runMode.contains(RunMode.ABI)
                ? TypeName.VOID
                : getReturnType(elements, type.asElement());
        final ImmutableList<FieldModel> fields =
            runMode.contains(RunMode.ABI)
                ? ImmutableList.of()
                : FieldsExtractor.extractFields(type.asElement());
        eventDeclarations.add(
            new EventDeclarationModel(
                ClassName.bestGuess(type.asElement().toString()),
                returnType,
                fields,
                type.asElement()));
      }
    } else {
      eventDeclarations = Collections.emptyList();
    }

    return ImmutableList.copyOf(eventDeclarations);
  }

  @Nullable
  static TypeName getReturnType(Elements elements, Element typeElement) {
    TypeMirror typeMirror =
        ProcessorUtils.getAnnotationParameter(
            elements, typeElement, Event.class, "returnType", TypeMirror.class);

    return typeMirror != null ? TypeName.get(typeMirror) : TypeName.VOID;
  }
}
