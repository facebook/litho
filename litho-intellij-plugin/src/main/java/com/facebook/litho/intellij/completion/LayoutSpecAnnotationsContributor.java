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

import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.annotations.OnEnteredRange;
import com.facebook.litho.annotations.OnError;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnExitedRange;
import com.facebook.litho.annotations.OnRegisterRanges;
import com.facebook.litho.annotations.OnShouldCreateLayoutWithNewSizeSpec;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.OnUpdateStateWithTransition;
import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.JavaClassNameCompletionContributor;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
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

  private static final CompletionProvider<CompletionParameters> LAYOUT_SPEC_ANNOTATION_PROVIDER =
      new LayoutSpecAnnotationsCompletionProvider();

  public LayoutSpecAnnotationsContributor() {
    extend(CompletionType.BASIC, METHOD_ANNOTATION, LAYOUT_SPEC_ANNOTATION_PROVIDER);
  }

  static class LayoutSpecAnnotationsCompletionProvider
      extends CompletionProvider<CompletionParameters> {

    static final Set<String> ANNOTATION_QUALIFIED_NAMES = new HashSet<>();

    static {
      ANNOTATION_QUALIFIED_NAMES.add(OnAttached.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnCalculateCachedValue.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnCreateInitialState.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnCreateLayout.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnCreateLayoutWithSizeSpec.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnCreateTreeProp.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnCreateTransition.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnDetached.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnEnteredRange.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnError.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnEvent.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnExitedRange.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnRegisterRanges.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnShouldCreateLayoutWithNewSizeSpec.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnTrigger.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnUpdateStateWithTransition.class.getTypeName());
      ANNOTATION_QUALIFIED_NAMES.add(OnUpdateState.class.getTypeName());
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
      Optional<String> qualifiedName =
          Optional.ofNullable(completionResult.getLookupElement().getPsiElement())
              .filter(PsiClass.class::isInstance)
              .map(PsiClass.class::cast)
              .map(PsiClass::getQualifiedName)
              .filter(replacedQualifiedNames::remove);
      if (qualifiedName.isPresent()) {
        result.addElement(SpecLookupElement.create(qualifiedName.get(), project));
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

  /** Lookup item logs its usage, and renders an element with Spec type shown. */
  static class SpecLookupElement extends LookupElement {
    static final Map<String, LookupElement> CACHE = new HashMap<>(20);
    private final LookupItem<Object> lookupItem;

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

    private SpecLookupElement(PsiClass typeCls) {
      this(JavaClassNameCompletionContributor.createClassLookupItem(typeCls, true));
    }

    private SpecLookupElement(LookupItem<Object> lookupItem) {
      this.lookupItem = lookupItem;
    }

    @Override
    public String getLookupString() {
      return lookupItem.getLookupString();
    }

    @Override
    public Set<String> getAllLookupStrings() {
      return lookupItem.getAllLookupStrings();
    }

    @Override
    public void handleInsert(InsertionContext context) {
      lookupItem.handleInsert(context);

      LithoLoggerProvider.getEventLogger()
          .log(EventLogger.EVENT_COMPLETION_ANNOTATION + "." + lookupItem.getPresentableText());
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
      lookupItem.renderElement(presentation);
      presentation.setTypeText("Litho");
      presentation.setTypeGrayed(true);
    }

    @Override
    public boolean isCaseSensitive() {
      return false;
    }

    @Override
    public int hashCode() {
      return lookupItem.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return lookupItem.equals(o);
    }

    @Override
    public Object getObject() {
      return lookupItem.getObject();
    }
  }
}
