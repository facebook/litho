// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import java.io.Closeable;
import java.lang.annotation.Annotation;

import com.facebook.components.annotations.OnAcquire;
import com.facebook.components.annotations.OnRelease;
import com.facebook.components.annotations.ReferenceSpec;
import com.facebook.components.annotations.ShouldUpdate;
import com.facebook.components.specmodels.model.ClassNames;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

public class ReferenceSpecHelper extends SpecHelper implements Closeable {

  private static final Class<Annotation>[] STAGE_ANNOTATIONS = new Class[]{
      OnAcquire.class,
      OnRelease.class,
  };

  private TypeMirror mReferenceType;

  public ReferenceSpecHelper(
      ProcessingEnvironment processingEnv,
      TypeElement specElement,
      boolean isPublic,
      String name) {
    super(processingEnv, specElement, name, isPublic, STAGE_ANNOTATIONS, new Class[0], null);
  }

  @Override
  protected void validate() {
    if (mQualifiedClassName == null) {
      throw new ComponentsProcessingException(
          mSpecElement,
          "You should  suffix your class name with " +
          "\"Spec\" e.g. a \"MyReferenceSpec\" class name generates a component named " +
          "\"MyReference\".");
    }

    if (Utils.getAnnotatedMethod(mSpecElement, OnAcquire.class) == null) {
      throw new ComponentsProcessingException(
          mSpecElement,
          "You need to have a method annotated with @OnAcquire in your spec");
    }
  }

  @Override
  protected TypeName getSuperclassClass() {
    return ParameterizedTypeName.get(
        ClassNames.REFERENCE_LIFECYCLE,
        ClassName.get(getReferenceType()));
  }

  public TypeMirror getReferenceType() {
    if (mReferenceType == null) {
      computeReferenceType();
    }

    return mReferenceType;
  }

  private void computeReferenceType() {
    final ExecutableElement onAcquire = Utils.getAnnotatedMethod(mSpecElement, OnAcquire.class);
    mReferenceType =  onAcquire.getReturnType();
  }

  public void generateOnAcquire() {
    final ExecutableElement onAcquire = Utils.getAnnotatedMethod(mSpecElement, OnAcquire.class);

    final MethodDescription methodDescription = new MethodDescription();
    methodDescription.annotations = new Class[] { Override.class };
    methodDescription.accessType = Modifier.PROTECTED;
    methodDescription.returnType = ClassName.get(onAcquire.getReturnType());
    methodDescription.name = "onAcquire";
    methodDescription.parameterTypes = new TypeName[] {
        ClassNames.COMPONENT_CONTEXT
    };

    mStages.generateDelegate(
        methodDescription,
        onAcquire,
        ClassNames.REFERENCE);
  }

  public void generateOnRelease() {
    final ExecutableElement onRelease = Utils.getAnnotatedMethod(mSpecElement, OnRelease.class);
    if (onRelease == null) {
      return;
    }

    final MethodDescription methodDescription = new MethodDescription();
    methodDescription.annotations = new Class[] { Override.class };
    methodDescription.accessType = Modifier.PROTECTED;
    methodDescription.returnType = ClassName.get(onRelease.getReturnType());
    methodDescription.name = "onRelease";
    methodDescription.parameterTypes = new TypeName[] {
        ClassNames.COMPONENT_CONTEXT,
        ClassName.get(getReferenceType())
    };

    mStages.generateDelegate(
        methodDescription,
        onRelease,
        ClassNames.REFERENCE);
  }

  public void generateShouldUpdate() {
    final ExecutableElement onShouldUpdate = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        ShouldUpdate.class);
    if (onShouldUpdate != null) {
      mStages.generateShouldUpdateMethod(
          onShouldUpdate,
          ClassNames.REFERENCE);
    }
  }

  @Override
  public Class getSpecAnnotationClass() {
    return ReferenceSpec.class;
  }
}
