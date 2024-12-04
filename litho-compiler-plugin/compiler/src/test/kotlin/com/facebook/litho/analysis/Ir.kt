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

package com.facebook.litho.analysis

import com.facebook.litho.processor.AbstractTestProcessor
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class TestIrExtension(private val processor: AbstractTestProcessor) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val visitor = TestIrVisitor(processor, pluginContext)
    moduleFragment.acceptVoid(visitor)
  }
}

private class TestIrVisitor(
    private val processor: AbstractTestProcessor,
    private val context: IrPluginContext
) : IrElementVisitorVoid {
  override fun visitFile(declaration: IrFile) {
    super.visitFile(declaration)
    processor.analyse(AbstractTestProcessor.AnalysisData(declaration, context))
  }

  override fun visitElement(element: IrElement) {
    element.acceptChildrenVoid(this)
  }
}
