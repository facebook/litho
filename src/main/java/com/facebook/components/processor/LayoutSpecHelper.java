// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import java.lang.annotation.Annotation;

import com.facebook.components.annotations.FromCreateLayout;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.components.specmodels.model.SpecModel;

public class LayoutSpecHelper extends ComponentSpecHelper {
  private static final Class<Annotation>[] STAGE_ANNOTATIONS = new Class[] {
      OnCreateLayout.class,
      OnCreateLayoutWithSizeSpec.class,
  };

  private static final Class<Annotation>[] INTER_STAGE_INPUT_ANNOTATIONS = new Class[] {
      FromCreateLayout.class,
  };

  public LayoutSpecHelper(
      ProcessingEnvironment processingEnv,
      TypeElement specElement,
      SpecModel specModel) {
    super(
        processingEnv,
        specElement,
        specElement.getAnnotation(LayoutSpec.class).value(),
        specElement.getAnnotation(LayoutSpec.class).isPublic(),
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
          "e.g. @LayoutSpec(\"MyComponent\"); or suffix your class name with " +
          "\"Spec\" e.g. a \"MyComponentSpec\" class name generates a component named " +
          "\"MyComponent\".");
    }
  }

  @Override
  public Class getSpecAnnotationClass() {
    return LayoutSpec.class;
  }
}
