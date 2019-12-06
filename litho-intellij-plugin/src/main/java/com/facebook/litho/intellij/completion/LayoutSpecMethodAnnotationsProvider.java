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
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.OnUpdateStateWithTransition;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import java.util.HashSet;
import java.util.Set;

/**
 * Provider emphasized completion results for valid {@link
 * com.facebook.litho.annotations.LayoutSpec} method annotations.
 */
class LayoutSpecMethodAnnotationsProvider extends CompletionProvider<CompletionParameters> {
  static final CompletionProvider<CompletionParameters> INSTANCE =
      new LayoutSpecMethodAnnotationsProvider();

  static final Set<String> ANNOTATION_QUALIFIED_NAMES = new HashSet<>();

  static {
    for (Class permittedMethod : LayoutSpecModelFactory.DELEGATE_METHOD_ANNOTATIONS) {
      ANNOTATION_QUALIFIED_NAMES.add(permittedMethod.getTypeName());
    }
    ANNOTATION_QUALIFIED_NAMES.add(OnEvent.class.getTypeName());
    ANNOTATION_QUALIFIED_NAMES.add(OnTrigger.class.getTypeName());
    ANNOTATION_QUALIFIED_NAMES.add(OnUpdateState.class.getTypeName());
    ANNOTATION_QUALIFIED_NAMES.add(OnUpdateStateWithTransition.class.getTypeName());
  }

  @Override
  protected void addCompletions(
      CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
    PsiElement position = parameters.getPosition();
    CompletionUtils.findFirstParent(position, LithoPluginUtils::isLayoutSpec)
        .map(layoutSpecCls -> new ReplacingConsumer(ANNOTATION_QUALIFIED_NAMES, result))
        .ifPresent(
            replacingConsumer -> {
              result.runRemainingContributors(parameters, replacingConsumer);
              replacingConsumer.addRemainingCompletions(position.getProject());
            });
  }
}
