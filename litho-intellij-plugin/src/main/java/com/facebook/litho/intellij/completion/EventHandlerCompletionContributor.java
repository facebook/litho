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

import static com.facebook.litho.intellij.LithoPluginUtils.resolveEventName;

import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/** Offers completion for the {@code @EventHandler} method parameters in the Litho Spec class. */
public class EventHandlerCompletionContributor extends CompletionContributor {
  public EventHandlerCompletionContributor() {
    extend(CompletionType.BASIC, codeReferencePattern(), typeCompletionProvider());
  }

  private static ElementPattern<? extends PsiElement> codeReferencePattern() {
    return PlatformPatterns.psiElement(PsiIdentifier.class)
        .withParent(PsiJavaCodeReferenceElement.class)
        .withLanguage(JavaLanguage.INSTANCE);
  }

  private static CompletionProvider<CompletionParameters> typeCompletionProvider() {
    return new CompletionProvider<CompletionParameters>() {
      @Override
      protected void addCompletions(
          @Nullable CompletionParameters completionParameters,
          ProcessingContext processingContext,
          @Nullable CompletionResultSet completionResultSet) {
        if (completionResultSet == null) {
          return;
        }
        if (completionParameters == null) {
          return;
        }
        PsiElement element = completionParameters.getPosition();

        PsiMethodCallExpression eventHandlerSetter =
            PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (eventHandlerSetter == null) {
          return;
        }
        final String eventQualifiedName = resolveEventName(eventHandlerSetter);
        if (eventQualifiedName == null) {
          return;
        }
        final PsiClass parentCls =
            (PsiClass) PsiTreeUtil.findFirstParent(eventHandlerSetter, PsiClass.class::isInstance);
        if (parentCls == null) {
          return;
        }
        final SpecModel parentModel =
            ComponentGenerateService.getInstance().getOrCreateSpecModel(parentCls);
        if (parentModel == null) {
          return;
        }
        final ImmutableList<SpecMethodModel<EventMethod, EventDeclarationModel>>
            implementedEventHandlers = parentModel.getEventMethods();

        implementedEventHandlers.stream()
            .filter(handler -> eventQualifiedName.equals(handler.typeModel.getReflectionName()))
            .map(
                handler ->
                    createLookupElement(
                        eventHandlerSetter.getMethodExpression().getReferenceName(),
                        parentModel.getComponentName(),
                        handler))
            .map(
                lookupElement ->
                    PrioritizedLookupElement.withPriority(lookupElement, Integer.MAX_VALUE))
            .forEach(completionResultSet::addElement);
      }
    };
  }

  private static LookupElement createLookupElement(
      String methodCallName,
      String componentName,
      SpecMethodModel<EventMethod, EventDeclarationModel> handler) {
    String methodName = handler.name.toString();
    return LookupElementBuilder.create(componentName + "." + methodName + "()")
        .withLookupStrings(Arrays.asList(methodName, methodCallName))
        .withPresentableText(methodName)
        .withTypeText("EventHandler<" + handler.typeModel.getReflectionName() + ">")
        .withInsertHandler(
            (context, elem) -> {
              context.getEditor().getCaretModel().moveCaretRelatively(-1, 0, false, false, false);
              log("EventHandler");
            });
  }

  private static void log(String cls) {
    final Map<String, String> data = new HashMap<>();
    data.put(EventLogger.KEY_TARGET, EventLogger.VALUE_COMPLETION_TARGET_ARGUMENT);
    data.put(EventLogger.KEY_CLASS, cls);
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_COMPLETION, data);
  }
}
