// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.processor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.facebook.common.internal.ImmutableList;
import com.facebook.components.annotations.FromEvent;
import com.facebook.components.annotations.OnEvent;
import com.facebook.components.annotations.Param;
import com.facebook.components.annotations.Prop;
import com.facebook.components.annotations.State;
import com.facebook.components.annotations.TreeProp;
import com.facebook.components.specmodels.model.EventDeclarationModel;
import com.facebook.components.specmodels.model.EventMethodModel;
import com.facebook.components.specmodels.model.MethodParamModel;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import static com.facebook.components.specmodels.processor.MethodExtractorUtils.getMethodParams;

/**
 * Extracts event methods from the given input.
 */
public class EventMethodExtractor {

  private static final List<Class<? extends Annotation>> METHOD_PARAM_ANNOTATIONS =
      new ArrayList<>();
  static {
    METHOD_PARAM_ANNOTATIONS.add(FromEvent.class);
    METHOD_PARAM_ANNOTATIONS.add(Param.class);
    METHOD_PARAM_ANNOTATIONS.add(Prop.class);
    METHOD_PARAM_ANNOTATIONS.add(State.class);
    METHOD_PARAM_ANNOTATIONS.add(TreeProp.class);
  }

  /**
   * Get the delegate methods from the given {@link TypeElement}.
   */
  public static ImmutableList<EventMethodModel> getOnEventMethods(
      Elements elements,
      TypeElement typeElement,
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<EventMethodModel> delegateMethods = new ArrayList<>();

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
                getPermittedMethodParamAnnotations(permittedInterStageInputAnnotations));

        final DeclaredType eventClassDeclaredType = ProcessorUtils.getAnnotationParameter(
            elements,
            executableElement,
            OnEvent.class,
            "value");
        final Element eventClass = eventClassDeclaredType.asElement();

        final EventMethodModel eventMethod =
            new EventMethodModel(
                new EventDeclarationModel(
                    ClassName.bestGuess(eventClass.toString()),
                    EventDeclarationsExtractor.getReturnType(elements, eventClass),
                    EventDeclarationsExtractor.getFields(eventClass),
                    eventClass),
                ImmutableList.copyOf(new ArrayList<>(executableElement.getModifiers())),
                executableElement.getSimpleName(),
                TypeName.get(executableElement.getReturnType()),
                ImmutableList.copyOf(methodParams),
                executableElement);
        delegateMethods.add(eventMethod);
      }
    }

    return ImmutableList.copyOf(delegateMethods);
  }

  private static List<Class<? extends Annotation>> getPermittedMethodParamAnnotations(
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations) {
    final List<Class<? extends Annotation>> permittedMethodParamAnnotations =
        new ArrayList<>(METHOD_PARAM_ANNOTATIONS);
    permittedMethodParamAnnotations.addAll(permittedInterStageInputAnnotations);

    return permittedMethodParamAnnotations;
  }
}
