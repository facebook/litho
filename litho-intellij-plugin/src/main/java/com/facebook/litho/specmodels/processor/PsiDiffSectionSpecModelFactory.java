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

import static com.facebook.litho.sections.specmodels.processor.DiffSectionSpecModelFactory.DELEGATE_METHOD_ANNOTATIONS;

import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.sections.specmodels.model.DefaultDiffSectionSpecGenerator;
import com.facebook.litho.sections.specmodels.model.DiffSectionSpecModel;
import com.facebook.litho.sections.specmodels.model.SectionClassNames;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.BuilderMethodModel;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecElementType;
import com.facebook.litho.specmodels.model.SpecGenerator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class PsiDiffSectionSpecModelFactory {
  private final List<Class<? extends Annotation>> mDiffSectionSpecDelegateMethodAnnotations;
  private final SpecGenerator<DiffSectionSpecModel> mDiffSectionSpecGenerator;
  private static final BuilderMethodModel LOADING_EVENT_BUILDER_METHOD =
      new BuilderMethodModel(
          ParameterizedTypeName.get(
              ClassNames.EVENT_HANDLER.annotated(
                  ImmutableList.of(AnnotationSpec.builder(ClassNames.NULLABLE).build())),
              SectionClassNames.LOADING_EVENT_HANDLER),
          "loadingEventHandler");

  public PsiDiffSectionSpecModelFactory() {
    this(DELEGATE_METHOD_ANNOTATIONS, new DefaultDiffSectionSpecGenerator());
  }

  public PsiDiffSectionSpecModelFactory(
      List<Class<? extends Annotation>> diffSingleSectionSpecDelegateMethodAnnotations,
      SpecGenerator<DiffSectionSpecModel> diffSectionSpecGenerator) {
    mDiffSectionSpecDelegateMethodAnnotations = diffSingleSectionSpecDelegateMethodAnnotations;
    mDiffSectionSpecGenerator = diffSectionSpecGenerator;
  }

  @Nullable
  public DiffSectionSpecModel createWithPsi(
      Project project,
      PsiClass psiClass,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper) {
    final DiffSectionSpec diffSectionSpecAnnotation =
        PsiAnnotationProxyUtils.findAnnotationInHierarchy(psiClass, DiffSectionSpec.class);
    if (diffSectionSpecAnnotation == null) {
      return null;
    }

    final String qualifiedName = psiClass.getQualifiedName();
    if (qualifiedName == null) {
      return null;
    }

    return new DiffSectionSpecModel(
        qualifiedName,
        diffSectionSpecAnnotation.value(),
        PsiDelegateMethodExtractor.getDelegateMethods(
            psiClass,
            mDiffSectionSpecDelegateMethodAnnotations,
            ImmutableList.of(),
            ImmutableList.of(),
            ImmutableList.<Class<? extends Annotation>>of(ShouldUpdate.class, OnDiff.class)),
        PsiEventMethodExtractor.getOnEventMethods(psiClass, ImmutableList.of(), ImmutableList.of()),
        PsiAnnotationExtractor.extractValidAnnotations(project, psiClass),
        PsiTriggerMethodExtractor.getOnTriggerMethods(
            psiClass, ImmutableList.of(), ImmutableList.of()),
        PsiUpdateStateMethodExtractor.getOnUpdateStateMethods(
            psiClass, ImmutableList.of(), ImmutableList.of(), false),
        PsiTypeVariablesExtractor.getTypeVariables(psiClass),
        PsiPropDefaultsExtractor.getPropDefaults(psiClass),
        PsiEventDeclarationsExtractor.getEventDeclarations(psiClass, DiffSectionSpec.class),
        ImmutableList.of(BuilderMethodModel.KEY_BUILDER_METHOD, LOADING_EVENT_BUILDER_METHOD),
        ImmutableList.of(),
        "classJavadoc",
        ImmutableList.of(),
        diffSectionSpecAnnotation.isPublic(),
        SpecElementType.JAVA_CLASS,
        dependencyInjectionHelper,
        psiClass,
        mDiffSectionSpecGenerator,
        PsiFieldsExtractor.extractFields(psiClass));
  }
}
