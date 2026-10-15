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

package com.facebook.litho.processor

import com.facebook.litho.common.LithoNames
import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

@OptIn(DeprecatedForRemovalCompilerApi::class)
class HookIdentityCallProcessor : AbstractTestProcessor() {

  val callSiteIds: MutableList<Int> = mutableListOf()

  override fun StringBuilder.analyse(data: AnalysisData) {
    data.file.acceptVoid(
        object : IrVisitorVoid() {
          override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
          }

          override fun visitCall(expression: IrCall) {
            if (expression.symbol.owner.name == LithoNames.enterHook) {
              val callSiteId = (expression.getValueArgument(0) as? IrConst)?.value as? Int
              if (callSiteId != null) {
                callSiteIds.add(callSiteId)
              }
              appendLine(
                  "enterHook arguments=${expression.symbol.owner.valueParameters.size}",
              )
            }
            super.visitCall(expression)
          }
        },
    )
  }
}
