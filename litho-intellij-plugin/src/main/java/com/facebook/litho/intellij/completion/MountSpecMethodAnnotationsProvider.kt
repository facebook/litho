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

package com.facebook.litho.intellij.completion

import com.facebook.litho.annotations.OnBind
import com.facebook.litho.annotations.OnBoundsDefined
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnCreateMountContentPool
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnPrepare
import com.facebook.litho.annotations.OnTrigger
import com.facebook.litho.annotations.OnUnbind
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.OnUpdateStateWithTransition
import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.specmodels.processor.MountSpecModelFactory
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext

/**
 * Provider emphasized completion results for valid [com.facebook.litho.annotations.MountSpec]
 * method annotations.
 *
 * @see MountSpecModelFactory
 */
class MountSpecMethodAnnotationsProvider : CompletionProvider<CompletionParameters>() {

  protected override fun addCompletions(
      parameters: CompletionParameters,
      context: ProcessingContext,
      result: CompletionResultSet,
  ) {
    MethodCompletionContributor.addMethodCompletions(
        parameters.position,
        LithoPluginUtils::isMountSpec,
        ANNOTATION_QUALIFIED_NAMES,
        result,
    )
  }

  companion object {
    @JvmField
    internal val INSTANCE: CompletionProvider<CompletionParameters> =
        MountSpecMethodAnnotationsProvider()

    @JvmField
    internal val ANNOTATION_QUALIFIED_NAMES: Set<String> = buildSet {
      for (permittedMethod in MountSpecModelFactory.DELEGATE_METHOD_ANNOTATIONS) {
        add(permittedMethod.typeName)
      }
      add(OnTrigger::class.java.typeName)
      add(OnEvent::class.java.typeName)
      add(OnCreateMountContent::class.java.typeName)
      add(OnPrepare::class.java.typeName)
      add(OnBoundsDefined::class.java.typeName)
      add(OnUnbind::class.java.typeName)
      add(OnBind::class.java.typeName)
      add(OnUnmount::class.java.typeName)
      add(OnMeasure::class.java.typeName)
      add(OnMount::class.java.typeName)
      add(OnUpdateState::class.java.typeName)
      add(OnUpdateStateWithTransition::class.java.typeName)
      add(OnCreateMountContentPool::class.java.typeName)
    }
  }
}
