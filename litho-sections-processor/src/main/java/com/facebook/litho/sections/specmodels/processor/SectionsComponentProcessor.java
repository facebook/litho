/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.sections.specmodels.processor;

import com.facebook.litho.sections.specmodels.model.SectionClassNames;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.processor.AbstractComponentsProcessor;
import com.facebook.litho.specmodels.processor.SpecModelFactory;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
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
