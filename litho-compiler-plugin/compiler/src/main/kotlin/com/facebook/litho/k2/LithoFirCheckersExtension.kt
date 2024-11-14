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

package com.facebook.litho.k2

import com.facebook.litho.common.LithoNames
import com.facebook.litho.k2.diagnostics.LithoFirHookUsageChecker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate

/**
 * Responsible for supplying additional checkers that will be called when the compiler analyses
 * code. It can also report additional errors or warnings that can be visualized by the IDE.
 *
 * There are different kinds of checkers available, and can be as fine-grained as possible.
 *
 * @see [FirAdditionalCheckersExtension]
 * @see [LithoFirHookUsageChecker]
 */
class LithoFirCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {

  override val expressionCheckers: ExpressionCheckers =
      object : ExpressionCheckers() {
        override val functionCallCheckers = setOf(LithoFirHookUsageChecker())
      }

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(predicate)
  }

  /**
   * Defined a set of predicates that will be used to filter the declarations that should trigger
   * the checkers defined in this extension.
   */
  private val predicate =
      DeclarationPredicate.create {
        metaAnnotated(LithoNames.Unconditional.asSingleFqName(), includeItself = false)
      }
}
