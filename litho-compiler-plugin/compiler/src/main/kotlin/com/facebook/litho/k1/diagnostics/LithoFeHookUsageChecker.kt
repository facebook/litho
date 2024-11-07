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

package com.facebook.litho.k1.diagnostics

import com.facebook.litho.common.LithoNames
import com.facebook.litho.k1.isLoop
import com.facebook.litho.k1.isRenderMethod
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

/**
 * Checks that the rules surrounding hooks usage are not violated, specifically around usage in
 * conditional statements and non-render methods.
 *
 * See
 * [Litho Hook Usage](https://www.internalfb.com/intern/staticdocs/litho/docs/mainconcepts/hooks-intro/#rules-for-hooks)
 * for more details.
 */
class LithoFeHookUsageChecker : CallChecker {

  override fun check(
      resolvedCall: ResolvedCall<*>,
      reportOn: PsiElement,
      context: CallCheckerContext
  ) {
    val descriptor = resolvedCall.resultingDescriptor
    if (!descriptor.annotations.hasAnnotation(LithoNames.Hook.asSingleFqName())) return
    val callElement = resolvedCall.call.callElement
    if (callElement.hasConditional()) {
      context.trace.report(LithoFeErrors.HOOK_CONDITIONAL_MISUSE.on(reportOn))
    }
    if (callElement.hasInvalidParent()) {
      context.trace.report(LithoFeErrors.HOOK_CALLSITE_MISUSE.on(reportOn))
    }
  }

  private fun KtElement.hasConditional(): Boolean {
    return parents.filterIsInstance<KtElement>().any {
      it is KtIfExpression || it is KtWhenExpression || it.isLoop()
    }
  }

  private fun KtElement.hasInvalidParent(): Boolean {
    val containingDeclaration = getNonStrictParentOfType<KtNamedFunction>() ?: return false
    return !containingDeclaration.isRenderMethod() &&
        !containingDeclaration.annotationEntries.any {
          it.shortName == LithoNames.Hook.shortClassName
        }
  }
}
