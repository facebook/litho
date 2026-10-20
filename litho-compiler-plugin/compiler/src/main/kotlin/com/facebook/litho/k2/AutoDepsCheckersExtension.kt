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

import com.facebook.litho.k2.diagnostics.AutoDepsClassChecker
import com.facebook.litho.k2.diagnostics.AutoDepsFunctionChecker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

/**
 * Validates the shape of `@AutoDeps` declarations.
 *
 * These are declaration checks, so they belong in FIR: it is the only phase that produces
 * diagnostics the IDE positions. The capture-dependent errors have no declaration to attach to and
 * are reported from the transform instead.
 *
 * @see [AutoDepsFunctionChecker]
 * @see [AutoDepsClassChecker]
 */
class AutoDepsCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {

  override val declarationCheckers: DeclarationCheckers =
      object : DeclarationCheckers() {
        override val simpleFunctionCheckers = setOf(AutoDepsFunctionChecker())
        override val regularClassCheckers = setOf(AutoDepsClassChecker())
      }
}
