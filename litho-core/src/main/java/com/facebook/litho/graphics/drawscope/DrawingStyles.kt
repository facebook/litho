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

package com.facebook.litho.graphics.drawscope

import androidx.core.graphics.withSave
import com.facebook.litho.ComponentHost
import com.facebook.litho.Style
import com.facebook.litho.binders.ModelWithContext
import com.facebook.litho.binders.onBindHostView
import com.facebook.litho.uiStateReadRecords
import com.facebook.rendercore.graphics.drawscope.CanvasDrawScope
import com.facebook.rendercore.graphics.drawscope.DrawScope

fun Style.drawBehind(block: DrawScope.() -> Unit): Style {
  return this +
      onBindHostView(Unit) { content ->
        val model = binderModel as ModelWithContext
        val uiStateReadsRecords = model.scopedContext.uiStateReadRecords
        // Retrieve the binderId during binder execution so that we can use it later in Draw
        val binderId = binderId

        content as ComponentHost
        content.drawBehind = { canvas ->
          uiStateReadsRecords.recordOnDraw(binderId.renderUnitId) {
            canvas.withSave {
              CanvasDrawScope(
                      // canvas = canvas,
                      // context = draw context,
                      // size = Size(w, h),
                      )
                  .block()
            }
          }
        }
        onUnbind {
          content.drawBehind = null
          uiStateReadsRecords.removeDrawScope(binderId.renderUnitId)
        }
      }
}
