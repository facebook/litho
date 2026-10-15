/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.intellij.navigation

import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.intellij.PsiSearchUtils
import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.DebounceEventLogger
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.function.Predicate
import java.util.stream.Stream
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/** Utility class helping resolve component class to componentSpec class. */
internal object BaseLithoComponentsDeclarationHandler {

  @JvmField val logger: EventLogger = DebounceEventLogger(4_000)

  /**
   * @param sourceElement component to find declaration for
   * @param isComponentClass filters component type
   * @param hasComponentSpecAnnotation filters resolved componentSpec type
   * @param tag event tag for logging
   * @return declaration target for the provided sourceElement or null if it wasn't found
   */
  @JvmStatic
  fun getGotoDeclarationTarget(
      sourceElement: PsiElement?,
      isComponentClass: Predicate<PsiClass>,
      hasComponentSpecAnnotation: Predicate<PsiClass>,
      tag: String,
  ): PsiElement? {
    // Exclusions
    if (
        sourceElement == null ||
            PsiTreeUtil.getParentOfType(sourceElement, PsiImportStatement::class.java) != null
    ) {
      return null
    }
    val project = sourceElement.project

    val resolvedElement: Stream<PsiElement> =
        // must check first that kotlin plugin is available, otherwise we'll get runtime exception
        // when accessing objects defined in Kotlin plugin
        if (LithoPluginUtils.isKotlinPluginAvailable() && isKotlinFile(sourceElement)) {
          resolveKotlinElement(sourceElement)
        } else {
          resolve(sourceElement)
        }

    val result =
        resolvedElement
            // Filter Component classes
            .filter { it is PsiClass }
            .map { it as PsiClass }
            .filter(isComponentClass)
            // Find Spec classes by name
            .map { it.qualifiedName }
            .filter { it != null }
            .map { name -> LithoPluginUtils.getLithoComponentSpecNameFromComponent(name!!) }
            .map { specName ->
              specName?.let { PsiSearchUtils.getInstance().findOriginalClass(project, it) }
            }
            .filter { it != null }
            .map { it as PsiClass }
            // Filter Spec classes by implementation
            .filter(hasComponentSpecAnnotation)
            .limit(1)
            .findFirst()
            .orElse(null)

    if (result != null) {
      val data = HashMap<String, String>()
      data[EventLogger.KEY_TARGET] = EventLogger.VALUE_NAVIGATION_TARGET_CLASS
      data[EventLogger.KEY_CLASS] = tag
      data[EventLogger.KEY_TYPE] = EventLogger.VALUE_NAVIGATION_TYPE_GOTO
      logger.log(EventLogger.EVENT_NAVIGATION, data)
    }

    return result
  }

  /** @return true if the containing file of the given element is a Kotlin file. */
  private fun isKotlinFile(sourceElement: PsiElement): Boolean {
    return sourceElement.containingFile.language === KotlinLanguage.INSTANCE
  }

  /**
   * @return Stream of resolved elements from the given element or an empty stream if nothing found.
   */
  @JvmStatic
  fun resolve(sourceElement: PsiElement): Stream<PsiElement> {
    if (sourceElement !is PsiIdentifier) {
      return Stream.empty()
    }
    val referenceElement =
        PsiTreeUtil.getParentOfType(sourceElement, PsiJavaCodeReferenceElement::class.java)
            ?: return Stream.empty()
    val references = referenceElement.references
    @Suppress("UNCHECKED_CAST")
    return Stream.of(*references).map { it.resolve() }.filter { it != null } as Stream<PsiElement>
  }

  /**
   * @return Stream of resolved elements from the given Kotlin element or an empty stream if nothing
   *   found.
   */
  @JvmStatic
  fun resolveKotlinElement(sourceElement: PsiElement): Stream<PsiElement> {
    val nameRefExpr =
        PsiTreeUtil.getParentOfType(sourceElement, KtNameReferenceExpression::class.java)
            ?: return Stream.empty()
    val references = nameRefExpr.references
    @Suppress("UNCHECKED_CAST")
    return Stream.of(*references)
        .filter { it is KtSimpleNameReference }
        .map { it.resolve() }
        .filter { it != null } as Stream<PsiElement>
  }
}
