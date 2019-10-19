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

import com.facebook.litho.annotations.FromPreviousCreateLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnEnteredRange;
import com.facebook.litho.annotations.OnExitedRange;
import com.facebook.litho.annotations.OnRegisterRanges;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.DefaultLayoutSpecGenerator;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.SpecGenerator;
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

/** Factory for creating {@link LayoutSpecModel}s. */
public class LayoutSpecModelFactory implements SpecModelFactory<LayoutSpecModel> {
  public static final ImmutableList<Class<? extends Annotation>> DELEGATE_METHOD_ANNOTATIONS;

  static final List<Class<? extends Annotation>> INTER_STAGE_INPUT_ANNOTATIONS = new ArrayList<>();

  static {
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromPreviousCreateLayout.class);

    List<Class<? extends Annotation>> delegateMethodAnnotations = new ArrayList<>();
    delegateMethodAnnotations.addAll(
        DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP.keySet());
    delegateMethodAnnotations.add(OnCreateTreeProp.class);
    delegateMethodAnnotations.add(ShouldUpdate.class);
    delegateMethodAnnotations.add(OnEnteredRange.class);
    delegateMethodAnnotations.add(OnExitedRange.class);
    delegateMethodAnnotations.add(OnRegisterRanges.class);
    delegateMethodAnnotations.add(OnCalculateCachedValue.class);

    DELEGATE_METHOD_ANNOTATIONS = ImmutableList.copyOf(delegateMethodAnnotations);
  }

  private final List<Class<? extends Annotation>> mLayoutSpecDelegateMethodAnnotations;
  private final SpecGenerator<LayoutSpecModel> mLayoutSpecGenerator;

  public LayoutSpecModelFactory() {
    this(DELEGATE_METHOD_ANNOTATIONS, new DefaultLayoutSpecGenerator());
  }

  public LayoutSpecModelFactory(
      List<Class<? extends Annotation>> layoutSpecDelegateMethodAnnotations,
      SpecGenerator<LayoutSpecModel> layoutSpecGenerator) {

    mLayoutSpecDelegateMethodAnnotations = layoutSpecDelegateMethodAnnotations;
    mLayoutSpecGenerator = layoutSpecGenerator;
  }

  @Override
  public Set<Element> extract(RoundEnvironment roundEnvironment) {
    return (Set<Element>) roundEnvironment.getElementsAnnotatedWith(LayoutSpec.class);
  }

  /**
   * Create a {@link LayoutSpecModel} from the given {@link TypeElement} and an optional {@link
   * DependencyInjectionHelper}.
   */
  @Override
  public LayoutSpecModel create(
      Elements elements,
      Types types,
      TypeElement element,
      Messager messager,
      EnumSet<RunMode> runMode,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      @Nullable InterStageStore interStageStore) {

    return new LayoutSpecModel(
        element.getQualifiedName().toString(),
        element.getAnnotation(LayoutSpec.class).value(),
        DelegateMethodExtractor.getDelegateMethods(
            element,
            mLayoutSpecDelegateMethodAnnotations,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.<Class<? extends Annotation>>of(ShouldUpdate.class),
            messager),
        EventMethodExtractor.getOnEventMethods(
            elements, element, INTER_STAGE_INPUT_ANNOTATIONS, messager, runMode),
        TriggerMethodExtractor.getOnTriggerMethods(
            elements, element, INTER_STAGE_INPUT_ANNOTATIONS, messager, runMode),
        WorkingRangesMethodExtractor.getRegisterMethod(
            element, INTER_STAGE_INPUT_ANNOTATIONS, messager),
        WorkingRangesMethodExtractor.getRangesMethods(
            elements, element, INTER_STAGE_INPUT_ANNOTATIONS, messager),
        UpdateStateMethodExtractor.getOnUpdateStateMethods(
            element, INTER_STAGE_INPUT_ANNOTATIONS, messager),
        UpdateStateMethodExtractor.getOnUpdateStateWithTransitionMethods(
            element, INTER_STAGE_INPUT_ANNOTATIONS, messager),
        interStageStore == null
            ? ImmutableList.of()
            : CachedPropNameExtractor.getCachedPropNames(
                interStageStore, element.getQualifiedName()),
        ImmutableList.copyOf(PropDefaultsExtractor.getPropDefaults(element)),
        EventDeclarationsExtractor.getEventDeclarations(
            elements, element, LayoutSpec.class, runMode),
        AnnotationExtractor.extractValidAnnotations(element),
        TagExtractor.extractTagsFromSpecClass(types, element),
        JavadocExtractor.getClassJavadoc(elements, element),
        JavadocExtractor.getPropJavadocs(elements, element),
        element.getAnnotation(LayoutSpec.class).isPublic(),
        dependencyInjectionHelper,
        element.getAnnotation(LayoutSpec.class).isPureRender(),
        SpecElementTypeDeterminator.determine(element),
        element,
        mLayoutSpecGenerator,
        ImmutableList.copyOf(TypeVariablesExtractor.getTypeVariables(element)),
        FieldsExtractor.extractFields(element),
        element.getAnnotation(LayoutSpec.class).simpleNameDelegate());
  }
}
