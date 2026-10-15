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

package com.facebook.litho.intellij.inspections

import com.google.common.annotations.VisibleForTesting
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import icons.LithoPluginIcons
import java.util.function.Function

class RequiredPropLineMarkerProvider
@VisibleForTesting
internal constructor(
    private val generatedClassResolver: Function<PsiMethodCallExpression, PsiClass?>,
) : LineMarkerProvider {

  constructor() : this(RequiredPropAnnotator.GENERATED_CLASS_RESOLVER)

  override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
    val methodCalls = mutableListOf<PsiReferenceExpression>()
    val missingPropNames = mutableListOf<Collection<String>>()
    // One annotation per statement
    RequiredPropAnnotator.annotate(
        element,
        { missingRequiredPropNames, createMethodCall ->
          methodCalls.add(createMethodCall)
          missingPropNames.add(missingRequiredPropNames)
        },
        generatedClassResolver,
    )
    if (methodCalls.isEmpty()) {
      return null
    }
    var leaf: PsiElement = methodCalls[0]
    val missingProps = missingPropNames[0]
    while (leaf.firstChild != null) {
      leaf = leaf.firstChild
    }
    @Suppress("DEPRECATION")
    return LineMarkerInfo(
        leaf,
        leaf.textRange,
        LithoPluginIcons.ERROR_ACTION,
        0,
        { RequiredPropAnnotator.createErrorMessage(missingProps) },
        null,
        GutterIconRenderer.Alignment.CENTER,
    )
  }
}
