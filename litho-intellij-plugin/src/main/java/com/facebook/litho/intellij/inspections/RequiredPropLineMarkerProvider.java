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

package com.facebook.litho.intellij.inspections;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import icons.LithoPluginIcons;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

public class RequiredPropLineMarkerProvider implements LineMarkerProvider {
  private final Function<PsiMethodCallExpression, PsiClass> generatedClassResolver;

  public RequiredPropLineMarkerProvider() {
    this(RequiredPropAnnotator.GENERATED_CLASS_RESOLVER);
  }

  @VisibleForTesting
  RequiredPropLineMarkerProvider(
      Function<PsiMethodCallExpression, PsiClass> generatedClassResolver) {
    this.generatedClassResolver = generatedClassResolver;
  }

  @Nullable
  @Override
  public LineMarkerInfo getLineMarkerInfo(PsiElement element) {
    final List<PsiReferenceExpression> methodCalls = new ArrayList<>();
    // One annotation per statement
    RequiredPropAnnotator.annotate(
        element,
        (missingRequiredPropNames, createMethodCall) -> methodCalls.add(createMethodCall),
        generatedClassResolver);
    if (methodCalls.isEmpty()) {
      return null;
    }
    PsiElement leaf = methodCalls.get(0);
    while (leaf.getFirstChild() != null) {
      leaf = leaf.getFirstChild();
    }
    return new LineMarkerInfo<>(
        leaf,
        leaf.getTextRange(),
        LithoPluginIcons.ERROR_ACTION,
        0,
        ignored -> "Missing Required Prop",
        null,
        GutterIconRenderer.Alignment.CENTER);
  }

  @Override
  public void collectSlowLineMarkers(
      List<PsiElement> elements, Collection<LineMarkerInfo> result) {}
}
