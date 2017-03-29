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
