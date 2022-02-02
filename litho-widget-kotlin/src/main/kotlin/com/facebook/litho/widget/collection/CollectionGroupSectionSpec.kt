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

package com.facebook.litho.widget.collection

import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.ChangesInfo
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.annotations.GroupSectionSpec
import com.facebook.litho.sections.annotations.OnCreateChildren
import com.facebook.litho.sections.annotations.OnDataBound
import com.facebook.litho.sections.annotations.OnDataRendered
import com.facebook.litho.sections.annotations.OnRefresh
import com.facebook.litho.sections.annotations.OnViewportChanged

@GroupSectionSpec
object CollectionGroupSectionSpec {

  @JvmStatic
  @OnCreateChildren
  fun onCreateChildren(c: SectionContext?, @Prop childrenBuilder: Children.Builder): Children =
      childrenBuilder.build()

  @JvmStatic
  @OnDataBound
  fun onDataBound(
      c: SectionContext,
      @Prop(optional = true) onDataBound: ((ComponentContext) -> Unit)?
  ) {
    onDataBound?.invoke(c)
  }

  @JvmStatic
  @OnViewportChanged
  fun onViewportChanged(
      c: SectionContext,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      totalCount: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int,
      @Prop(optional = true)
      onViewportChanged: ((ComponentContext, Int, Int, Int, Int, Int) -> Unit)?
  ) {
    onViewportChanged?.invoke(
        c,
        firstVisibleIndex,
        lastVisibleIndex,
        totalCount,
        firstFullyVisibleIndex,
        lastFullyVisibleIndex)
  }

  @JvmStatic
  @OnRefresh
  fun onRefresh(c: SectionContext, @Prop(optional = true) onPullToRefresh: (() -> Unit)?) {
    onPullToRefresh?.invoke()
  }

  @JvmStatic
  @OnDataRendered
  fun onDataRendered(
      c: SectionContext,
      isDataChanged: Boolean,
      isMounted: Boolean,
      monoTimestampMs: Long,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      changesInfo: ChangesInfo,
      globalOffset: Int,
      @Prop(optional = true)
      onDataRendered:
          ((ComponentContext, Boolean, Boolean, Long, Int, Int, ChangesInfo, Int) -> Unit)?
  ) {
    onDataRendered?.invoke(
        c,
        isDataChanged,
        isMounted,
        monoTimestampMs,
        firstVisibleIndex,
        lastVisibleIndex,
        changesInfo,
        globalOffset)
  }
}
