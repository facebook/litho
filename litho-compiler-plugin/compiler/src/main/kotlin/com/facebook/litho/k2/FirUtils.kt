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
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType

fun FirSimpleFunction.isRenderMethod(): Boolean {
  val receiver = receiverParameter?.typeRef?.coneType?.classId
  return name == LithoNames.render &&
      (receiver == LithoNames.ComponentScope || receiver == LithoNames.PrimitiveComponentScope) &&
      valueParameters.isEmpty()
}

/**
 * The node is a loop. Covers both regular loops (for, while, etc.) and iterator loops (forEach
 * etc.)
 */
fun FirElement.isLoop(): Boolean = this is FirLoop || isLambdaLoop()

/** Is a forEach loop or similar loop where a lambda is executed on the each item of an iterator. */
fun FirElement.isLambdaLoop(): Boolean {
  if (this !is FirFunctionCall) return false
  return calleeReference.name.identifier in iteratorLoopExpressions &&
      argumentList.arguments.any {
        it is FirAnonymousFunctionExpression && it.anonymousFunction.isLambda
      }
}

private val iteratorLoopExpressions: Set<String> =
    setOf(
        "forEach",
        "forEachIndexed",
        "onEach",
        "onEachIndexed",
        "map",
        "mapIndexed",
        "mapIndexedNotNull",
        "mapIndexedNotNullTo",
        "mapIndexedTo",
        "mapKeys",
        "mapKeysTo",
        "mapNotNull",
        "mapNotNullTo",
        "mapTo",
        "mapValues",
        "mapValuesTo",
    )
