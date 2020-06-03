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

import static com.facebook.litho.intellij.completion.MethodCompletionContributor.createMethodLookup;
import static com.facebook.litho.intellij.completion.MethodCompletionContributor.getOrCreateClass;

import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ProcessingContext;
import java.util.Collections;
import java.util.Optional;

/** Provider suggests completion for the Click event in the Litho Spec. */
class OnEventCompletionProvider extends CompletionProvider<CompletionParameters> {
  public static final CompletionProvider<CompletionParameters> INSTANCE =
      new OnEventCompletionProvider();

  @Override
  protected void addCompletions(
      CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
    final Optional<PsiClass> maybeCls =
        CompletionUtils.findFirstParent(parameters.getPosition(), LithoPluginUtils::isLithoSpec);
    if (!maybeCls.isPresent()) return;

    final PsiClass lithoSpecCls = maybeCls.get();
    final PsiClass clickEventCls =
        getOrCreateClass(LithoClassNames.CLICK_EVENT_CLASS_NAME, lithoSpecCls.getProject());
    final PsiMethod onEventMethod =
        OnEventGenerateUtils.createOnEventMethod(
            lithoSpecCls, clickEventCls, Collections.emptyList());
    result.addElement(
        PrioritizedLookupElement.withPriority(
            createMethodLookup(
                onEventMethod,
                clickEventCls,
                OnEventGenerateUtils.createOnEventLookupString(clickEventCls),
                () -> {
                  LithoLoggerProvider.getEventLogger()
                      .log(EventLogger.EVENT_COMPLETION_METHOD + ".OnEvent");
                  LithoPluginUtils.getFirstLayoutSpec(parameters.getOriginalFile())
                      .ifPresent(ComponentGenerateUtils::updateLayoutComponent);
                }),
            Integer.MAX_VALUE));
  }
}
