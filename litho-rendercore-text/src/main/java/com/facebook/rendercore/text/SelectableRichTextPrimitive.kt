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

package com.facebook.rendercore.text

import androidx.core.util.component1
import androidx.core.util.component2
import com.facebook.rendercore.RenderCoreConfig
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.ViewAllocator

/**
 * A Primitive that renders selectable text using [SelectableTextView].
 *
 * This is similar to [RichTextPrimitive] but uses a native Android TextView with
 * [android.widget.TextView.setTextIsSelectable] enabled, allowing users to select and copy text.
 */
fun SelectableRichTextPrimitive(
    id: Long,
    text: CharSequence,
    style: TextStyle,
    usePerformantTruncation: Boolean = RenderCoreConfig.usePerformantTruncation,
    useTruncationCaching: Boolean = RenderCoreConfig.useTruncationCaching,
    hasIntrinsicSize: Boolean = false,
): Primitive {
  return Primitive(
      layoutBehavior =
          RichTextLayoutBehavior(
              text,
              style,
              usePerformantTruncation,
              useTruncationCaching,
              hasIntrinsicSize,
          ),
      mountBehavior =
          MountBehavior<SelectableTextView>(
              id = id,
              contentAllocator =
                  ViewAllocator<SelectableTextView>(
                      canPreallocate = RichTextPrimitiveConfig.canPreallocation,
                      poolSize = RichTextPrimitiveConfig.poolSize,
                  ) { c ->
                    SelectableTextView(c)
                  },
          ) {
            bindWithLayoutData<RichTextLayoutData>(Unit) { content, layoutData ->
              content.mount(layoutData.textLayout)
              onUnbind { content.unmount() }
            }
          },
  )
}
