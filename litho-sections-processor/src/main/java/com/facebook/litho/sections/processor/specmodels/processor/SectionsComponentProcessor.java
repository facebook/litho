/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor.specmodels.processor;

import com.facebook.litho.sections.processor.specmodels.model.SectionClassNames;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.processor.AbstractComponentsProcessor;
import com.facebook.litho.specmodels.processor.SpecModelFactory;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class SectionsComponentProcessor extends AbstractComponentsProcessor {

  public SectionsComponentProcessor() {
    super(
        ImmutableList.<SpecModelFactory>of(
            new GroupSectionSpecModelFactory(), new DiffSectionSpecModelFactory()),
        null);
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new LinkedHashSet<>(
        Arrays.asList(
            SectionClassNames.GROUP_SECTION_SPEC.toString(),
            SectionClassNames.DIFF_SECTION_SPEC.toString()));
  }
}
