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

package com.facebook.litho.k1

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

fun KtNamedFunction.isRenderMethod(): Boolean {
  return name == "render" &&
      valueParameters.isEmpty() &&
      receiverTypeReference?.text.orEmpty().endsWith("ComponentScope")
}

/**
 * The node is a loop. Covers both regular loops (for, while, etc.) and iterator loops (forEach
 * etc.)
 */
fun KtElement.isLoop(): Boolean = this is KtLoopExpression || isLambdaLoop()

/** Is a forEach loop or similar loop where a lambda is executed on the each item of an iterator. */
fun KtElement.isLambdaLoop(): Boolean {
  if (this !is KtCallExpression) return false
  val referenceExpression = firstChild as? KtNameReferenceExpression ?: return false
  return lastChild is KtLambdaArgument &&
      referenceExpression.getReferencedName() in iteratorLoopExpressions
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
