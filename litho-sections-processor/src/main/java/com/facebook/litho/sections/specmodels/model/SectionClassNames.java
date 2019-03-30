/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.sections.specmodels.model;

import com.squareup.javapoet.ClassName;

/**
 * To avoid referring to classes directly in the processor (which would be a circular dependency and
 * would mean the annotation processor might need to process Android-specific class definitions) we
 * provide class name constants instead.
 */
public interface SectionClassNames {

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

  ClassName CHANGE_CHANGES_INFO = ClassName.bestGuess("com.facebook.litho.sections.ChangesInfo");
}
