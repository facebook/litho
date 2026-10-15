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

package com.facebook.litho.intellij.inspections

import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.DebounceEventLogger
import com.facebook.litho.intellij.services.ComponentGenerateService
import com.facebook.litho.specmodels.internal.RunMode
import com.facebook.litho.specmodels.model.LayoutSpecModel
import com.facebook.litho.specmodels.model.MountSpecModel
import com.facebook.litho.specmodels.model.SpecModel
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Annotator that uses [LayoutSpecModel] and [MountSpecModel] validation to annotate class with
 * error messages. This re-uses Litho compile-time check.
 */
class SpecAnnotator : Annotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    // Reduce warnings check to Class objects only
    val spec: PsiClass =
        when (element) {
          is PsiClass -> {
            element
          }
          is PsiFile -> {
            LithoPluginUtils.getFirstClass(element) { true } ?: return
          }
          else -> {
            return
          }
        }

    // Verify Litho classes only
    val loggingType: String =
        when {
          LithoPluginUtils.isLayoutSpec(spec) -> {
            "layout_spec"
          }
          LithoPluginUtils.isMountSpec(spec) -> {
            "mount_spec"
          }
          LithoPluginUtils.hasLithoSectionSpecAnnotation(spec) -> {
            "section_spec"
          }
          else -> {
            return
          }
        }

    var specModel: SpecModel? = null
    try {
      // Assuming that this Annotator is called sequentially and not in parallel, we don't do extra
      // work by calling update directly
      specModel = ComponentGenerateService.getInstance().getOrCreateSpecModel(spec, false)
    } catch (e: Exception) {
      // Model might contain errors. Proceed to surfacing them.
      DEBUG_LOGGER.debug(e)
    }

    val errors = specModel?.validate(RunMode.normal()).orEmpty()
    if (errors.isNotEmpty()) {
      val data = HashMap<String, String>()
      data[EventLogger.KEY_TYPE] = loggingType
      LOGGER.log(EventLogger.EVENT_ANNOTATOR, data)
      errors.forEach { error -> AnnotatorUtils.addError(holder, error) }
    }
  }

  companion object {
    private val DEBUG_LOGGER: Logger = Logger.getInstance(SpecAnnotator::class.java)
    private val LOGGER: EventLogger = DebounceEventLogger(60_000)
  }
}
