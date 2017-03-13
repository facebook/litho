// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.processor;

import javax.annotation.Nullable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.facebook.common.internal.ImmutableList;
import com.facebook.components.annotations.FromCreateLayout;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateTreeProp;
import com.facebook.components.annotations.ShouldUpdate;
import com.facebook.components.specmodels.model.DependencyInjectionGenerator;
import com.facebook.components.specmodels.model.LayoutSpecDelegateMethodDescriptions;
import com.facebook.components.specmodels.model.LayoutSpecModel;

/**
 * Factory for creating {@link LayoutSpecModel}s.
 */
public class LayoutSpecModelFactory {
  private static final List<Class<? extends Annotation>> INTER_STAGE_INPUT_ANNOTATIONS =
      new ArrayList<>();
  private static final List<Class<? extends Annotation>> DELEGATE_METHOD_ANNOTATIONS =
      new ArrayList<>();
  static {
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromCreateLayout.class);
    DELEGATE_METHOD_ANNOTATIONS.addAll(
        LayoutSpecDelegateMethodDescriptions.DELEGATE_METHODS_MAP.keySet());
    DELEGATE_METHOD_ANNOTATIONS.add(OnCreateTreeProp.class);
    DELEGATE_METHOD_ANNOTATIONS.add(ShouldUpdate.class);
  }

  /**
   * Create a {@link LayoutSpecModel} from the given {@link TypeElement} and an optional
   * {@link DependencyInjectionGenerator}.
   */
  public static LayoutSpecModel create(
      Elements elements,
      TypeElement element,
      @Nullable DependencyInjectionGenerator dependencyInjectionGenerator) {
    return new LayoutSpecModel(
        element.getQualifiedName().toString(),
        DelegateMethodExtractor.getDelegateMethods(
            element,
            DELEGATE_METHOD_ANNOTATIONS,
            INTER_STAGE_INPUT_ANNOTATIONS),
        EventMethodExtractor.getOnEventMethods(
            elements, element, INTER_STAGE_INPUT_ANNOTATIONS),
        UpdateStateMethodExtractor.getOnUpdateStateMethods(
            element,
            INTER_STAGE_INPUT_ANNOTATIONS),
        ImmutableList.copyOf(TypeVariablesExtractor.getTypeVariables(element)),
        ImmutableList.copyOf(PropDefaultsExtractor.getPropDefaults(element)),
        EventDeclarationsExtractor.getEventDeclarations(elements, element),
        JavadocExtractor.getClassJavadoc(elements, element),
        JavadocExtractor.getPropJavadocs(elements, element),
        dependencyInjectionGenerator,
        element.getAnnotation(LayoutSpec.class).isPureRender(),
        element);
  }
}
