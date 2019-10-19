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

import static com.facebook.litho.specmodels.model.SpecModelUtils.generateTypeSpec;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtils.getMethodParams;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtils.getTypeVariables;

import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.InjectProp;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.FieldModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

/** Extracts event methods from the given input. */
public class EventMethodExtractor {

  private static final List<Class<? extends Annotation>> METHOD_PARAM_ANNOTATIONS =
      new ArrayList<>();

  static {
    METHOD_PARAM_ANNOTATIONS.add(FromEvent.class);
    METHOD_PARAM_ANNOTATIONS.add(Param.class);
    METHOD_PARAM_ANNOTATIONS.add(Prop.class);
    METHOD_PARAM_ANNOTATIONS.add(State.class);
    METHOD_PARAM_ANNOTATIONS.add(TreeProp.class);
    METHOD_PARAM_ANNOTATIONS.add(InjectProp.class);
    METHOD_PARAM_ANNOTATIONS.add(CachedValue.class);
  }

  /** Get the delegate methods from the given {@link TypeElement}. */
  public static ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>>
      getOnEventMethods(
          Elements elements,
          TypeElement typeElement,
          List<Class<? extends Annotation>> permittedInterStageInputAnnotations,
          Messager messager,
          EnumSet<RunMode> runMode) {
    final List<SpecMethodModel<EventMethod, EventDeclarationModel>> delegateMethods =
        new ArrayList<>();

    for (Element enclosedElement : typeElement.getEnclosedElements()) {
      if (enclosedElement.getKind() != ElementKind.METHOD) {
        continue;
      }

      final OnEvent onEventAnnotation = enclosedElement.getAnnotation(OnEvent.class);
      if (onEventAnnotation != null) {
        final ExecutableElement executableElement = (ExecutableElement) enclosedElement;

        final List<MethodParamModel> methodParams =
            getMethodParams(
                executableElement,
                messager,
                getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations),
                permittedInterStageInputAnnotations,
                ImmutableList.<Class<? extends Annotation>>of());

        final DeclaredType eventClassDeclaredType =
            ProcessorUtils.getAnnotationParameter(
                elements, executableElement, OnEvent.class, "value", DeclaredType.class);
        final Element eventClass = eventClassDeclaredType.asElement();

        final TypeName returnType =
            runMode.contains(RunMode.ABI)
                ? TypeName.VOID
                : EventDeclarationsExtractor.getReturnType(elements, eventClass);
        final ImmutableList<FieldModel> fields =
            runMode.contains(RunMode.ABI)
                ? ImmutableList.of()
                : FieldsExtractor.extractFields(eventClass);

        final SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod =
            SpecMethodModel.<EventMethod, EventDeclarationModel>builder()
                .annotations(ImmutableList.of())
                .modifiers(ImmutableList.copyOf(new ArrayList<>(executableElement.getModifiers())))
                .name(executableElement.getSimpleName())
                .returnTypeSpec(generateTypeSpec(executableElement.getReturnType()))
                .typeVariables(ImmutableList.copyOf(getTypeVariables(executableElement)))
                .methodParams(ImmutableList.copyOf(methodParams))
                .representedObject(executableElement)
                .typeModel(
                    new EventDeclarationModel(
                        ClassName.bestGuess(eventClass.toString()), returnType, fields, eventClass))
                .build();
        delegateMethods.add(eventMethod);
      }
    }

    return ImmutableList.copyOf(delegateMethods);
  }

  static List<Class<? extends Annotation>> getPermittedMethodParamAnnotations(
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<Class<? extends Annotation>> permittedMethodParamAnnotations =
        new ArrayList<>(METHOD_PARAM_ANNOTATIONS);
    permittedMethodParamAnnotations.addAll(permittedInterStageInputAnnotations);

    return permittedMethodParamAnnotations;
  }
}
