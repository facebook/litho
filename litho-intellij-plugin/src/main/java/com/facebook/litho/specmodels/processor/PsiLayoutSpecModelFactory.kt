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

import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.ShouldUpdate
import com.facebook.litho.specmodels.internal.ImmutableList
import com.facebook.litho.specmodels.model.DefaultLayoutSpecGenerator
import com.facebook.litho.specmodels.model.LayoutSpecModel
import com.facebook.litho.specmodels.model.PropJavadocModel
import com.facebook.litho.specmodels.model.SpecElementType
import com.facebook.litho.specmodels.model.SpecGenerator
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory.DELEGATE_METHOD_ANNOTATIONS
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory.INTER_STAGE_INPUT_ANNOTATIONS
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass

/** Factory for creating [LayoutSpecModel]s. */
class PsiLayoutSpecModelFactory(
    private val layoutSpecDelegateMethodAnnotations: List<Class<out Annotation>>,
    private val layoutSpecGenerator: SpecGenerator<LayoutSpecModel>,
) {

  constructor() : this(DELEGATE_METHOD_ANNOTATIONS, DefaultLayoutSpecGenerator())

  /**
   * @return a new [LayoutSpecModel] or null if provided class isn't a [LayoutSpec] class. Access is
   *   allowed from event dispatch thread or inside read-action only.
   */
  fun createWithPsi(project: Project, psiClass: PsiClass): LayoutSpecModel? {
    @Suppress("UNCHECKED_CAST")
    val layoutSpecAnnotation =
        PsiAnnotationProxyUtils.findAnnotationInHierarchy(
            psiClass,
            LayoutSpec::class.java as Class<java.lang.annotation.Annotation>,
        ) as LayoutSpec? ?: return null

    // #5 trigger methods
    val triggerMethods =
        PsiTriggerMethodExtractor.getOnTriggerMethods(
            psiClass,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.of(),
        )

    // #12 classJavadoc
    val classJavadoc = "classJavadoc"

    // #13 JavadocExtractor.getPropJavadocs()
    val propJavadocs = ImmutableList.of<PropJavadocModel>()

    return LayoutSpecModel(
        psiClass.qualifiedName,
        layoutSpecAnnotation.value,
        PsiDelegateMethodExtractor.getDelegateMethods(
            psiClass,
            layoutSpecDelegateMethodAnnotations,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.of(),
            ImmutableList.of<Class<out Annotation>>(ShouldUpdate::class.java),
        ),
        PsiEventMethodExtractor.getOnEventMethods(
            psiClass,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.of(),
        ),
        triggerMethods,
        PsiWorkingRangesMethodExtractor.getRegisterMethod(
            psiClass,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.of(),
        ),
        PsiWorkingRangesMethodExtractor.getRangesMethods(
            psiClass,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.of(),
        ),
        PsiUpdateStateMethodExtractor.getOnUpdateStateMethods(
            psiClass,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.of(),
            false,
        ),
        PsiUpdateStateMethodExtractor.getOnUpdateStateMethods(
            psiClass,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.of(),
            true,
        ),
        ImmutableList.of<String>(),
        PsiPropDefaultsExtractor.getPropDefaults(psiClass),
        PsiEventDeclarationsExtractor.getEventDeclarations(psiClass, LayoutSpec::class.java),
        PsiAnnotationExtractor.extractValidAnnotations(project, psiClass),
        null,
        classJavadoc,
        propJavadocs,
        layoutSpecAnnotation.isPublic,
        SpecElementType.JAVA_CLASS,
        psiClass,
        layoutSpecGenerator,
        PsiTypeVariablesExtractor.getTypeVariables(psiClass),
        PsiFieldsExtractor.extractFields(psiClass),
        null,
    )
  }
}
