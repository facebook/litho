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
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandlerBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public class ComponentDeclarationHandler extends GotoDeclarationHandlerBase {
  @Nullable
  @Override
  public PsiElement getGotoDeclarationTarget(@Nullable PsiElement sourceElement, Editor editor) {
    return BaseLithoComponentsDeclarationHandler.getGotoDeclarationTarget(
        sourceElement, LithoPluginUtils::isComponentClass, LithoPluginUtils::hasLithoAnnotation);
  }
}
