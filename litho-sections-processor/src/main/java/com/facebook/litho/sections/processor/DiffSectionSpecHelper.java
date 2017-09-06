/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor;

import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.OnBindService;
import com.facebook.litho.sections.annotations.OnCreateService;
import com.facebook.litho.sections.annotations.OnDataBound;
import com.facebook.litho.sections.annotations.OnDestroyService;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.sections.annotations.OnRefresh;
import com.facebook.litho.sections.annotations.OnUnbindService;
import com.facebook.litho.sections.annotations.OnViewportChanged;
import com.facebook.litho.sections.processor.specmodels.model.DiffSectionSpecModel;
import com.facebook.litho.specmodels.processor.ComponentsProcessingException;
import java.lang.annotation.Annotation;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/** Helper class to generate code for {@link DiffSectionSpec}. */
public class DiffSectionSpecHelper extends ListSpecHelper<DiffSectionSpecModel> {

  protected static final Class[] STAGE_ANNOTATIONS = new Class[] {
      OnBindService.class,
      OnCreateService.class,
      OnDataBound.class,
      OnDestroyService.class,
      OnDiff.class,
      OnRefresh.class,
      OnUnbindService.class,
      OnViewportChanged.class,
  };

  protected static final Class<Annotation>[] INTER_STAGE_INPUT_ANNOTATIONS = new Class[] {};

  public DiffSectionSpecHelper(
      ProcessingEnvironment processingEnv,
      TypeElement specElement,
      boolean isPublic,
      DiffSectionSpecModel specModel) {
    super(
        processingEnv,
        specElement,
        specElement.getAnnotation(DiffSectionSpec.class).value(),
        isPublic,
        STAGE_ANNOTATIONS,
        INTER_STAGE_INPUT_ANNOTATIONS,
        specModel);
  }

  @Override
  protected void validate() {
    if (mQualifiedClassName == null) {
      throw new ComponentsProcessingException(
          mSpecElement,
          "You should either provide an explicit changeset name " +
          "e.g. @DiffSectionSpec(\"MyChangeset\"); or suffix your class name with " +
          "\"Spec\" e.g. a \"MyDiffSectionSpec\" class name generates a section named " +
          "\"MyChangeset\".");
    }
  }

  @Override
  public Class getSpecAnnotationClass() {
    return DiffSectionSpec.class;
  }
}
