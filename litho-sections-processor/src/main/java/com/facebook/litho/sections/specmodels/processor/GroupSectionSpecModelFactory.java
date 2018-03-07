/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.specmodels.processor;

import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnBindService;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.annotations.OnCreateService;
import com.facebook.litho.sections.annotations.OnDataBound;
import com.facebook.litho.sections.annotations.OnRefresh;
import com.facebook.litho.sections.annotations.OnUnbindService;
import com.facebook.litho.sections.annotations.OnViewportChanged;
import com.facebook.litho.sections.specmodels.model.GroupSectionSpecModel;
import com.facebook.litho.sections.specmodels.model.SectionClassNames;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.BuilderMethodModel;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.AnnotationExtractor;
import com.facebook.litho.specmodels.processor.DelegateMethodExtractor;
import com.facebook.litho.specmodels.processor.EventDeclarationsExtractor;
import com.facebook.litho.specmodels.processor.EventMethodExtractor;
import com.facebook.litho.specmodels.processor.InterStageStore;
import com.facebook.litho.specmodels.processor.JavadocExtractor;
import com.facebook.litho.specmodels.processor.PropDefaultsExtractor;
import com.facebook.litho.specmodels.processor.SpecElementTypeDeterminator;
import com.facebook.litho.specmodels.processor.SpecModelFactory;
import com.facebook.litho.specmodels.processor.TriggerMethodExtractor;
import com.facebook.litho.specmodels.processor.TypeVariablesExtractor;
import com.facebook.litho.specmodels.processor.UpdateStateMethodExtractor;
import com.squareup.javapoet.ParameterizedTypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/** Factory for creating {@link GroupSectionSpecModel}s. */
public class GroupSectionSpecModelFactory implements SpecModelFactory {

  private static final List<Class<? extends Annotation>> INTER_STAGE_INPUT_ANNOTATIONS =
      new ArrayList<>();
  private static final List<Class<? extends Annotation>> DELEGATE_METHOD_ANNOTATIONS =
      new ArrayList<>();
  private static final BuilderMethodModel LOADING_EVENT_BUILDER_METHOD =
      new BuilderMethodModel(
          ParameterizedTypeName.get(
              ClassNames.EVENT_HANDLER, SectionClassNames.LOADING_EVENT_HANDLER),
          "loadingEventHandler");

  static {
    DELEGATE_METHOD_ANNOTATIONS.add(ShouldUpdate.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnCreateInitialState.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnCreateChildren.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnCreateService.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnBindService.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnUnbindService.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnDataBound.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnRefresh.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnViewportChanged.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnCreateTreeProp.class);
  }

  @Override
  public Set<Element> extract(RoundEnvironment roundEnvironment) {
    return (Set<Element>) roundEnvironment.getElementsAnnotatedWith(GroupSectionSpec.class);
  }

  @Override
  public SpecModel create(
      Elements elements,
      Types types,
      TypeElement element,
      Messager messager,
      RunMode runMode,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      @Nullable InterStageStore interStageStore) {
    return createModel(elements, element, messager, dependencyInjectionHelper, runMode);
  }

  public static GroupSectionSpecModel createModel(
      Elements elements,
      TypeElement element,
      Messager messager,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      RunMode runMode) {
    return new GroupSectionSpecModel(
        element.getQualifiedName().toString(),
        element.getAnnotation(GroupSectionSpec.class).value(),
        DelegateMethodExtractor.getDelegateMethods(
            element,
            DELEGATE_METHOD_ANNOTATIONS,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.<Class<? extends Annotation>>of(ShouldUpdate.class),
            messager),
        EventMethodExtractor.getOnEventMethods(
            elements, element, INTER_STAGE_INPUT_ANNOTATIONS, messager, runMode),
        TriggerMethodExtractor.getOnTriggerMethods(
            elements, element, INTER_STAGE_INPUT_ANNOTATIONS, messager, runMode),
        UpdateStateMethodExtractor.getOnUpdateStateMethods(
            element, INTER_STAGE_INPUT_ANNOTATIONS, messager),
        ImmutableList.copyOf(TypeVariablesExtractor.getTypeVariables(element)),
        ImmutableList.copyOf(PropDefaultsExtractor.getPropDefaults(element)),
        EventDeclarationsExtractor.getEventDeclarations(
            elements, element, GroupSectionSpec.class, runMode),
        AnnotationExtractor.extractValidAnnotations(element),
        ImmutableList.of(BuilderMethodModel.KEY_BUILDER_METHOD, LOADING_EVENT_BUILDER_METHOD),
        JavadocExtractor.getClassJavadoc(elements, element),
        JavadocExtractor.getPropJavadocs(elements, element),
        element.getAnnotation(GroupSectionSpec.class).isPublic(),
        SpecElementTypeDeterminator.determine(element),
        dependencyInjectionHelper,
        element);
  }
}
