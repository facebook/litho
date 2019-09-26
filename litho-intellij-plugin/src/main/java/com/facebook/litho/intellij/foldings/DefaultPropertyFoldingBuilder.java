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
package com.facebook.litho.intellij.foldings;

import com.facebook.litho.intellij.LithoPluginUtils;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Uses folding to display the default value of {@code @Prop}-annotated parameters. */
public class DefaultPropertyFoldingBuilder extends FoldingBuilderEx {
  private static final String FOLDING_GROUP_NAME = "DefaultProperties";

  @NotNull
  @Override
  public FoldingDescriptor[] buildFoldRegions(
      @NotNull PsiElement root, @NotNull Document document, boolean quick) {
    FoldingGroup group = FoldingGroup.newGroup(FOLDING_GROUP_NAME);

    final Map<String, PsiExpression> defaultProps =
        PsiTreeUtil.findChildrenOfType(root, PsiField.class).stream()
            .filter(LithoPluginUtils::isPropDefault)
            .filter(field -> field.getInitializer() != null)
            .collect(Collectors.toMap(PsiField::getName, PsiField::getInitializer));

    if (defaultProps.isEmpty()) {
      return FoldingDescriptor.EMPTY;
    }

    return PsiTreeUtil.findChildrenOfType(root, PsiParameter.class).stream()
        .filter(LithoPluginUtils::isProp)
        .map(
            parameter -> {
              String name = parameter.getName();
              if (name == null) {
                return null;
              }
              PsiExpression nameExpression = defaultProps.get(name);
              if (nameExpression == null) {
                return null;
              }
              PsiIdentifier nameIdentifier = parameter.getNameIdentifier();
              if (nameIdentifier == null) {
                return null;
              }
              return new FoldingDescriptor(
                  nameIdentifier.getNode(), nameIdentifier.getTextRange(), group) {
                @Override
                public String getPlaceholderText() {
                  return name + ": " + nameExpression.getText();
                }
              };
            })
        .filter(Objects::nonNull)
        .toArray(FoldingDescriptor[]::new);
  }

  @Nullable
  @Override
  public String getPlaceholderText(@NotNull ASTNode astNode) {
    return "...";
  }

  @Override
  public boolean isCollapsedByDefault(@NotNull ASTNode astNode) {
    return true;
  }
}
