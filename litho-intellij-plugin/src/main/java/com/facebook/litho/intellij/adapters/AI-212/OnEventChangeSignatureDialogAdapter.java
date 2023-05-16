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

package com.facebook.litho.intellij.adapters;

import com.facebook.litho.intellij.actions.OnEventMethodDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.refactoring.changeSignature.CallerChooserBase;
import com.intellij.refactoring.changeSignature.ChangeSignatureDialogBase;
import com.intellij.refactoring.changeSignature.JavaMethodDescriptor;
import com.intellij.refactoring.changeSignature.JavaParameterTableModel;
import com.intellij.refactoring.changeSignature.ParameterInfoImpl;
import com.intellij.refactoring.changeSignature.ParameterTableModelItemBase;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Consumer;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public abstract class OnEventChangeSignatureDialogAdapter
    extends ChangeSignatureDialogBase<
        ParameterInfoImpl,
        PsiMethod,
        String,
        JavaMethodDescriptor,
        ParameterTableModelItemBase<ParameterInfoImpl>,
        JavaParameterTableModel> {

  public OnEventChangeSignatureDialogAdapter(
      Project project,
      OnEventMethodDescriptor method,
      boolean allowDelegation,
      PsiElement defaultValueContext) {
    super(project, method, allowDelegation, defaultValueContext);
  }

  @Nullable
  protected CallerChooserBase<PsiMethod> createCallerChooser(
      String title, Tree treeToReuse, Consumer<Set<PsiMethod>> callback) {
    return null;
  }
}
