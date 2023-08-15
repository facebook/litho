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

package com.facebook.litho

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.view.View
import androidx.annotation.IntDef
import com.facebook.litho.ComponentHostUtils.maybeSetDrawableState
import com.facebook.litho.ComponentsSystrace.beginSection
import com.facebook.litho.ComponentsSystrace.endSection
import com.facebook.litho.ComponentsSystrace.isTracing
import com.facebook.litho.LithoLayoutData.Companion.getInterStageProps
import com.facebook.litho.LithoLayoutData.Companion.verifyAndGetLithoLayoutData
import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.MountItemsPool.ItemPool
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.RenderUnit

/** This [RenderUnit] encapsulates a Litho output to be mounted using Render Core. */
class MountSpecLithoRenderUnit
private constructor(
    id: Long,
    component: Component,
    commonDynamicProps: SparseArray<DynamicValue<Any?>>?,
    nodeInfo: NodeInfo?,
    flags: Int,
    importantForAccessibility: Int,
    @UpdateState val updateState: Int,
    context: ComponentContext?,
    debugKey: String?
) :
    LithoRenderUnit(
        id,
        component,
        commonDynamicProps,
        nodeInfo,
        flags,
        importantForAccessibility,
        getRenderType(component),
        context,
        debugKey),
    ContentAllocator<Any?> {

  private var isShouldUpdateCachingEnabled = false
  private var isShouldUpdateResultCached = false
  private var isCachedShouldUpdateResult = false

  init {
    addOptionalMountBinders(DelegateBinder.createDelegateBinder(this, mountBinder))
    addAttachBinder(DelegateBinder.createDelegateBinder(this, binderBinder))
  }

  override fun onStartUpdateRenderUnit() {
    isShouldUpdateCachingEnabled = true
  }

  override fun onEndUpdateRenderUnit() {
    isShouldUpdateCachingEnabled = false
    isShouldUpdateResultCached = false
  }

  override fun createRecyclingPool(): ItemPool? {
    return try {
      if (component is SpecGeneratedComponent) {
        component.createRecyclingPool()
      } else {
        null
      }
    } catch (e: Exception) {
      if (componentContext != null) {
        ComponentUtils.handle(componentContext, e)
      }
      null
    }
  }

  override fun createContent(c: Context): Any {
    return component.createMountContent(c)
  }

  override fun getPoolableContentType(): Class<*> {
    return renderContentType
  }

  override fun isRecyclingDisabled(): Boolean {
    return (component is SpecGeneratedComponent && component.isRecyclingDisabled)
  }

  override fun getDescription(): String {
    return component.simpleName
  }

  override fun getContentAllocator(): ContentAllocator<Any?> {
    return this
  }

  override fun getRenderContentType(): Class<*> {
    return component.javaClass
  }

  companion object {

    const val STATE_UNKNOWN = 0
    const val STATE_UPDATED = 1
    const val STATE_DIRTY = 2

    val mountBinder: Binder<MountSpecLithoRenderUnit, Any, Any?> =
        object : Binder<MountSpecLithoRenderUnit, Any, Any?> {
          override fun shouldUpdate(
              current: MountSpecLithoRenderUnit,
              next: MountSpecLithoRenderUnit,
              currentData: Any?,
              nextData: Any?,
          ): Boolean {
            return if (next.component is HostComponent) {
              false
            } else {
              shouldUpdateMountItem(current, next, currentData, nextData)
            }
          }

          override fun bind(
              context: Context,
              content: Any,
              unit: MountSpecLithoRenderUnit,
              layoutData: Any?,
          ): Any? {
            val component = unit.component
            (component as SpecGeneratedComponent).mount(
                getComponentContext(unit),
                content,
                getInterStageProps(layoutData),
            )
            return null
          }

          override fun unbind(
              context: Context,
              content: Any,
              unit: MountSpecLithoRenderUnit,
              layoutData: Any?,
              bindData: Any?
          ) {
            (unit.component as SpecGeneratedComponent).unmount(
                getComponentContext(unit),
                content,
                getInterStageProps(layoutData),
            )
          }
        }

    val binderBinder: Binder<MountSpecLithoRenderUnit, Any, Any?> =
        object : Binder<MountSpecLithoRenderUnit, Any, Any?> {
          override fun shouldUpdate(
              current: MountSpecLithoRenderUnit,
              next: MountSpecLithoRenderUnit,
              c: Any?,
              n: Any?,
          ): Boolean {
            return true
          }

          override fun bind(
              context: Context,
              content: Any,
              unit: MountSpecLithoRenderUnit,
              layoutData: Any?,
          ): Any? {
            if (content is Drawable) {
              if (content.callback is View) {
                val view = content.callback as View?
                maybeSetDrawableState(view!!, content, unit.flags)
              }
            }
            (unit.component as SpecGeneratedComponent).bind(
                getComponentContext(unit),
                content,
                getInterStageProps(layoutData),
            )
            return null
          }

          override fun unbind(
              context: Context,
              content: Any,
              unit: MountSpecLithoRenderUnit,
              layoutData: Any?,
              bindData: Any?,
          ) {
            (unit.component as SpecGeneratedComponent).unbind(
                getComponentContext(unit),
                content,
                getInterStageProps(layoutData),
            )
          }
        }

    @JvmStatic
    fun create(
        id: Long,
        component: Component,
        commonDynamicProps: SparseArray<DynamicValue<Any?>>?,
        context: ComponentContext?,
        nodeInfo: NodeInfo?,
        flags: Int,
        importantForAccessibility: Int,
        @UpdateState updateState: Int,
        debugKey: String?
    ): MountSpecLithoRenderUnit {
      return MountSpecLithoRenderUnit(
          id,
          component,
          commonDynamicProps,
          nodeInfo,
          flags,
          importantForAccessibility,
          updateState,
          context,
          debugKey)
    }

    @JvmStatic
    fun shouldUpdateMountItem(
        current: MountSpecLithoRenderUnit,
        next: MountSpecLithoRenderUnit,
        currentData: Any?,
        nextData: Any?,
    ): Boolean {
      if (current.isShouldUpdateCachingEnabled && current.isShouldUpdateResultCached) {
        return current.isCachedShouldUpdateResult
      }
      val currentLithoData = verifyAndGetLithoLayoutData(currentData)
      val nextLithoData = verifyAndGetLithoLayoutData(nextData)
      val nextContext = getComponentContext(next)
      val previousIdFromNextOutput = nextLithoData.previousLayoutStateId
      val currentContext = getComponentContext(current)
      val idFromCurrentOutput = currentLithoData.currentLayoutStateId
      val updateValueFromLayoutOutput = previousIdFromNextOutput == idFromCurrentOutput
      val result =
          shouldUpdateMountItem(
              next,
              nextData as LithoLayoutData?,
              nextContext,
              current,
              currentData as LithoLayoutData?,
              currentContext,
              updateValueFromLayoutOutput,
          )
      if (current.isShouldUpdateCachingEnabled && !current.isShouldUpdateResultCached) {
        current.isCachedShouldUpdateResult = result
        current.isShouldUpdateResultCached = true
      }
      return result
    }

    private fun getRenderType(component: Component?): RenderType {
      requireNotNull(component) { "Null output used for LithoRenderUnit." }
      return if (component.mountType == Component.MountType.DRAWABLE) {
        RenderType.DRAWABLE
      } else {
        RenderType.VIEW
      }
    }

    private fun shouldUpdateMountItem(
        nextRenderUnit: MountSpecLithoRenderUnit,
        nextLayoutData: LithoLayoutData?,
        nextContext: ComponentContext?,
        currentRenderUnit: MountSpecLithoRenderUnit,
        currentLayoutData: LithoLayoutData?,
        currentContext: ComponentContext?,
        useUpdateValueFromLayoutOutput: Boolean,
    ): Boolean {
      @UpdateState val updateState = nextRenderUnit.updateState
      val currentComponent = currentRenderUnit.component
      val nextComponent = nextRenderUnit.component

      // If the two components have different sizes and the mounted content depends on the size we
      // just return true immediately.
      if (nextComponent is SpecGeneratedComponent &&
          nextComponent.isMountSizeDependent &&
          !sameSize(
              checkNotNull(nextLayoutData),
              checkNotNull(currentLayoutData),
          )) {
        return true
      }
      if (useUpdateValueFromLayoutOutput) {
        if (updateState == STATE_UPDATED) {
          return false
        } else if (updateState == STATE_DIRTY) {
          return true
        }
      }
      return shouldUpdate(currentComponent, currentContext, nextComponent, nextContext)
    }

    private fun shouldUpdate(
        currentComponent: Component,
        currentScopedContext: ComponentContext?,
        nextComponent: Component,
        nextScopedContext: ComponentContext?,
    ): Boolean {
      val isTracing = isTracing
      return try {
        if (isTracing) {
          beginSection("MountState.shouldUpdate")
        }
        currentComponent.shouldComponentUpdate(
            currentScopedContext,
            currentComponent,
            nextScopedContext,
            nextComponent,
        )
      } catch (e: Exception) {
        ComponentUtils.handle(nextScopedContext, e)
        true
      } finally {
        if (isTracing) {
          endSection()
        }
      }
    }

    private fun sameSize(next: LithoLayoutData, current: LithoLayoutData): Boolean {
      return next.width == current.width && next.height == current.height
    }

    @JvmStatic
    @UpdateState
    fun getUpdateState(node: RenderTreeNode): Int {
      return (node.renderUnit as MountSpecLithoRenderUnit).updateState
    }
  }

  @IntDef(STATE_UPDATED, STATE_UNKNOWN, STATE_DIRTY)
  @Retention(AnnotationRetention.SOURCE)
  annotation class UpdateState
}
