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

package com.facebook.litho.intellij.toolwindows

import com.facebook.litho.specmodels.model.SpecModel
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.psi.PsiFile

/** Structure view containing both leafs and branches. */
internal class ComponentTreeModel(
    psiFile: PsiFile,
    model: StructureViewTreeElement,
) : StructureViewModelBase(psiFile, model), StructureViewModel.ElementInfoProvider {

  override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean {
    return false
  }

  override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean {
    return false
  }

  companion object {
    @JvmStatic
    fun create(selectedFile: PsiFile, model: SpecModel): StructureViewModel {
      val viewTreeElement = SpecTreeElementFactory.createTree(model)
      return ComponentTreeModel(selectedFile, viewTreeElement)
    }
  }
}
