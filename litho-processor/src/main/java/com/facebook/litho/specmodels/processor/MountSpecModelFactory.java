/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.annotations.FromBind;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.FromMeasureBaseline;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

/** Factory for creating {@link MountSpecModel}s. */
public class MountSpecModelFactory {
  private static final List<Class<? extends Annotation>> INTER_STAGE_INPUT_ANNOTATIONS =
      new ArrayList<>();
  private static final List<Class<? extends Annotation>> DELEGATE_METHOD_ANNOTATIONS =
      new ArrayList<>();

  static {
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromPrepare.class);
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromMeasureBaseline.class);
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromMeasure.class);
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromBoundsDefined.class);
    INTER_STAGE_INPUT_ANNOTATIONS.add(FromBind.class);
    DELEGATE_METHOD_ANNOTATIONS.addAll(
        DelegateMethodDescriptions.MOUNT_SPEC_DELEGATE_METHODS_MAP.keySet());
    DELEGATE_METHOD_ANNOTATIONS.add(OnCreateTreeProp.class);
    DELEGATE_METHOD_ANNOTATIONS.add(ShouldUpdate.class);
  }

  /**
   * Create a {@link MountSpecModel} from the given {@link TypeElement} and an optional {@link
   * DependencyInjectionHelper}.
   */
  public static MountSpecModel create(
      Elements elements,
      TypeElement element,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper) {
    return new MountSpecModel(
        element.getQualifiedName().toString(),
        element.getAnnotation(MountSpec.class).value(),
        DelegateMethodExtractor.getDelegateMethods(
            element, DELEGATE_METHOD_ANNOTATIONS, INTER_STAGE_INPUT_ANNOTATIONS),
        EventMethodExtractor.getOnEventMethods(elements, element, INTER_STAGE_INPUT_ANNOTATIONS),
        UpdateStateMethodExtractor.getOnUpdateStateMethods(element, INTER_STAGE_INPUT_ANNOTATIONS),
        ImmutableList.copyOf(TypeVariablesExtractor.getTypeVariables(element)),
        ImmutableList.copyOf(PropDefaultsExtractor.getPropDefaults(element)),
        EventDeclarationsExtractor.getEventDeclarations(elements, element, MountSpec.class),
        JavadocExtractor.getClassJavadoc(elements, element),
        AnnotationExtractor.extractValidAnnotations(element),
        JavadocExtractor.getPropJavadocs(elements, element),
        element.getAnnotation(MountSpec.class).isPublic(),
        dependencyInjectionHelper,
        element.getAnnotation(MountSpec.class).isPureRender(),
        element.getAnnotation(MountSpec.class).canMountIncrementally(),
        element.getAnnotation(MountSpec.class).shouldUseDisplayList(),
        element.getAnnotation(MountSpec.class).poolSize(),
        getMountType(element),
        element);
  }

  private static TypeName getMountType(TypeElement element) {
    for (Element enclosedElement : element.getEnclosedElements()) {
      if (enclosedElement.getKind() != ElementKind.METHOD) {
        continue;
      }

      if (enclosedElement.getAnnotation(OnCreateMountContent.class) != null) {
        TypeMirror returnType = ((ExecutableElement) enclosedElement).getReturnType();
        while (returnType.getKind() != TypeKind.NONE && returnType.getKind() != TypeKind.VOID) {
          final TypeElement returnElement = (TypeElement) ((DeclaredType) returnType).asElement();

          final TypeName type = ClassName.get(returnElement);
          if (type.equals(ClassNames.VIEW)) {
            return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_VIEW;
          } else if (type.equals(ClassNames.DRAWABLE)) {
            return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_DRAWABLE;
          }
          returnType = returnElement.getSuperclass();
        }
      }
    }

    return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_NONE;
  }
}
