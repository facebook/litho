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

import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.JavaClassNameCompletionContributor;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.util.IncorrectOperationException;
import java.util.HashMap;
import java.util.Map;

/** Lookup item logs its usage, and renders an element with Litho type. */
class SpecLookupElement extends LookupElementDecorator<LookupItem> {
  static final Map<String, LookupElement> CACHE = new HashMap<>(20);

  /**
   * @param qualifiedName the name of the class to create lookup
   * @param project to find the lookup annotation class
   * @throws IncorrectOperationException if the qualifiedName does not specify a valid type
   * @return new {@link LookupElement} or cached instance if it was created previously
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
    // This is a dummy class, we don't want to cache it.
    typeCls =
        JavaPsiFacade.getInstance(project)
            .getElementFactory()
            .createClass(LithoClassNames.shortName(qualifiedName));
    return new SpecLookupElement(typeCls);
  }

  /**
   * @param typeCls the class to create lookup
   * @return new {@link LookupElement} or cached instance if it was created previously
   */
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
