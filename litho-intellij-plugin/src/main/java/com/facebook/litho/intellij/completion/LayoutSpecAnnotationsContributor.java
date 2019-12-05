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

import static com.facebook.litho.intellij.completion.CompletionUtils.METHOD_ANNOTATION;

import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.OnUpdateStateWithTransition;
import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.JavaClassNameCompletionContributor;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ProcessingContext;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Contributor suggests completion for the available annotations in the Layout Spec. */
public class LayoutSpecAnnotationsContributor extends CompletionContributor {

  public LayoutSpecAnnotationsContributor() {
    extend(
        CompletionType.BASIC, METHOD_ANNOTATION, LayoutSpecAnnotationsCompletionProvider.INSTANCE);
  }

  static class LayoutSpecAnnotationsCompletionProvider
      extends CompletionProvider<CompletionParameters> {
    static final CompletionProvider<CompletionParameters> INSTANCE =
        new LayoutSpecAnnotationsCompletionProvider();

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
      CompletionUtils.findFirstParent(parameters.getPosition(), LithoPluginUtils::isLayoutSpec)
          .map(
              layoutSpecCls ->
                  new ReplacingConsumer(
                      ANNOTATION_QUALIFIED_NAMES, result, layoutSpecCls.getProject()))
          .ifPresent(
              replacingConsumer -> {
                result.runRemainingContributors(parameters, replacingConsumer);
                replacingConsumer.addRemainingCompletions();
              });
    }
  }

  /**
   * Consumer adds custom {@link LookupElement} items to the given {@link #result}. During
   * consumption if it receives same type as in {@link #replacedQualifiedNames} it creates new one,
   * otherwise passes consumed {@link CompletionResult} unchanged.
   *
   * <p>It should be passed before other consumers.
   *
   * @see #addRemainingCompletions()
   */
  static class ReplacingConsumer implements Consumer<CompletionResult> {
    private final Set<String> replacedQualifiedNames;
    private final CompletionResultSet result;
    private final Project project;

    ReplacingConsumer(
        Collection<String> replacedQualifiedNames, CompletionResultSet result, Project project) {
      this.replacedQualifiedNames = new HashSet<>(replacedQualifiedNames);
      this.result = result;
      this.project = project;
    }

    @Override
    public void consume(CompletionResult completionResult) {
      PsiElement psiElement = completionResult.getLookupElement().getPsiElement();
      Optional<String> qualifiedName =
          Optional.ofNullable(psiElement)
              .filter(PsiClass.class::isInstance)
              .map(psiClass -> ((PsiClass) psiClass).getQualifiedName())
              .filter(replacedQualifiedNames::remove);
      if (qualifiedName.isPresent()) {
        result.addElement(SpecLookupElement.create((PsiClass) psiElement));
      } else {
        result.passResult(completionResult);
      }
    }

    /**
     * Adds {@link LookupElement} for any {@link #replacedQualifiedNames} unseen during consumption.
     */
    void addRemainingCompletions() {
      for (String qualifiedName : replacedQualifiedNames) {
        result.addElement(SpecLookupElement.create(qualifiedName, project));
      }
    }
  }

  /** Lookup item logs its usage, and renders an element with Litho type. */
  static class SpecLookupElement extends LookupElementDecorator<LookupItem> {
    static final Map<String, LookupElement> CACHE = new HashMap<>(20);

    /**
     * @param qualifiedName the name of the class to create lookup.
     * @param project to find the lookup annotation class.
     * @throws IncorrectOperationException if the qualifiedName does not specify a valid type.
     */
    static LookupElement create(String qualifiedName, Project project)
        throws IncorrectOperationException {
      if (CACHE.containsKey(qualifiedName)) {
        return CACHE.get(qualifiedName);
      }
      PsiClass typeCls = PsiSearchUtils.findClass(project, qualifiedName);
      if (typeCls != null) {
        SpecLookupElement lookupElement = new SpecLookupElement(typeCls);
        CACHE.put(qualifiedName, lookupElement);
        return lookupElement;
      }
      typeCls =
          JavaPsiFacade.getInstance(project)
              .getElementFactory()
              .createClass(LithoClassNames.shortName(qualifiedName));
      return new SpecLookupElement(typeCls);
    }

    static LookupElement create(PsiClass typeCls) {
      String qualifiedName = typeCls.getQualifiedName();
      if (CACHE.containsKey(qualifiedName)) {
        return CACHE.get(qualifiedName);
      }
      SpecLookupElement lookupElement = new SpecLookupElement(typeCls);
      CACHE.put(qualifiedName, lookupElement);
      return lookupElement;
    }

    private SpecLookupElement(PsiClass typeCls) {
      super(JavaClassNameCompletionContributor.createClassLookupItem(typeCls, true));
    }

    @Override
    public void handleInsert(InsertionContext context) {
      super.handleInsert(context);
      LithoLoggerProvider.getEventLogger()
          .log(EventLogger.EVENT_COMPLETION_ANNOTATION + "." + super.getLookupString());
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
      super.renderElement(presentation);
      presentation.setTypeText("Litho");
      presentation.setTypeGrayed(true);
    }

    @Override
    public boolean isCaseSensitive() {
      return false;
    }
  }
}
