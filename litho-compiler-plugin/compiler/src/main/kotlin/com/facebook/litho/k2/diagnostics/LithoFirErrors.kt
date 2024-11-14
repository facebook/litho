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

package com.facebook.litho.k2.diagnostics

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.NAME_IDENTIFIER
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

object LithoFirErrors : BaseDiagnosticRendererFactory() {
  val HOOK_CONDITIONAL_MISUSE: KtDiagnosticFactory0 by error0<PsiElement>(NAME_IDENTIFIER)
  val HOOK_CALLSITE_MISUSE: KtDiagnosticFactory0 by error0<PsiElement>(NAME_IDENTIFIER)

  override val MAP: KtDiagnosticFactoryToRendererMap =
      KtDiagnosticFactoryToRendererMap("Litho").apply {
        put(
            HOOK_CONDITIONAL_MISUSE,
            "Hooks should not be called from conditionals (if, for, while, or when). " +
                "See: https://www.internalfb.com/intern/staticdocs/litho/docs/mainconcepts/hooks-intro/#rules-for-hooks")
        put(
            HOOK_CALLSITE_MISUSE,
            "Hooks can be only called from render() or another @Hook annotated method. " +
                "See: https://www.internalfb.com/intern/staticdocs/litho/docs/mainconcepts/hooks-intro/#rules-for-hooks")
      }

  init {
    RootDiagnosticRendererFactory.registerFactory(this)
  }
}
