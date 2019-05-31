/*
 * Copyright 2017-present Facebook, Inc.
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

import static com.facebook.litho.specmodels.processor.LayoutSpecModelFactory.DELEGATE_METHOD_ANNOTATIONS;
import static com.facebook.litho.specmodels.processor.LayoutSpecModelFactory.INTER_STAGE_INPUT_ANNOTATIONS;

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DefaultLayoutSpecGenerator;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.facebook.litho.specmodels.model.PropJavadocModel;
import com.facebook.litho.specmodels.model.SpecElementType;
import com.facebook.litho.specmodels.model.SpecGenerator;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.annotation.Nullable;

/** Factory for creating {@link LayoutSpecModel}s. */
public class PsiLayoutSpecModelFactory {
  private final List<Class<? extends Annotation>> mLayoutSpecDelegateMethodAnnotations;
  private final SpecGenerator<LayoutSpecModel> mLayoutSpecGenerator;

  public PsiLayoutSpecModelFactory() {
    this(DELEGATE_METHOD_ANNOTATIONS, new DefaultLayoutSpecGenerator());
  }

  public PsiLayoutSpecModelFactory(
      List<Class<? extends Annotation>> layoutSpecDelegateMethodAnnotations,
      SpecGenerator<LayoutSpecModel> layoutSpecGenerator) {

    mLayoutSpecDelegateMethodAnnotations = layoutSpecDelegateMethodAnnotations;
    mLayoutSpecGenerator = layoutSpecGenerator;
  }

  @Nullable
  public LayoutSpecModel createWithPsi(
      Project project,
      PsiClass psiClass,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper) {
    LayoutSpec layoutSpecAnnotation =
        PsiAnnotationProxyUtils.findAnnotationInHierarchy(psiClass, LayoutSpec.class);
    if (layoutSpecAnnotation == null) {
      return null;
    }

    // #5 trigger methods
    ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>> triggerMethods =
        PsiTriggerMethodExtractor.getOnTriggerMethods(psiClass, INTER_STAGE_INPUT_ANNOTATIONS);

    // #12 classJavadoc
    String classJavadoc = "classJavadoc";

    // #13 JavadocExtractor.getPropJavadocs()
    ImmutableList<PropJavadocModel> propJavadocs = ImmutableList.of();

    return new LayoutSpecModel(
        psiClass.getQualifiedName(),
        layoutSpecAnnotation.value(),
        PsiDelegateMethodExtractor.getDelegateMethods(
            psiClass,
            mLayoutSpecDelegateMethodAnnotations,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.<Class<? extends Annotation>>of(ShouldUpdate.class)),
        PsiEventMethodExtractor.getOnEventMethods(project, psiClass, INTER_STAGE_INPUT_ANNOTATIONS),
        triggerMethods,
        PsiWorkingRangesMethodExtractor.getRegisterMethod(psiClass, INTER_STAGE_INPUT_ANNOTATIONS),
        PsiWorkingRangesMethodExtractor.getRangesMethods(psiClass, INTER_STAGE_INPUT_ANNOTATIONS),
        PsiUpdateStateMethodExtractor.getOnUpdateStateMethods(
            psiClass, INTER_STAGE_INPUT_ANNOTATIONS, false),
        PsiUpdateStateMethodExtractor.getOnUpdateStateMethods(
            psiClass, INTER_STAGE_INPUT_ANNOTATIONS, true),
        ImmutableList.<String>of(),
        PsiPropDefaultsExtractor.getPropDefaults(psiClass),
        PsiEventDeclarationsExtractor.getEventDeclarations(project, psiClass),
        PsiAnnotationExtractor.extractValidAnnotations(project, psiClass),
        null,
        classJavadoc,
        propJavadocs,
        layoutSpecAnnotation.isPublic(),
        dependencyInjectionHelper,
        layoutSpecAnnotation.isPureRender(),
        SpecElementType.JAVA_CLASS,
        psiClass,
        mLayoutSpecGenerator,
        PsiTypeVariablesExtractor.getTypeVariables(psiClass),
        PsiFieldsExtractor.extractFields(psiClass),
        null);
  }
}
