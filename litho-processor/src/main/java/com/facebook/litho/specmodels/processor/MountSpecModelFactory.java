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

import com.facebook.litho.annotations.FromBind;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.FromMeasureBaseline;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.MountingType;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnEnteredRange;
import com.facebook.litho.annotations.OnExitedRange;
import com.facebook.litho.annotations.OnRegisterRanges;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DefaultMountSpecGenerator;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.facebook.litho.specmodels.model.SpecGenerator;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/** Factory for creating {@link MountSpecModel}s. */
public class MountSpecModelFactory implements SpecModelFactory<MountSpecModel> {
  public static final List<Class<? extends Annotation>> DELEGATE_METHOD_ANNOTATIONS =
      new ArrayList<>();
  static final List<Class<? extends Annotation>> INTER_STAGE_INPUT_ANNOTATIONS = new ArrayList<>();

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
    DELEGATE_METHOD_ANNOTATIONS.add(OnEnteredRange.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnExitedRange.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnRegisterRanges.class);
    DELEGATE_METHOD_ANNOTATIONS.add(OnCalculateCachedValue.class);
  }

  private final SpecGenerator<MountSpecModel> mMountSpecGenerator;

  public MountSpecModelFactory() {
    this(new DefaultMountSpecGenerator());
  }

  public MountSpecModelFactory(SpecGenerator<MountSpecModel> mountSpecGenerator) {
    mMountSpecGenerator = mountSpecGenerator;
  }

  @Override
  public Set<Element> extract(RoundEnvironment roundEnvironment) {
    return (Set<Element>) roundEnvironment.getElementsAnnotatedWith(MountSpec.class);
  }

  /**
   * Create a {@link MountSpecModel} from the given {@link TypeElement} and an optional {@link
   * DependencyInjectionHelper}.
   */
  @Override
  public MountSpecModel create(
      Elements elements,
      Types types,
      TypeElement element,
      Messager messager,
      EnumSet<RunMode> runMode,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      @Nullable InterStageStore interStageStore) {
    return new MountSpecModel(
        element.getQualifiedName().toString(),
        element.getAnnotation(MountSpec.class).value(),
        DelegateMethodExtractor.getDelegateMethods(
            element,
            DELEGATE_METHOD_ANNOTATIONS,
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
        interStageStore == null
            ? ImmutableList.of()
            : CachedPropNameExtractor.getCachedPropNames(
                interStageStore, element.getQualifiedName()),
        ImmutableList.copyOf(TypeVariablesExtractor.getTypeVariables(element)),
        ImmutableList.copyOf(PropDefaultsExtractor.getPropDefaults(element)),
        EventDeclarationsExtractor.getEventDeclarations(
            elements, element, MountSpec.class, runMode),
        JavadocExtractor.getClassJavadoc(elements, element),
        AnnotationExtractor.extractValidAnnotations(element),
        TagExtractor.extractTagsFromSpecClass(types, element, runMode),
        JavadocExtractor.getPropJavadocs(elements, element),
        element.getAnnotation(MountSpec.class).isPublic(),
        dependencyInjectionHelper,
        element.getAnnotation(MountSpec.class).isPureRender(),
        element.getAnnotation(MountSpec.class).hasChildLithoViews(),
        element.getAnnotation(MountSpec.class).poolSize(),
        element.getAnnotation(MountSpec.class).canPreallocate(),
        getMountType(elements, element, runMode),
        SpecElementTypeDeterminator.determine(element),
        element,
        mMountSpecGenerator,
        FieldsExtractor.extractFields(element),
        BindDynamicValuesMethodExtractor.getOnBindDynamicValuesMethods(element, messager));
  }

  private static TypeName getMountType(
      Elements elements, TypeElement element, EnumSet<RunMode> runMode) {
    TypeElement viewType = elements.getTypeElement(ClassNames.VIEW_NAME);
    TypeElement drawableType = elements.getTypeElement(ClassNames.DRAWABLE_NAME);

    for (Element enclosedElement : element.getEnclosedElements()) {
      if (enclosedElement.getKind() != ElementKind.METHOD) {
        continue;
      }

      OnCreateMountContent annotation = enclosedElement.getAnnotation(OnCreateMountContent.class);
      if (annotation != null) {
        if (annotation.mountingType() == MountingType.VIEW) {
          return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_VIEW;
        }
        if (annotation.mountingType() == MountingType.DRAWABLE) {
          return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_DRAWABLE;
        }

        TypeMirror initialReturnType = ((ExecutableElement) enclosedElement).getReturnType();
        if (runMode.contains(RunMode.ABI)) {
          // We can't access the supertypes of the return type, so let's guess, and we'll verify
          // that our guess was correct when we do a full build later.
          if (initialReturnType.toString().contains("Drawable")) {
            return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_DRAWABLE;
          } else {
            return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_VIEW;
          }
        }

        TypeMirror returnType = initialReturnType;
        while (returnType.getKind() != TypeKind.NONE && returnType.getKind() != TypeKind.VOID) {
          final TypeElement returnElement = (TypeElement) ((DeclaredType) returnType).asElement();

          if (returnElement.equals(viewType)) {
            if (initialReturnType.toString().contains("Drawable")) {
              throw new ComponentsProcessingException(
                  "Mount type cannot be correctly inferred from the name of "
                      + element
                      + ".  Please specify `@OnCreateMountContent(mountingType = MountingType.VIEW)`.");
            }

            return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_VIEW;
          } else if (returnElement.equals(drawableType)) {
            if (!initialReturnType.toString().contains("Drawable")) {
              throw new ComponentsProcessingException(
                  "Mount type cannot be correctly inferred from the name of "
                      + element
                      + ".  Please specify `@OnCreateMountContent(mountingType = MountingType.DRAWABLE)`.");
            }
            return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_DRAWABLE;
          }

          try {
            returnType = returnElement.getSuperclass();
          } catch (RuntimeException e) {
            throw new ComponentsProcessingException(
                "Failed to get mount type for "
                    + element
                    + ".  Try specifying `@OnCreateMountContent(mountingType = MountingType.VIEW)` (or DRAWABLE).");
          }
        }
      }
    }

    return ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_NONE;
  }
}
