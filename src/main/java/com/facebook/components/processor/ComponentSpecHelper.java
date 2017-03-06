// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.lang.annotation.Annotation;

import com.facebook.components.annotations.OnCreateInitialState;
import com.facebook.components.annotations.ShouldUpdate;
import com.facebook.components.specmodels.model.ClassNames;
import com.facebook.components.specmodels.model.SpecModel;

import com.squareup.javapoet.TypeName;

public abstract class ComponentSpecHelper extends SpecHelper {

  public ComponentSpecHelper(
      ProcessingEnvironment processingEnv,
      TypeElement specElement,
      String name,
      boolean isPublic,
      Class<Annotation>[] stageAnnotations,
      Class<Annotation>[] interStageInputAnnotations,
      SpecModel specModel) {
    super(
        processingEnv,
        specElement,
        name,
        isPublic,
        stageAnnotations,
        interStageInputAnnotations,
        specModel);
  }

  @Override
  protected TypeName getSuperclassClass() {
    return ClassNames.COMPONENT_LIFECYCLE;
  }

  public void generateShouldUpdate() {
    final ExecutableElement onShouldUpdate = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        ShouldUpdate.class);
    if (onShouldUpdate != null) {
      mStages.generateShouldUpdateMethod(
          onShouldUpdate,
          ClassNames.COMPONENT);
    }
  }

  /**
   * Generate a createInitialState that delegates to the method in the Spec annotated with
   * {@link OnCreateInitialState} annotation.
   */
  public void generateCreateInitialState() {
    final ExecutableElement onCreateInitialState = Utils.getAnnotatedMethod(
        mStages.getSourceElement(),
        OnCreateInitialState.class);
    if (onCreateInitialState == null) {
      return;
    }

    mStages.generateCreateInitialState(
        onCreateInitialState,
        ClassNames.COMPONENT_CONTEXT,
        ClassNames.COMPONENT);
  }

  public void generateTreePropsMethods() {
    mStages.generateTreePropsMethods(ClassNames.COMPONENT_CONTEXT, ClassNames.COMPONENT);
  }

  public void generateEvents() {
    mStages.generateComponentEvents();
  }

  @Override
  protected boolean isStateSupported() {
    return true;
  }
}
