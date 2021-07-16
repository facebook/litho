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

package com.facebook.litho.intellij.completion;

import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.sections.specmodels.processor.GroupSectionSpecModelFactory;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

public class SectionSpecMethodAnnotationsProvider extends CompletionProvider<CompletionParameters> {
  static final String ANNOTATION_PREFIX = "SectionSpec";
  static final CompletionProvider<CompletionParameters> INSTANCE =
      new SectionSpecMethodAnnotationsProvider();

  static final Set<String> ANNOTATION_QUALIFIED_NAMES = new HashSet<>();

  static {
    for (Class<? extends Annotation> permittedMethod :
        GroupSectionSpecModelFactory.DELEGATE_METHOD_ANNOTATIONS) {
      ANNOTATION_QUALIFIED_NAMES.add(permittedMethod.getTypeName());
    }
    ANNOTATION_QUALIFIED_NAMES.add(OnEvent.class.getTypeName());
    ANNOTATION_QUALIFIED_NAMES.add(OnUpdateState.class.getTypeName());
  }

  @Override
  protected void addCompletions(
      CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
    MethodCompletionContributor.addMethodCompletions(
        parameters.getPosition(),
        LithoPluginUtils::hasLithoSectionSpecAnnotation,
        ANNOTATION_PREFIX,
        ANNOTATION_QUALIFIED_NAMES,
        result);
  }
}
