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

package com.facebook.litho.specmodels.processor

import com.facebook.litho.annotations.ShouldUpdate
import com.facebook.litho.sections.annotations.GroupSectionSpec
import com.facebook.litho.sections.specmodels.model.DefaultGroupSectionSpecGenerator
import com.facebook.litho.sections.specmodels.model.GroupSectionSpecModel
import com.facebook.litho.sections.specmodels.model.SectionClassNames
import com.facebook.litho.sections.specmodels.processor.GroupSectionSpecModelFactory.DELEGATE_METHOD_ANNOTATIONS
import com.facebook.litho.specmodels.internal.ImmutableList
import com.facebook.litho.specmodels.model.BuilderMethodModel
import com.facebook.litho.specmodels.model.ClassNames
import com.facebook.litho.specmodels.model.SpecElementType
import com.facebook.litho.specmodels.model.SpecGenerator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.squareup.javapoet.ParameterizedTypeName

/** Factory for creating [GroupSectionSpecModel]s. */
class PsiGroupSectionSpecModelFactory(
    private val groupSectionSpecDelegateMethodAnnotations: List<Class<out Annotation>>,
    private val groupSectionSpecGenerator: SpecGenerator<GroupSectionSpecModel>,
) {

  constructor() : this(DELEGATE_METHOD_ANNOTATIONS, DefaultGroupSectionSpecGenerator())

  /**
   * @return a new [GroupSectionSpecModel] or null if provided class isn't a [GroupSectionSpec]
   *   class. Access is allowed from event dispatch thread or inside read-action only.
   */
  fun createWithPsi(project: Project, psiClass: PsiClass): GroupSectionSpecModel? {
    @Suppress("UNCHECKED_CAST")
    val groupSectionSpecAnnotation =
        PsiAnnotationProxyUtils.findAnnotationInHierarchy(
            psiClass,
            GroupSectionSpec::class.java as Class<java.lang.annotation.Annotation>,
        ) as GroupSectionSpec? ?: return null

    val qualifiedName = psiClass.qualifiedName ?: return null

    return GroupSectionSpecModel(
        qualifiedName,
        groupSectionSpecAnnotation.value,
        PsiDelegateMethodExtractor.getDelegateMethods(
            psiClass,
            groupSectionSpecDelegateMethodAnnotations,
            ImmutableList.of(),
            ImmutableList.of(),
            ImmutableList.of<Class<out Annotation>>(ShouldUpdate::class.java),
        ),
        PsiEventMethodExtractor.getOnEventMethods(psiClass, ImmutableList.of(), ImmutableList.of()),
        PsiTriggerMethodExtractor.getOnTriggerMethods(
            psiClass,
            ImmutableList.of(),
            ImmutableList.of(),
        ),
        PsiUpdateStateMethodExtractor.getOnUpdateStateMethods(
            psiClass,
            ImmutableList.of(),
            ImmutableList.of(),
            false,
        ),
        PsiTypeVariablesExtractor.getTypeVariables(psiClass),
        PsiPropDefaultsExtractor.getPropDefaults(psiClass),
        PsiEventDeclarationsExtractor.getEventDeclarations(psiClass, GroupSectionSpec::class.java),
        PsiAnnotationExtractor.extractValidAnnotations(project, psiClass),
        ImmutableList.of(BuilderMethodModel.KEY_BUILDER_METHOD, LOADING_EVENT_BUILDER_METHOD),
        ImmutableList.of(),
        "classJavadoc",
        ImmutableList.of(),
        groupSectionSpecAnnotation.isPublic,
        SpecElementType.JAVA_CLASS,
        psiClass,
        groupSectionSpecGenerator,
        PsiFieldsExtractor.extractFields(psiClass),
    )
  }

  companion object {
    private val LOADING_EVENT_BUILDER_METHOD = BuilderMethodModel(
        ParameterizedTypeName.get(
            ClassNames.EVENT_HANDLER,
            SectionClassNames.LOADING_EVENT_HANDLER,
        ),
        "loadingEventHandler",
    )
  }
}
