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

import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.ShouldUpdate
import com.facebook.litho.specmodels.internal.ImmutableList
import com.facebook.litho.specmodels.model.ClassNames
import com.facebook.litho.specmodels.model.DefaultMountSpecGenerator
import com.facebook.litho.specmodels.model.MountSpecModel
import com.facebook.litho.specmodels.model.SpecElementType
import com.facebook.litho.specmodels.model.SpecGenerator
import com.facebook.litho.specmodels.processor.MountSpecModelFactory.DELEGATE_METHOD_ANNOTATIONS
import com.facebook.litho.specmodels.processor.MountSpecModelFactory.INTER_STAGE_INPUT_ANNOTATIONS
import com.facebook.litho.specmodels.processor.MountSpecModelFactory.PREPARE_INTER_STAGE_INPUT_ANNOTATIONS
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.squareup.javapoet.TypeName

/** Factory for creating [MountSpecModel]s. */
class PsiMountSpecModelFactory(
    private val mountSpecDelegateMethodAnnotations: List<Class<out Annotation>>,
    private val mountSpecGenerator: SpecGenerator<MountSpecModel>,
) {

  constructor() : this(DELEGATE_METHOD_ANNOTATIONS, DefaultMountSpecGenerator())

  /** @return a new [MountSpecModel] or null */
  @Suppress("DEPRECATION")
  fun createWithPsi(project: Project, psiClass: PsiClass): MountSpecModel? {
    @Suppress("UNCHECKED_CAST")
    val mountSpecAnnotation =
        PsiAnnotationProxyUtils.findAnnotationInHierarchy(
            psiClass,
            MountSpec::class.java as Class<java.lang.annotation.Annotation>,
        ) as MountSpec? ?: return null
    val className = psiClass.qualifiedName ?: return null

    return MountSpecModel(
        className,
        mountSpecAnnotation.value,
        PsiDelegateMethodExtractor.getDelegateMethods(
            psiClass,
            mountSpecDelegateMethodAnnotations,
            INTER_STAGE_INPUT_ANNOTATIONS,
            PREPARE_INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.of(ShouldUpdate::class.java),
        ),
        PsiEventMethodExtractor.getOnEventMethods(
            psiClass,
            INTER_STAGE_INPUT_ANNOTATIONS,
            PREPARE_INTER_STAGE_INPUT_ANNOTATIONS,
        ),
        PsiTriggerMethodExtractor.getOnTriggerMethods(
            psiClass,
            INTER_STAGE_INPUT_ANNOTATIONS,
            PREPARE_INTER_STAGE_INPUT_ANNOTATIONS,
        ),
        PsiWorkingRangesMethodExtractor.getRegisterMethod(
            psiClass,
            INTER_STAGE_INPUT_ANNOTATIONS,
            PREPARE_INTER_STAGE_INPUT_ANNOTATIONS,
        ),
        PsiWorkingRangesMethodExtractor.getRangesMethods(
            psiClass,
            INTER_STAGE_INPUT_ANNOTATIONS,
            PREPARE_INTER_STAGE_INPUT_ANNOTATIONS,
        ),
        PsiUpdateStateMethodExtractor.getOnUpdateStateMethods(
            psiClass,
            INTER_STAGE_INPUT_ANNOTATIONS,
            PREPARE_INTER_STAGE_INPUT_ANNOTATIONS,
            false,
        ),
        PsiUpdateStateMethodExtractor.getOnUpdateStateMethods(
            psiClass,
            INTER_STAGE_INPUT_ANNOTATIONS,
            PREPARE_INTER_STAGE_INPUT_ANNOTATIONS,
            true,
        ),
        ImmutableList.of(),
        PsiTypeVariablesExtractor.getTypeVariables(psiClass),
        PsiPropDefaultsExtractor.getPropDefaults(psiClass),
        PsiEventDeclarationsExtractor.getEventDeclarations(psiClass, MountSpec::class.java),
        "classJavadoc",
        PsiAnnotationExtractor.extractValidAnnotations(project, psiClass),
        ImmutableList.of(),
        ImmutableList.of(),
        mountSpecAnnotation.isPublic,
        mountSpecAnnotation.isPureRender,
        mountSpecAnnotation.hasIntrinsicSize,
        mountSpecAnnotation.hasChildLithoViews,
        mountSpecAnnotation.poolSize,
        mountSpecAnnotation.canPreallocate,
        getMountType(psiClass),
        SpecElementType.JAVA_CLASS,
        psiClass,
        mountSpecGenerator,
        PsiFieldsExtractor.extractFields(psiClass),
        PsiBindDynamicValuesMethodExtractor.getOnBindDynamicValuesMethods(psiClass),
    )
  }

  companion object {
    private fun getMountType(psiClass: PsiClass): TypeName {
      val onCreateMountContentClassName = OnCreateMountContent::class.java.typeName
      for (psiMethod in psiClass.allMethods) {
        val onCreateMountContentAnnotation =
            psiMethod.getAnnotation(onCreateMountContentClassName) ?: continue

        val psiAnnotationMemberValue =
            onCreateMountContentAnnotation.findAttributeValue("mountingType")
        if (
            psiAnnotationMemberValue != null &&
                psiAnnotationMemberValue.textMatches("MountingType.VIEW")
        ) {
          return ClassNames.COMPONENT_MOUNT_TYPE_VIEW
        }
        if (
            psiAnnotationMemberValue != null &&
                psiAnnotationMemberValue.textMatches("MountingType.DRAWABLE")
        ) {
          return ClassNames.COMPONENT_MOUNT_TYPE_DRAWABLE
        }

        val initialReturnType = psiMethod.returnType
        var returnType = initialReturnType
        while (returnType != null && returnType.presentableText != "void") {
          if (returnType.canonicalText == ClassNames.VIEW_NAME) {
            if (
                initialReturnType != null && initialReturnType.presentableText.contains("Drawable")
            ) {
              return ClassNames.COMPONENT_MOUNT_TYPE_NONE
            } else {
              return ClassNames.COMPONENT_MOUNT_TYPE_VIEW
            }
          } else if (returnType.canonicalText == ClassNames.DRAWABLE_NAME) {
            if (
                initialReturnType != null && initialReturnType.presentableText.contains("Drawable")
            ) {
              return ClassNames.COMPONENT_MOUNT_TYPE_DRAWABLE
            } else {
              return ClassNames.COMPONENT_MOUNT_TYPE_NONE
            }
          }
          try {
            returnType = returnType.superTypes[0]
          } catch (e: ArrayIndexOutOfBoundsException) {
            return ClassNames.COMPONENT_MOUNT_TYPE_NONE
          }
        }
      }

      return ClassNames.COMPONENT_MOUNT_TYPE_NONE
    }
  }
}
