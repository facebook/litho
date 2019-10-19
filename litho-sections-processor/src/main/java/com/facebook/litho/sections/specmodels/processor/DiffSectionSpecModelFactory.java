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

package com.facebook.litho.sections.specmodels.processor;

import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.OnBindService;
import com.facebook.litho.sections.annotations.OnCreateService;
import com.facebook.litho.sections.annotations.OnDataBound;
import com.facebook.litho.sections.annotations.OnDataRendered;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.sections.annotations.OnRefresh;
import com.facebook.litho.sections.annotations.OnUnbindService;
import com.facebook.litho.sections.annotations.OnViewportChanged;
import com.facebook.litho.sections.specmodels.model.DefaultDiffSectionSpecGenerator;
import com.facebook.litho.sections.specmodels.model.DiffSectionSpecModel;
import com.facebook.litho.sections.specmodels.model.SectionClassNames;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.BuilderMethodModel;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecGenerator;
import com.facebook.litho.specmodels.processor.AnnotationExtractor;
import com.facebook.litho.specmodels.processor.DelegateMethodExtractor;
import com.facebook.litho.specmodels.processor.EventDeclarationsExtractor;
import com.facebook.litho.specmodels.processor.EventMethodExtractor;
import com.facebook.litho.specmodels.processor.FieldsExtractor;
import com.facebook.litho.specmodels.processor.InterStageStore;
import com.facebook.litho.specmodels.processor.JavadocExtractor;
import com.facebook.litho.specmodels.processor.PropDefaultsExtractor;
import com.facebook.litho.specmodels.processor.SpecElementTypeDeterminator;
import com.facebook.litho.specmodels.processor.SpecModelFactory;
import com.facebook.litho.specmodels.processor.TagExtractor;
import com.facebook.litho.specmodels.processor.TriggerMethodExtractor;
import com.facebook.litho.specmodels.processor.TypeVariablesExtractor;
import com.facebook.litho.specmodels.processor.UpdateStateMethodExtractor;
import com.squareup.javapoet.ParameterizedTypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/** Factory for creating {@link DiffSectionSpecModel}s. */
public class DiffSectionSpecModelFactory implements SpecModelFactory<DiffSectionSpecModel> {

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
    DELEGATE_METHOD_ANNOTATIONS.add(OnDiff.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnCreateService.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnBindService.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnUnbindService.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnDataBound.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnDataRendered.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnRefresh.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnViewportChanged.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnCreateTreeProp.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnCalculateCachedValue.class);
  }

  private final SpecGenerator<DiffSectionSpecModel> mSpecGenerator;

  public DiffSectionSpecModelFactory() {
    this(new DefaultDiffSectionSpecGenerator());
  }

  public DiffSectionSpecModelFactory(SpecGenerator<DiffSectionSpecModel> specGenerator) {
    mSpecGenerator = specGenerator;
  }

  @Override
  public Set<Element> extract(RoundEnvironment roundEnvironment) {
    return (Set<Element>) roundEnvironment.getElementsAnnotatedWith(DiffSectionSpec.class);
  }

  @Override
  public DiffSectionSpecModel create(
      Elements elements,
      Types types,
      TypeElement element,
      Messager messager,
      EnumSet<RunMode> runMode,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      @Nullable InterStageStore interStageStore) {
    return createModel(elements, types, element, messager, dependencyInjectionHelper, runMode);
  }

  public DiffSectionSpecModel createModel(
      Elements elements,
      Types types,
      TypeElement element,
      Messager messager,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      EnumSet<RunMode> runMode) {
    return new DiffSectionSpecModel(
        element.getQualifiedName().toString(),
        element.getAnnotation(DiffSectionSpec.class).value(),
        DelegateMethodExtractor.getDelegateMethods(
            element,
            DELEGATE_METHOD_ANNOTATIONS,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.<Class<? extends Annotation>>of(ShouldUpdate.class, OnDiff.class),
            messager),
        EventMethodExtractor.getOnEventMethods(
            elements, element, INTER_STAGE_INPUT_ANNOTATIONS, messager, runMode),
        AnnotationExtractor.extractValidAnnotations(element),
        TriggerMethodExtractor.getOnTriggerMethods(
            elements, element, INTER_STAGE_INPUT_ANNOTATIONS, messager, runMode),
        UpdateStateMethodExtractor.getOnUpdateStateMethods(
            element, INTER_STAGE_INPUT_ANNOTATIONS, messager),
        ImmutableList.copyOf(TypeVariablesExtractor.getTypeVariables(element)),
        ImmutableList.copyOf(PropDefaultsExtractor.getPropDefaults(element)),
        EventDeclarationsExtractor.getEventDeclarations(
            elements, element, DiffSectionSpec.class, runMode),
        ImmutableList.of(BuilderMethodModel.KEY_BUILDER_METHOD, LOADING_EVENT_BUILDER_METHOD),
        TagExtractor.extractTagsFromSpecClass(types, element),
        JavadocExtractor.getClassJavadoc(elements, element),
        JavadocExtractor.getPropJavadocs(elements, element),
        element.getAnnotation(DiffSectionSpec.class).isPublic(),
        SpecElementTypeDeterminator.determine(element),
        dependencyInjectionHelper,
        element,
        mSpecGenerator,
        FieldsExtractor.extractFields(element));
  }
}
