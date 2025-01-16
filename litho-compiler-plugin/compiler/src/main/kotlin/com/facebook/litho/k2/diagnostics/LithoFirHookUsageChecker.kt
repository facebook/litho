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

import com.facebook.litho.common.LithoNames
import com.facebook.litho.k2.isLoop
import com.facebook.litho.k2.isRenderMethod
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

/**
 * Checks that the rules surrounding hooks usage are not violated, specifically around usage in
 * conditional statements and non-render methods.
 *
 * See
 * [Litho Hook Usage](https://www.internalfb.com/intern/staticdocs/litho/docs/mainconcepts/hooks-intro/#rules-for-hooks)
 * for more details.
 */
class LithoFirHookUsageChecker : FirFunctionCallChecker(MppCheckerKind.Platform) {

  override fun check(
      expression: FirFunctionCall,
      context: CheckerContext,
      reporter: DiagnosticReporter
  ) {
    val symbol = expression.calleeReference.symbol ?: return
    if (!symbol.hasAnnotation(LithoNames.Hook, context.session)) return
    if (context.hasConditional()) {
      reporter.reportOn(expression.source, LithoFirErrors.HOOK_CONDITIONAL_MISUSE, context)
    }
    if (context.hasInvalidParent()) {
      reporter.reportOn(expression.source, LithoFirErrors.HOOK_CALLSITE_MISUSE, context)
    }
  }

  private fun CheckerContext.hasConditional(): Boolean {
    return containingElements.any { it is FirWhenExpression || it.isLoop() }
  }

  private fun CheckerContext.hasInvalidParent(): Boolean {
    val container = containingElements.lastIsInstanceOrNull<FirSimpleFunction>() ?: return false
    return !(container.isRenderMethod() || container.hasAnnotation(LithoNames.Hook, session))
  }
}
