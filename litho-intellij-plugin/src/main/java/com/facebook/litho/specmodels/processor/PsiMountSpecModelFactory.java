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

import static com.facebook.litho.specmodels.processor.MountSpecModelFactory.DELEGATE_METHOD_ANNOTATIONS;
import static com.facebook.litho.specmodels.processor.MountSpecModelFactory.INTER_STAGE_INPUT_ANNOTATIONS;

import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DefaultMountSpecGenerator;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.facebook.litho.specmodels.model.SpecElementType;
import com.facebook.litho.specmodels.model.SpecGenerator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/** Factory for creating {@link MountSpecModel}s. */
public class PsiMountSpecModelFactory {
  private final List<Class<? extends Annotation>> mMountSpecDelegateMethodAnnotations;
  private final SpecGenerator<MountSpecModel> mMountSpecGenerator;

  public PsiMountSpecModelFactory() {
    this(DELEGATE_METHOD_ANNOTATIONS, new DefaultMountSpecGenerator());
  }

  public PsiMountSpecModelFactory(
      List<Class<? extends Annotation>> mountSpecDelegateMethodAnnotations,
      SpecGenerator<MountSpecModel> mountSpecGenerator) {
    mMountSpecDelegateMethodAnnotations = mountSpecDelegateMethodAnnotations;
    mMountSpecGenerator = mountSpecGenerator;
  }

  /** @return a new {@link MountSpecModel} or null */
  @Nullable
  public MountSpecModel createWithPsi(
      Project project,
      PsiClass psiClass,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper) {
    final MountSpec mountSpecAnnotation =
        PsiAnnotationProxyUtils.findAnnotationInHierarchy(psiClass, MountSpec.class);
    if (mountSpecAnnotation == null) {
      return null;
    }
    final String className = psiClass.getQualifiedName();
    if (className == null) {
      return null;
    }

    return new MountSpecModel(
        className,
        mountSpecAnnotation.value(),
        PsiDelegateMethodExtractor.getDelegateMethods(
            psiClass,
            mMountSpecDelegateMethodAnnotations,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.of(ShouldUpdate.class)),
        PsiEventMethodExtractor.getOnEventMethods(psiClass, INTER_STAGE_INPUT_ANNOTATIONS),
        PsiTriggerMethodExtractor.getOnTriggerMethods(psiClass, INTER_STAGE_INPUT_ANNOTATIONS),
        PsiWorkingRangesMethodExtractor.getRegisterMethod(psiClass, INTER_STAGE_INPUT_ANNOTATIONS),
        PsiWorkingRangesMethodExtractor.getRangesMethods(psiClass, INTER_STAGE_INPUT_ANNOTATIONS),
        PsiUpdateStateMethodExtractor.getOnUpdateStateMethods(
            psiClass, INTER_STAGE_INPUT_ANNOTATIONS, false),
        PsiUpdateStateMethodExtractor.getOnUpdateStateMethods(
            psiClass, INTER_STAGE_INPUT_ANNOTATIONS, true),
        ImmutableList.of(),
        PsiTypeVariablesExtractor.getTypeVariables(psiClass),
        PsiPropDefaultsExtractor.getPropDefaults(psiClass),
        PsiEventDeclarationsExtractor.getEventDeclarations(psiClass, MountSpec.class),
        "classJavadoc",
        PsiAnnotationExtractor.extractValidAnnotations(project, psiClass),
        ImmutableList.of(),
        ImmutableList.of(),
        mountSpecAnnotation.isPublic(),
        dependencyInjectionHelper,
        mountSpecAnnotation.isPureRender(),
        mountSpecAnnotation.hasChildLithoViews(),
        mountSpecAnnotation.poolSize(),
        mountSpecAnnotation.canPreallocate(),
        getMountType(psiClass),
        SpecElementType.JAVA_CLASS,
        psiClass,
        mMountSpecGenerator,
        PsiFieldsExtractor.extractFields(psiClass),
        PsiBindDynamicValuesMethodExtractor.getOnBindDynamicValuesMethods(psiClass));
  }

  private static TypeName getMountType(PsiClass psiClass) {
    String onCreateMountContentClassName = OnCreateMountContent.class.getTypeName();
    for (PsiMethod psiMethod : psiClass.getAllMethods()) {
      final PsiAnnotation onCreateMountContentAnnotation =
          psiMethod.getAnnotation(onCreateMountContentClassName);
      if (onCreateMountContentAnnotation == null) {
        continue;
      }

      final PsiAnnotationMemberValue psiAnnotationMemberValue =
          onCreateMountContentAnnotation.findAttributeValue("mountingType");
      if (psiAnnotationMemberValue != null
          && psiAnnotationMemberValue.textMatches("MountingType.VIEW")) {
        return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_VIEW;
      }
      if (psiAnnotationMemberValue != null
          && psiAnnotationMemberValue.textMatches("MountingType.DRAWABLE")) {
        return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_DRAWABLE;
      }

      final PsiType initialReturnType = psiMethod.getReturnType();
      PsiType returnType = initialReturnType;
      while (returnType != null && !returnType.getPresentableText().equals("void")) {
        if (returnType.getCanonicalText().equals(ClassNames.VIEW_NAME)) {
          if (initialReturnType.getPresentableText().contains("Drawable")) {
            return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_NONE;
          }
          return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_VIEW;
        } else if (returnType.getCanonicalText().equals(ClassNames.DRAWABLE_NAME)) {
          if (!initialReturnType.toString().contains("Drawable")) {
            return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_NONE;
          }
          return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_DRAWABLE;
        }
        try {
          returnType = returnType.getSuperTypes()[0];
        } catch (ArrayIndexOutOfBoundsException e) {
          return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_NONE;
        }
      }
    }

    return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_NONE;
  }
}
