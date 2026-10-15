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

package com.facebook.litho.intellij.foldings

import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.intellij.services.ComponentGenerateService
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.PsiTreeUtil

/** Uses folding to display the default value of `@Prop`-annotated parameters. */
class DefaultPropertyFoldingBuilder : FoldingBuilderEx() {

  override fun buildFoldRegions(
      root: PsiElement,
      document: Document,
      quick: Boolean,
  ): Array<FoldingDescriptor> {
    if (root !is PsiFile) {
      return FoldingDescriptor.EMPTY
    }
    val propDefaults = getPropDefaults(root)
    if (propDefaults.isEmpty()) {
      return FoldingDescriptor.EMPTY
    }

    val group = FoldingGroup.newGroup(FOLDING_GROUP_NAME)
    return PsiTreeUtil.findChildrenOfType(root, PsiParameter::class.java)
        .asSequence()
        .filter { LithoPluginUtils.isProp(it) }
        .mapNotNull { createFoldingDescriptor(it, propDefaults, group) }
        .toList()
        .toTypedArray()
  }

  override fun getPlaceholderText(astNode: ASTNode): String? {
    val foldingText = astNode.getUserData(FOLDING_KEY)
    return foldingText ?: "..."
  }

  override fun isCollapsedByDefault(astNode: ASTNode): Boolean {
    return true
  }

  companion object {
    private const val FOLDING_GROUP_NAME = "DefaultProperties"
    private val FOLDING_KEY: Key<String> =
        Key.create("com.facebook.litho.intellij.foldings.DefaultPropertyFoldingBuilder")

    private fun getPropDefaults(root: PsiFile): Map<String, PsiField> {
      val cls =
          LithoPluginUtils.getFirstClass(root) { c ->
            LithoPluginUtils.hasLithoComponentSpecAnnotation(c) ||
                LithoPluginUtils.hasLithoSectionSpecAnnotation(c)
          } ?: return emptyMap()

      val model =
          ComponentGenerateService.getInstance().getOrCreateSpecModel(cls) ?: return emptyMap()

      return buildMap {
        model.propDefaults
            .filter { it.mRepresentedObject is PsiField /* see: PsiPropDefaultsExtractor */ }
            .forEach { propDefaultModel ->
              val previous =
                  put(propDefaultModel.mName, propDefaultModel.mRepresentedObject as PsiField)
              check(previous == null) { "Duplicate key ${propDefaultModel.mName}" }
            }
      }
    }

    private fun createFoldingDescriptor(
        parameter: PsiParameter,
        propDefaults: Map<String, PsiField>,
        group: FoldingGroup,
    ): FoldingDescriptor? {
      val name = parameter.name
      val nameIdentifier = parameter.nameIdentifier ?: return null
      val propDefault = propDefaults[name] ?: return null
      val initializer = propDefault.initializer ?: return null
      val foldingText = "$name: ${initializer.text}"
      val node = nameIdentifier.node
      node.putUserData(FOLDING_KEY, foldingText)
      return object : FoldingDescriptor(node, nameIdentifier.textRange, group) {
        override fun getPlaceholderText(): String {
          return foldingText
        }
      }
    }
  }
}
