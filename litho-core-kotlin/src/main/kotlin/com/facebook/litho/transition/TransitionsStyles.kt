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

@file:Suppress("MatchingDeclarationName")

package com.facebook.litho.transition

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.CommonProps
import com.facebook.litho.ComponentContext
import com.facebook.litho.Style
import com.facebook.litho.StyleItem
import com.facebook.litho.StyleItemField
import com.facebook.litho.Transition
import com.facebook.litho.Transition.TransitionKeyType

@DataClassGenerate
data class TransitionKeyStyleItemValue(
    val transitionKey: String?,
    val transitionKeyType: TransitionKeyType
)

enum class TransitionKeyFields : StyleItemField {
  TRANSITION_KEY,
}

@PublishedApi
@DataClassGenerate
internal data class TransitionKeyStyleItem(
    val context: ComponentContext,
    val transitionKey: String?,
    val transitionKeyType: TransitionKeyType
) : StyleItem<TransitionKeyStyleItemValue> {
  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    commonProps.transitionKey(transitionKey, this.context.globalKey)
    commonProps.transitionKeyType(transitionKeyType)
  }

  override val value: TransitionKeyStyleItemValue =
      TransitionKeyStyleItemValue(transitionKey, transitionKeyType)
  override val field: TransitionKeyFields = TransitionKeyFields.TRANSITION_KEY
}

/**
 * Sets transition key and [Transition.TransitionKeyType] on the View this Component mounts to.
 * Setting this property will cause the Component to be represented as a View at mount time if it
 * wasn't going to already.
 */
inline fun Style.transitionKey(
    context: ComponentContext,
    transitionKey: String?,
    transitionKeyType: TransitionKeyType = TransitionKeyType.LOCAL
): Style = this + TransitionKeyStyleItem(context, transitionKey, transitionKeyType)
