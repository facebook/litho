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
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.JavaClassNameCompletionContributor;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.util.IncorrectOperationException;
import java.util.HashMap;
import java.util.Map;

/** Lookup item renders an element with Litho type. */
class SpecLookupElement extends LookupElementDecorator<LookupElement> {
  private static final Map<String, LookupElement> CACHE = new HashMap<>(20);
  private final InsertHandler<LookupElement> insertHandler;

  /**
   * @param qualifiedName the name of the class to create lookup
   * @param project to find the lookup annotation class
   * @param insertHandler adds custom actions to the insert handling
   * @throws IncorrectOperationException if the qualifiedName does not specify a valid type
   * @return new {@link LookupElement} or cached instance if it was created previously
   */
  static LookupElement create(
      String qualifiedName, Project project, InsertHandler<LookupElement> insertHandler)
      throws IncorrectOperationException {
    if (CACHE.containsKey(qualifiedName)) {
      return CACHE.get(qualifiedName);
    }
    PsiClass typeCls = PsiSearchUtils.findClass(project, qualifiedName);
    if (typeCls != null) {
      SpecLookupElement lookupElement = new SpecLookupElement(typeCls, insertHandler);
      CACHE.put(qualifiedName, lookupElement);
      return lookupElement;
    }
    // This is a dummy class, we don't want to cache it.
    typeCls =
        JavaPsiFacade.getInstance(project)
            .getElementFactory()
            .createClass(LithoClassNames.shortName(qualifiedName));
    return new SpecLookupElement(typeCls, insertHandler);
  }

  static LookupElement create(
      LookupElement delegate, String qualifiedName, InsertHandler<LookupElement> insertHandler) {
    SpecLookupElement lookupElement = new SpecLookupElement(delegate, insertHandler);
    CACHE.put(qualifiedName, lookupElement);
    return lookupElement;
  }

  private SpecLookupElement(PsiClass typeCls, InsertHandler<LookupElement> insertHandler) {
    this(JavaClassNameCompletionContributor.createClassLookupItem(typeCls, true), insertHandler);
  }

  private SpecLookupElement(
      LookupElement delegate, InsertHandler<LookupElement> lookupElementDecoratorInsertHandler) {
    super(delegate);
    insertHandler = lookupElementDecoratorInsertHandler;
  }

  @Override
  public void handleInsert(InsertionContext context) {
    super.handleInsert(context);
    insertHandler.handleInsert(context, this);
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
