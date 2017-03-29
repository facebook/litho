/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import com.facebook.litho.annotations.FromBind;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.GetExtraAccessibilityNodeAt;
import com.facebook.litho.annotations.GetExtraAccessibilityNodesCount;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMeasureBaseline;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPopulateAccessibilityNode;
import com.facebook.litho.annotations.OnPopulateExtraAccessibilityNode;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.SpecModel;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

public class MountSpecHelper extends ComponentSpecHelper {

  private enum MountType {
    NONE,
    DRAWABLE,
    VIEW,
  }

  private static final Class<Annotation>[] STAGE_ANNOTATIONS = new Class[] {
      /* Methods that can have inter-stage props - these MUST come first in this list */
      OnPrepare.class,
      OnMeasure.class,
      OnBoundsDefined.class,
      OnBind.class,
      /* Methods that do not support inter-stage props */
      OnMount.class,
      OnPopulateAccessibilityNode.class,
      GetExtraAccessibilityNodesCount.class,
      OnPopulateExtraAccessibilityNode.class,
      GetExtraAccessibilityNodeAt.class,
      OnUnbind.class,
      OnUnmount.class,
  };

  private static final Class<Annotation>[] INTER_STAGE_INPUT_ANNOTATIONS = new Class[] {
      FromPrepare.class,
      FromMeasure.class,
      FromBoundsDefined.class,
      FromBind.class,
  };

  public MountSpecHelper(
      ProcessingEnvironment processingEnv,
      TypeElement specElement,
      SpecModel specModel) {
    super(
        processingEnv,
        specElement,
        specElement.getAnnotation(MountSpec.class).value(),
        specElement.getAnnotation(MountSpec.class).isPublic(),
        STAGE_ANNOTATIONS,
        INTER_STAGE_INPUT_ANNOTATIONS,
        specModel);
  }

  @Override
  protected void validate() {
    if (mQualifiedClassName == null) {
      throw new ComponentsProcessingException(
          mSpecElement,
          "You should either provide an explicit component name " +
          "e.g. @MountSpec(\"MyComponent\"); or suffix your class name with " +
          "\"Spec\" e.g. a \"MyComponentSpec\" class name generates a component named " +
          "\"MyComponent\".");
    }

    if (Utils.getAnnotatedMethod(mStages.getSourceElement(), OnCreateMountContent.class) == null) {
      throw new ComponentsProcessingException(
          mSpecElement,
          "You need to have a method annotated with @OnCreateMountContent in your spec");
    }
  }

  /**
   * Generate an onPrepare implementation that delegates to the @OnPrepare-annotated method.
   */
  public void generateOnPrepare() {
    final ExecutableElement onPrepare = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        OnPrepare.class);
    if (onPrepare == null) {
      return;
    }

    final MethodDescription methodDescription = new MethodDescription();
    methodDescription.annotations = new Class[] { Override.class };
    methodDescription.accessType = Modifier.PROTECTED;
    methodDescription.returnType = null;
    methodDescription.name = "onPrepare";
    methodDescription.parameterTypes = new TypeName[] {ClassNames.COMPONENT_CONTEXT};

    mStages.generateDelegate(
        methodDescription,
        onPrepare,
        ClassNames.COMPONENT);
  }

  /**
   * Generate an onMeasure implementation that delegates to the @OnCreateLayout-annotated method.
   */
  public void generateOnMeasure() {
    final ExecutableElement onMeasure = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        OnMeasure.class);
    if (onMeasure == null) {
      return;
    }

    final MethodDescription methodDescription = new MethodDescription();
    methodDescription.annotations = new Class[] { Override.class };
    methodDescription.accessType = Modifier.PROTECTED;
    methodDescription.name = "onMeasure";
    methodDescription.parameterTypes = new TypeName[] {
        ClassNames.COMPONENT_CONTEXT,
        ClassNames.COMPONENT_LAYOUT,
        ClassName.INT,
        ClassName.INT,
        ClassNames.SIZE,
    };

    mTypeSpec.addMethod(
        MethodSpec.methodBuilder("canMeasure")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PROTECTED)
            .returns(TypeName.BOOLEAN)
            .addStatement("return true")
            .build());

    mStages.generateDelegate(
        methodDescription,
        onMeasure,
        ClassNames.COMPONENT);
  }

  /**
   * Generate an onBoundsDefined implementation that delegates to the
   * @OnBoundsDefined-annotated method.
   */
  public void generateOnBoundsDefined() {
    final ExecutableElement onBoundsDefined = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        OnBoundsDefined.class);
    if (onBoundsDefined == null) {
      return;
    }

    final MethodDescription methodDescription = new MethodDescription();
    methodDescription.annotations = new Class[] { Override.class };
    methodDescription.accessType = Modifier.PROTECTED;
    methodDescription.name = "onBoundsDefined";
    methodDescription.parameterTypes = new TypeName[] {
        ClassNames.COMPONENT_CONTEXT,
        ClassNames.COMPONENT_LAYOUT,
    };

    mStages.generateDelegate(
        methodDescription,
        onBoundsDefined,
        ClassNames.COMPONENT);
  }

  /**
   * Generate an onMeasureBaseline implementation that delegates to the
   * @OnMeasureBaseline-annotated method.
   */
  public void generateOnMeasureBaseline() {
    final ExecutableElement onMeasureBaseline = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        OnMeasureBaseline.class);
    if (onMeasureBaseline == null) {
      return;
    }

    final MethodDescription methodDescription = new MethodDescription();
    methodDescription.annotations = new Class[] { Override.class };
    methodDescription.accessType = Modifier.PROTECTED;
    methodDescription.name = "onMeasureBaseline";
    methodDescription.returnType = TypeName.INT;
    methodDescription.parameterTypes = new TypeName[] {
        ClassNames.COMPONENT_CONTEXT,
        TypeName.INT,
        TypeName.INT,
    };

    mStages.generateDelegate(
        methodDescription,
        onMeasureBaseline,
        ClassNames.COMPONENT);
  }

  /**
   * Generate an onCreateMountContent implementation that delegates to the
   * @OnCreateMountContent-annotated method.
   */
  public void generateOnCreateMountContentAndGetMountType() {
    final ExecutableElement onCreateMountContent = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        OnCreateMountContent.class);

    final MountType componentMountType = getMountType(onCreateMountContent.getReturnType());
    if (componentMountType != MountType.VIEW &&
        componentMountType != MountType.DRAWABLE) {
      throw new ComponentsProcessingException(
          onCreateMountContent,
          "onCreateMountContent's return type should be either a View or a Drawable subclass");
    }

    mTypeSpec.addMethod(
        MethodSpec.methodBuilder("getMountType")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE)
            .addStatement(
                "return $T.$L",
                ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE,
                componentMountType)
            .build());

    mTypeSpec.addMethod(
        MethodSpec.methodBuilder("poolSize")
        .addAnnotation(Override.class)
        .addModifiers(javax.lang.model.element.Modifier.PROTECTED)
        .returns(TypeName.INT)
        .addStatement("return "+mSpecElement.getAnnotation(MountSpec.class).poolSize())
        .build());

    mTypeSpec.addMethod(
        new OnCreateMountContentMethodBuilder()
            .target(mStages.getSourceDelegateAccessorName())
            .delegateName(onCreateMountContent.getSimpleName().toString())
            .build());
  }

  /**
   * Generate an onMount implementation that delegates to the @OnMount-annotated method.
   */
  public void generateOnMount() {
    final ExecutableElement onMount = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        OnMount.class);
    if (onMount == null) {
      return;
    }

    final MethodDescription methodDescription = new MethodDescription();
    methodDescription.annotations = new Class[] { Override.class };
    methodDescription.accessType = Modifier.PROTECTED;
    methodDescription.name = "onMount";
    methodDescription.returnType = ClassName.VOID;
    methodDescription.parameterTypes = new TypeName[] {
        ClassNames.COMPONENT_CONTEXT,
        ClassName.OBJECT,
    };

    if (!Utils.getParametersWithAnnotation(onMount, FromMeasure.class).isEmpty() ||
        !Utils.getParametersWithAnnotation(onMount, FromBoundsDefined.class).isEmpty()) {
      mTypeSpec.addMethod(new IsMountSizeDependentMethodSpecBuilder().build());
    }

    mStages.generateDelegate(
        methodDescription,
        onMount,
        Arrays.asList(ClassNames.COMPONENT_CONTEXT, getMountType()),
        ClassNames.COMPONENT);
  }

  private TypeName getMountType() {
    final ExecutableElement onCreateMountContent = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        OnCreateMountContent.class);
    return ClassName.get(onCreateMountContent.getReturnType());
  }

  /**
   * Generate an onBind implementation that delegates to the @OnBind-annotated method.
   */
  public void generateOnBind() {
    final ExecutableElement onBind = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        OnBind.class);
    if (onBind == null) {
      return;
    }

    final MethodDescription methodDescription = new MethodDescription();
    methodDescription.annotations = new Class[] { Override.class };
    methodDescription.accessType = Modifier.PROTECTED;
    methodDescription.returnType = ClassName.VOID;
    methodDescription.name = "onBind";
    methodDescription.parameterTypes = new TypeName[] {
        ClassNames.COMPONENT_CONTEXT,
        ClassName.OBJECT,
    };

    generateMountCompliantMethod(onBind, methodDescription);
  }

  /**
   * Generate an onUnbind implementation that delegates to the @OnUnbind-annotated method.
   */
  public void generateOnUnbind() {
    final ExecutableElement onIdle = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        OnUnbind.class);
    if (onIdle == null) {
      return;
    }

    final MethodDescription methodDescription = new MethodDescription();
    methodDescription.annotations = new Class[] { Override.class };
    methodDescription.accessType = Modifier.PROTECTED;
    methodDescription.returnType = ClassName.VOID;
    methodDescription.name = "onUnbind";
    methodDescription.parameterTypes = new TypeName[] {
        ClassNames.COMPONENT_CONTEXT,
        ClassName.OBJECT,
    };

    generateMountCompliantMethod(onIdle, methodDescription);
  }

  /**
   * Generate an onUnmount implementation that delegates to the @OnUnmount-annotated method.
   */
  public void generateOnUnmount() {
    final ExecutableElement onUnmount = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        OnUnmount.class);
    if (onUnmount == null) {
      return;
    }

    final MethodDescription methodDescription = new MethodDescription();
    methodDescription.annotations = new Class[] { Override.class };
    methodDescription.accessType = Modifier.PROTECTED;
    methodDescription.returnType = ClassName.VOID;
    methodDescription.name = "onUnmount";
    methodDescription.parameterTypes = new TypeName[] {
        ClassNames.COMPONENT_CONTEXT,
        ClassName.OBJECT,
    };

    generateMountCompliantMethod(onUnmount, methodDescription);
  }

  private void generateMountCompliantMethod(
      ExecutableElement method,
      MethodDescription methodDescription) {
    final ExecutableElement onCreateMountContent = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        OnCreateMountContent.class);

    final TypeMirror mountReturnType = onCreateMountContent.getReturnType();

    final VariableElement mountParameter = method.getParameters().get(1);
    final TypeMirror mountParameterType = mountParameter.asType();

    // The return type of onMount() and the second parameter on onUnmount()
    // should have the same type.
    if (!mStages.isSameType(mountParameterType, mountReturnType)) {
      throw new ComponentsProcessingException(
          mountParameter,
          "Second parameter of " + methodDescription.name + " should be of same type " +
          "of onCreateMountContent()'s return");
    }

    mStages.generateDelegate(
        methodDescription,
        method,
        Arrays.asList(ClassNames.COMPONENT_CONTEXT, ClassName.get(mountParameterType)),
        ClassNames.COMPONENT);
  }

  private static MountType getMountType(TypeMirror returnTypeParam) {
    TypeMirror returnType = returnTypeParam;
    while (returnType.getKind() != TypeKind.NONE && returnType.getKind() != TypeKind.VOID) {
      final TypeElement returnElement = (TypeElement) ((DeclaredType) returnType).asElement();

      final TypeName type = ClassName.get(returnElement);
      if (type.equals(ClassNames.VIEW)) {
        return MountType.VIEW;
      } else if (type.equals(ClassNames.DRAWABLE)) {
        return MountType.DRAWABLE;
      }

      returnType = returnElement.getSuperclass();
    }

    return MountType.NONE;
  }

