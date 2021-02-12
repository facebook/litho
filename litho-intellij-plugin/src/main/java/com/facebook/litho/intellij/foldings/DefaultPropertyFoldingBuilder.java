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

package com.facebook.litho.intellij.foldings;

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

/** Uses folding to display the default value of {@code @Prop}-annotated parameters. */
public class DefaultPropertyFoldingBuilder extends FoldingBuilderEx {
  private static final String FOLDING_GROUP_NAME = "DefaultProperties";
  private static final Key<String> FOLDING_KEY =
      Key.create("com.facebook.litho.intellij.foldings.DefaultPropertyFoldingBuilder");

  @Override
  public FoldingDescriptor[] buildFoldRegions(PsiElement root, Document document, boolean quick) {
    if (!(root instanceof PsiFile)) {
      return FoldingDescriptor.EMPTY;
    }
    final Map<String, PsiField> propDefaults = getPropDefaults((PsiFile) root);
    if (propDefaults.isEmpty()) {
      return FoldingDescriptor.EMPTY;
    }

    final FoldingGroup group = FoldingGroup.newGroup(FOLDING_GROUP_NAME);
    return PsiTreeUtil.findChildrenOfType(root, PsiParameter.class).stream()
        .filter(LithoPluginUtils::isProp)
        .map(parameter -> createFoldingDescriptor(parameter, propDefaults, group))
        .filter(Objects::nonNull)
        .toArray(FoldingDescriptor[]::new);
  }

  @Nullable
  @Override
  public String getPlaceholderText(ASTNode astNode) {
    final String foldingText = astNode.getUserData(FOLDING_KEY);
    return foldingText != null ? foldingText : "...";
  }

  @Override
  public boolean isCollapsedByDefault(ASTNode astNode) {
    return true;
  }

  private static Map<String, PsiField> getPropDefaults(PsiFile root) {
    return LithoPluginUtils.getFirstClass(root, LithoPluginUtils::hasLithoComponentSpecAnnotation)
        .map(cls -> ComponentGenerateService.getInstance().getOrCreateSpecModel(cls))
        .map(
            model ->
                model.getPropDefaults().stream()
                    .filter(
                        propDefaultModel ->
                            propDefaultModel.mRepresentedObject
                                instanceof PsiField /* see: PsiPropDefaultsExtractor */)
                    .collect(
                        Collectors.toMap(
                            _model -> _model.mName,
                            _model -> (PsiField) _model.mRepresentedObject)))
        .orElse(Collections.emptyMap());
  }

  @Nullable
  private static FoldingDescriptor createFoldingDescriptor(
      PsiParameter parameter, Map<String, PsiField> propDefaults, FoldingGroup group) {
    final String name = parameter.getName();
    final PsiIdentifier nameIdentifier = parameter.getNameIdentifier();
    if (nameIdentifier == null) {
      return null;
    }
    final String foldingText =
        Optional.ofNullable(propDefaults.get(name))
            .map(PsiVariable::getInitializer)
            .map(psiExpression -> name + ": " + psiExpression.getText())
            .orElse(null);
    if (foldingText == null) {
      return null;
    }
    final ASTNode node = nameIdentifier.getNode();
    node.putUserData(FOLDING_KEY, foldingText);
    return new FoldingDescriptor(node, nameIdentifier.getTextRange(), group) {
      @Override
      public String getPlaceholderText() {
        return foldingText;
      }
    };
  }
}
