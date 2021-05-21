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

import static com.facebook.litho.sections.specmodels.processor.GroupSectionSpecModelFactory.DELEGATE_METHOD_ANNOTATIONS;
import static com.facebook.litho.sections.specmodels.processor.GroupSectionSpecModelFactory.INTER_STAGE_INPUT_ANNOTATIONS;

import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.specmodels.model.DefaultGroupSectionSpecGenerator;
import com.facebook.litho.sections.specmodels.model.GroupSectionSpecModel;
import com.facebook.litho.sections.specmodels.model.SectionClassNames;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.BuilderMethodModel;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecElementType;
import com.facebook.litho.specmodels.model.SpecGenerator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.squareup.javapoet.ParameterizedTypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/** Factory for creating {@link GroupSectionSpecModel}s. */
public class PsiGroupSectionSpecModelFactory {
  private final List<Class<? extends Annotation>> mGroupSectionSpecDelegateMethodAnnotations;
  private final SpecGenerator<GroupSectionSpecModel> mGroupSectionSpecGenerator;
  private static final BuilderMethodModel LOADING_EVENT_BUILDER_METHOD =
      new BuilderMethodModel(
          ParameterizedTypeName.get(
              ClassNames.EVENT_HANDLER, SectionClassNames.LOADING_EVENT_HANDLER),
          "loadingEventHandler");

  public PsiGroupSectionSpecModelFactory() {
    this(DELEGATE_METHOD_ANNOTATIONS, new DefaultGroupSectionSpecGenerator());
  }

  public PsiGroupSectionSpecModelFactory(
      List<Class<? extends Annotation>> groupSectionSpecDelegateMethodAnnotations,
      SpecGenerator<GroupSectionSpecModel> layoutSpecGenerator) {
    mGroupSectionSpecDelegateMethodAnnotations = groupSectionSpecDelegateMethodAnnotations;
    mGroupSectionSpecGenerator = layoutSpecGenerator;
  }

  /**
   * @return a new {@link GroupSectionSpecModel} or null if provided class isn't a {@link
   *     GroupSectionSpec} class. Access is allowed from event dispatch thread or inside read-action
   *     only.
   */
  @Nullable
  public GroupSectionSpecModel createWithPsi(
      Project project,
      PsiClass psiClass,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper) {
    GroupSectionSpec groupSectionSpecAnnotation =
        PsiAnnotationProxyUtils.findAnnotationInHierarchy(psiClass, GroupSectionSpec.class);
    if (groupSectionSpecAnnotation == null) {
      return null;
    }

    final String qualifiedName = psiClass.getQualifiedName();
    if (qualifiedName == null) {
      return null;
    }

    return new GroupSectionSpecModel(
        qualifiedName,
        groupSectionSpecAnnotation.value(),
        PsiDelegateMethodExtractor.getDelegateMethods(
            psiClass,
            mGroupSectionSpecDelegateMethodAnnotations,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.<Class<? extends Annotation>>of(ShouldUpdate.class)),
        PsiEventMethodExtractor.getOnEventMethods(psiClass, INTER_STAGE_INPUT_ANNOTATIONS),
        PsiTriggerMethodExtractor.getOnTriggerMethods(psiClass, INTER_STAGE_INPUT_ANNOTATIONS),
        PsiUpdateStateMethodExtractor.getOnUpdateStateMethods(
            psiClass, INTER_STAGE_INPUT_ANNOTATIONS, false),
        PsiTypeVariablesExtractor.getTypeVariables(psiClass),
        PsiPropDefaultsExtractor.getPropDefaults(psiClass),
        PsiEventDeclarationsExtractor.getEventDeclarations(psiClass, GroupSectionSpec.class),
        PsiAnnotationExtractor.extractValidAnnotations(project, psiClass),
        ImmutableList.of(BuilderMethodModel.KEY_BUILDER_METHOD, LOADING_EVENT_BUILDER_METHOD),
        ImmutableList.of(),
        "classJavadoc",
        ImmutableList.of(),
        groupSectionSpecAnnotation.isPublic(),
        SpecElementType.JAVA_CLASS,
        dependencyInjectionHelper,
        psiClass,
        mGroupSectionSpecGenerator,
        PsiFieldsExtractor.extractFields(psiClass));
  }
}
