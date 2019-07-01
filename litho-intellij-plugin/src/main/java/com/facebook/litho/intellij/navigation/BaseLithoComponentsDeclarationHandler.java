/*
 * Copyright 2004-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.intellij.navigation;

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** Utility class helping resolve component class to componentSpec class. */
class BaseLithoComponentsDeclarationHandler {
  static final EventLogger logger = new DebounceEventLogger(500);

  private BaseLithoComponentsDeclarationHandler() {}

  /**
   * @param sourceElement component to find declaration for
   * @param isComponentClass filters component type
   * @param hasComponentSpecAnnotation filters resolved componentSpec type
   * @param event event tag for logging
   * @return declaration target for the provided sourceElement or null if it wasn't found
   */
  @Nullable
  static PsiElement getGotoDeclarationTarget(
      @Nullable PsiElement sourceElement,
      Predicate<PsiClass> isComponentClass,
      Predicate<PsiClass> hasComponentSpecAnnotation,
      String event) {
    // Exclusions
    if (sourceElement == null
        || PsiTreeUtil.getParentOfType(sourceElement, PsiImportStatement.class) != null) {
      return null;
    }
    final Project project = sourceElement.getProject();

    return resolve(sourceElement)
        // Filter Component classes
        .filter(PsiClass.class::isInstance)
        .map(PsiClass.class::cast)
        .filter(isComponentClass)
        // Find Spec classes by name
        .map(PsiClass::getQualifiedName)
        .filter(Objects::nonNull)
        .map(LithoPluginUtils::getLithoComponentSpecNameFromComponent)
        .flatMap(
            specName -> {
              GlobalSearchScope scope = GlobalSearchScope.everythingScope(project);
              return Stream.of(JavaPsiFacade.getInstance(project).findClasses(specName, scope));
            })
        // Filter Spec classes by implementation
        .filter(hasComponentSpecAnnotation)
        .limit(1)
        .peek(psiClass -> logger.log(event))
        .findFirst()
        .orElse(null);
  }

  /**
   * @return Stream of resolved elements from the given element or an empty stream if nothing found.
   */
  private static Stream<PsiElement> resolve(PsiElement sourceElement) {
    return Optional.of(sourceElement)
        .filter(PsiIdentifier.class::isInstance)
        .map(element -> PsiTreeUtil.getParentOfType(element, PsiJavaCodeReferenceElement.class))
        .map(PsiElement::getReferences)
        .map(
            psiReferences ->
                Stream.of(psiReferences).map(PsiReference::resolve).filter(Objects::nonNull))
        .orElse(Stream.empty());
  }
}
