/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.codelab

import android.os.SystemClock
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateTreeProp
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.sections.ChangesInfo
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.LoadingEvent
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.SectionLifecycle
import com.facebook.litho.sections.annotations.GroupSectionSpec
import com.facebook.litho.sections.annotations.OnCreateChildren
import com.facebook.litho.sections.annotations.OnDataBound
import com.facebook.litho.sections.annotations.OnDataRendered
import com.facebook.litho.sections.annotations.OnViewportChanged
import com.facebook.litho.sections.annotations.OnRefresh
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.widget.Card
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaEdge

@Suppress("MagicNumber")
@GroupSectionSpec
object LifecycleGroupSectionSpec {

  @OnCreateInitialState
  fun onCreateInitialState(
      c: SectionContext,
      startTime: StateValue<Long>,
      @Prop lifecycleListener: LifecycleListener
  ) {
    val timestamp = SystemClock.uptimeMillis()
    startTime.set(timestamp)
    dispatchLifecycleEvent(LifecycleEventType.ON_CREATE_INITIAL_STATE, lifecycleListener, timestamp)
  }

  @OnCreateTreeProp
  fun onCreateTreeProp(
      c: SectionContext,
      @Prop lifecycleListener: LifecycleListener,
      @State startTime: Long
  ): DummyTreeProp {
    dispatchLifecycleEvent(LifecycleEventType.ON_CREATE_TREE_PROP, lifecycleListener, startTime)
    return DummyTreeProp
  }

  @OnCreateChildren
  fun onCreateChildren(
      c: SectionContext,
      @Prop zodiacs: List<Zodiac>,
      @Prop lifecycleListener: LifecycleListener,
      @State startTime: Long,
      @State scramble: Boolean
  ): Children {
    val children = Children.create()
        .child(
            DataDiffSection.create<Zodiac>(c)
                .data(if (scramble) zodiacs.toMutableList().shuffled() else zodiacs)
                .renderEventHandler(LifecycleGroupSection.onRender(c))
        )
        .build()
    dispatchLifecycleEvent(LifecycleEventType.ON_CREATE_CHILDREN, lifecycleListener, startTime)
    return children
  }

  @OnEvent(RenderEvent::class)
  fun onRender(c: SectionContext, @FromEvent model: Zodiac): RenderInfo {
    return ComponentRenderInfo.create()
        .component(
            Card.create(c)
                .content(
                    Text.create(c)
                        .text(model.animal)
                        .textSizeSp(20f)
                        .paddingDip(YogaEdge.ALL, 8f)))
        .build()
  }

  @OnDataBound
  fun onDataBound(
      c: SectionContext,
      @Prop lifecycleListener: LifecycleListener,
      @State startTime: Long
  ) {
    dispatchLifecycleEvent(LifecycleEventType.ON_DATA_BOUND, lifecycleListener, startTime)
  }

  @OnDataRendered
  fun onDataRendered(
      c: SectionContext,
      isDataChanged: Boolean,
      isMounted: Boolean,
      uptimeMillis: Long,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      changesInfo: ChangesInfo,
      @Prop lifecycleListener: LifecycleListener,
      @State startTime: Long
  ) {
    dispatchLifecycleEvent(LifecycleEventType.ON_DATA_RENDERED, lifecycleListener, startTime)
  }

  @OnViewportChanged
  fun onViewportChanged(
      c: SectionContext,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      totalCount: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int,
      @Prop lifecycleListener: LifecycleListener,
      @State startTime: Long
  ) {
    dispatchLifecycleEvent(LifecycleEventType.ON_VIEWPORT_CHANGED, lifecycleListener, startTime)
  }

  @OnRefresh
  fun onRefresh(
      c: SectionContext,
      @Prop lifecycleListener: LifecycleListener,
      @State startTime: Long
  ) {
    dispatchLifecycleEvent(LifecycleEventType.ON_REFRESH, lifecycleListener, startTime)
    LifecycleGroupSection.updateScramble(c)
    SectionLifecycle.dispatchLoadingEvent(c, false, LoadingEvent.LoadingState.SUCCEEDED, null)
  }

  @OnUpdateState
  fun updateScramble(scramble: StateValue<Boolean>) {
    scramble.set(scramble.get()?.not())
  }

  private fun dispatchLifecycleEvent(
      type: LifecycleEventType,
      listener: LifecycleListener,
      startTime: Long
  ) {
    val endTime = SystemClock.uptimeMillis() - startTime
    listener.onLifecycleMethodCalled(type, endTime)
  }
}
