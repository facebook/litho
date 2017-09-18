/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * <p>This source code is licensed under the BSD-style license found in the LICENSE file in the root
 * directory of this source tree. An additional grant of patent rights can be found in the PATENTS
 * file in the same directory.
 */
package com.facebook.litho.sections.specmodels.model;

import com.squareup.javapoet.ClassName;

/**
 * To avoid referring to classes directly in the processor (which would be a circular dependency and
 * would mean the annotation processor might need to process Android-specific class definitions) we
 * provide class name constants instead.
 */
public interface SectionClassNames {

  ClassName STATE_CONTAINER_SECTION =
      ClassName.bestGuess("com.facebook.litho.sections.SectionLifecycle.StateContainer");
  ClassName SECTION = ClassName.bestGuess("com.facebook.litho.sections.Section");
  ClassName SECTION_LIFECYCLE = ClassName.bestGuess("com.facebook.litho.sections.SectionLifecycle");
  ClassName SECTION_CONTEXT = ClassName.bestGuess("com.facebook.litho.sections.SectionContext");
  ClassName CHANGESET = ClassName.bestGuess("com.facebook.litho.sections.ChangeSet");
  ClassName LIST = ClassName.bestGuess("java.util.List");
  ClassName SECTION_STATE_UPDATE =
      ClassName.bestGuess("com.facebook.litho.sections.SectionLifecycle.StateUpdate");
  ClassName LOADING_EVENT_HANDLER = ClassName.bestGuess("com.facebook.litho.sections.LoadingEvent");
  ClassName CHILDREN = ClassName.bestGuess("com.facebook.litho.sections.Children");

  ClassName GROUP_SECTION_SPEC =
      ClassName.bestGuess("com.facebook.litho.sections.annotations.GroupSectionSpec");
  ClassName DIFF_SECTION_SPEC =
      ClassName.bestGuess("com.facebook.litho.sections.annotations.DiffSectionSpec");
}
