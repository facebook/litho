/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor;

import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnBindService;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.annotations.OnCreateService;
import com.facebook.litho.sections.annotations.OnDataBound;
import com.facebook.litho.sections.annotations.OnDestroyService;
import com.facebook.litho.sections.annotations.OnRefresh;
import com.facebook.litho.sections.annotations.OnUnbindService;
import com.facebook.litho.sections.annotations.OnViewportChanged;
import com.facebook.litho.sections.processor.specmodels.model.GroupSectionSpecModel;
import com.facebook.litho.specmodels.processor.ComponentsProcessingException;
import java.lang.annotation.Annotation;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/** Helper class to generate code for {@link GroupSectionSpec}. */
public class GroupSectionSpecHelper extends ListSpecHelper<GroupSectionSpecModel> {

  protected static final Class<Annotation>[] STAGE_ANNOTATIONS = new Class[] {
      OnBindService.class,
      OnCreateChildren.class,
      OnCreateService.class,
      OnDataBound.class,
      OnDestroyService.class,
      OnRefresh.class,
      OnUnbindService.class,
      OnViewportChanged.class,
  };

  protected static final Class<Annotation>[] INTER_STAGE_INPUT_ANNOTATIONS = new Class[] {};

  public GroupSectionSpecHelper(
      ProcessingEnvironment processingEnv,
      TypeElement specElement,
      boolean isPublic,
      GroupSectionSpecModel specModel) {
    super(
        processingEnv,
        specElement,
        specElement.getAnnotation(GroupSectionSpec.class).value(),
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
          "You should either provide an explicit section name " +
          "e.g. @SectionSpec(\"MyComponent\"); or suffix your class name with " +
          "\"Spec\" e.g. a \"MySectionSpec\" class name generates a section named " +
          "\"MySection\".");
    }

    if (Utils.getAnnotatedMethod(mStages.getSourceElement(), OnCreateChildren.class) == null) {
      throw new ComponentsProcessingException(
          mSpecElement,
          "You need to have a method annotated with @OnCreateChildren in your spec");
    }
  }

  public void generateTreePropsMethods() {
    mStages.generateTreePropsMethods(SectionClassNames.SECTION_CONTEXT, SectionClassNames.SECTION);
  }

  @Override
  public Class getSpecAnnotationClass() {
    return GroupSectionSpec.class;
  }
}
