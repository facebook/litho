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

package com.facebook.litho.specmodels.processor

import com.facebook.litho.specmodels.internal.ImmutableList
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiKeyword
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import javax.lang.model.element.Modifier

/** Extractor provides [Modifier]s. */
internal object PsiModifierExtractor {

  private val EMPTY: Array<Modifier> = emptyArray()

  @JvmStatic
  fun extractModifiers(psiField: PsiField): Array<Modifier> {
    val modifierList = psiField.modifierList
    return if (modifierList == null) {
      EMPTY
    } else {
      extractModifiersInternal(modifierList).toTypedArray()
    }
  }

  @JvmStatic
  fun extractModifiers(modifierList: PsiModifierList): ImmutableList<Modifier> {
    return ImmutableList.copyOf(extractModifiersInternal(modifierList))
  }

  private fun extractModifiersInternal(modifierList: PsiModifierList): List<Modifier> {
    val modifiers = ArrayList<Modifier>()

    val children = modifierList.children
    for (child in children) {
      if (child is PsiModifier || child is PsiKeyword) {
        modifiers.add(psiModifierToModifier(child))
      }
    }
    return modifiers
  }

  private fun psiModifierToModifier(psiModifier: PsiElement): Modifier {
    return when (psiModifier.text) {
      PsiModifier.ABSTRACT -> {
        Modifier.ABSTRACT
      }
      PsiModifier.FINAL -> {
        Modifier.FINAL
      }
      PsiModifier.NATIVE -> {
        Modifier.NATIVE
      }
      PsiModifier.PRIVATE -> {
        Modifier.PRIVATE
      }
      PsiModifier.PROTECTED -> {
        Modifier.PROTECTED
      }
      PsiModifier.PUBLIC -> {
        Modifier.PUBLIC
      }
      PsiModifier.STATIC -> {
        Modifier.STATIC
      }
      PsiModifier.STRICTFP -> {
        Modifier.STRICTFP
      }
      PsiModifier.SYNCHRONIZED -> {
        Modifier.SYNCHRONIZED
      }
      PsiModifier.TRANSIENT -> {
        Modifier.TRANSIENT
      }
      PsiModifier.VOLATILE -> {
        Modifier.VOLATILE
      }
      else -> {
        // TODO better error message, ideally w/ line number
        throw ComponentsProcessingException("Unexpected Modifier, modifier is: " + psiModifier.text)
      }
    }
  }
}
