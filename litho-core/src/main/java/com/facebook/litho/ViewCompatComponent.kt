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
import android.view.View
import android.view.ViewGroup
import com.facebook.litho.viewcompat.ViewBinder
import com.facebook.litho.viewcompat.ViewCreator

/**
 * A component that can wrap a view using a [ViewBinder] class to bind the view and a [ViewCreator]
 * to create the mount contents. This component will have a different recycle pool per [ViewCreator]
 * instance.
 */
@Suppress("UNCHECKED_CAST")
@Deprecated(
    """ViewCompatComponent is not efficient as it will do measurement of views twice.
      Recommended way now is to use either ViewRenderInfo (which utilizes same interfaces as this
      class: ViewCreator and ViewBinder) if the view is used with sections API or create a custom
      MountSpec.""")
class ViewCompatComponent<V : View>
private constructor(private val viewCreator: ViewCreator<*>, componentName: String) :
    SpecGeneratedComponent(
        System.identityHashCode(viewCreator), "ViewCompatComponent_$componentName") {
  private var viewBinder: ViewBinder<V>? = null
  private var poolSize = UNSPECIFIED_POOL_SIZE

  fun requireViewBinder(): ViewBinder<V> {
    return checkNotNull(viewBinder)
  }

  fun create(componentContext: ComponentContext): Builder<V> {
    return Builder(componentContext, this)
  }

  override fun isEquivalentProps(other: Component?, shouldCompareCommonProps: Boolean): Boolean {
    return this === other
  }

  override fun canMeasure(): Boolean {
    return true
  }

  override fun onMeasure(
      c: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      interStagePropsContainer: InterStagePropsContainer?
  ) {
    val toMeasure: V = onCreateMountContent(c.androidContext)
    val layoutParams = ViewGroup.LayoutParams(size.width, size.height)
    toMeasure.layoutParams = layoutParams
    requireViewBinder().bind(toMeasure)
    if (toMeasure.visibility == View.GONE) {
      // No need to measure the view if binding it caused its visibility to become GONE.
      size.width = 0
      size.height = 0
    } else {
      toMeasure.measure(widthSpec, heightSpec)
      size.width = toMeasure.measuredWidth
      size.height = toMeasure.measuredHeight
    }
    requireViewBinder().unbind(toMeasure)
  }

  override fun onPrepare(c: ComponentContext) {
    requireViewBinder().prepare()
  }

  override fun onBind(
      c: ComponentContext?,
      mountedContent: Any,
      interStagePropsContainer: InterStagePropsContainer?
  ) {
    requireViewBinder().bind(mountedContent as V)
  }

  override fun onUnbind(
      c: ComponentContext?,
      mountedContent: Any,
      interStagePropsContainer: InterStagePropsContainer?
  ) {
    requireViewBinder().unbind(mountedContent as V)
  }

  override fun getMountType(): MountType {
    return MountType.VIEW
  }

  public override fun onCreateMountContent(c: Context): V {
    return viewCreator.createView(c, null) as V
  }

  class Builder<V : View>(
      context: ComponentContext,
      private var viewCompatComponent: ViewCompatComponent<V>
  ) : Component.Builder<Builder<V>?>(context, 0, 0, viewCompatComponent) {
    fun viewBinder(viewBinder: ViewBinder<V>): Builder<V> {
      viewCompatComponent.viewBinder = viewBinder
      return this
    }

    fun contentPoolSize(size: Int): Builder<V> {
      viewCompatComponent.poolSize = size
      return this
    }

    override fun setComponent(component: Component) {
      viewCompatComponent = component as ViewCompatComponent<V>
    }

    override fun getThis(): Builder<V> {
      return this
    }

    override fun build(): ViewCompatComponent<V> {
      checkNotNull(viewCompatComponent.viewBinder) {
        "To create a ViewCompatComponent you must provide a ViewBinder."
      }
      return viewCompatComponent
    }
  }

  override fun poolSize(): Int {
    return if (poolSize == UNSPECIFIED_POOL_SIZE) super.poolSize() else poolSize
  }

  companion object {
    private const val UNSPECIFIED_POOL_SIZE = -1

    @JvmStatic
    operator fun <V : View> get(
        viewCreator: ViewCreator<V>,
        componentName: String
    ): ViewCompatComponent<V> {
      return ViewCompatComponent(viewCreator, componentName)
    }
  }
}
