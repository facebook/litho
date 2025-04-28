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

package com.facebook.litho.widget

import android.view.View
import androidx.annotation.IntDef
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import com.facebook.litho.AOSPLifecycleOwnerProvider
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.ComponentTree.MeasureListener
import com.facebook.litho.ComponentTree.NewLayoutStateReadyListener
import com.facebook.litho.LithoVisibilityEventsController
import com.facebook.litho.LithoVisibilityEventsController.LithoVisibilityState
import com.facebook.litho.LithoVisibilityEventsControllerDelegate
import com.facebook.litho.LithoVisibilityEventsListener
import com.facebook.litho.Size
import com.facebook.litho.StateUpdaterDelegator
import com.facebook.litho.ThreadUtils
import com.facebook.litho.TreePropContainer
import com.facebook.litho.TreeState
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.PoolScope
import com.facebook.rendercore.RunnableHandler
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.concurrent.GuardedBy
import javax.annotation.concurrent.ThreadSafe

/**
 * A class used to store the data backing a [RecyclerBinder]. For each item the ComponentTreeHolder
 * keeps the [RenderInfo] which contains the original [Component] and either the [ComponentTree] or
 * the [TreeState] depending upon whether the item is within the current working range or not.
 */
@ThreadSafe
open class ComponentTreeHolder @VisibleForTesting internal constructor(builder: Builder) {

  @IntDef(RENDER_UNINITIALIZED, RENDER_ADDED, RENDER_DRAWN) annotation class RenderState

  fun interface ComponentTreeMeasureListenerFactory {
    fun create(holder: ComponentTreeHolder): MeasureListener?
  }

  val id: Int = idGenerator.getAndIncrement()

  private val lithoVisibilityEventsController = builder.lithoVisibilityEventsController
  private val componentsConfiguration = builder.componentsConfiguration
  private var visibilityEventsController: ComponentTreeHolderVisibilityEventsController? = null
  private var stateUpdaterDelegator = builder.stateUpdaterDelegator

  private var poolScope = builder.poolScope

  private val componentTreeMeasureListenerFactory = builder.componentTreeMeasureListenerFactory
  private val _renderState = AtomicInteger(RENDER_UNINITIALIZED)

  @GuardedBy("this") private val acquireTreeStateOnRelease = builder.acquireTreeStateOnRelease
  @GuardedBy("this") private var layoutHandler = builder.layoutHandler
  @GuardedBy("this") private var pendingNewLayoutListener: NewLayoutStateReadyListener? = null
  @GuardedBy("this") private var lastRequestedWidthSpec = UNINITIALIZED
  @GuardedBy("this") private var lastRequestedHeightSpec = UNINITIALIZED

  @GuardedBy("this") private var _isTreeValid = false
  @GuardedBy("this") private var _componentTree: ComponentTree? = null

  @get:Synchronized @set:Synchronized @GuardedBy("this") var measuredHeight: Int = 0

  /** @return whether this ComponentTreeHolder has been inserted into the adapter yet. */
  @get:Synchronized
  /** Set whether this ComponentTreeHolder has been inserted into the adapter. */
  @set:Synchronized
  @GuardedBy("this")
  var isInserted: Boolean = true

  @get:Synchronized
  open val isTreeValid: Boolean
    get() = _isTreeValid

  @get:Synchronized
  open val componentTree: ComponentTree?
    get() = _componentTree

  @get:VisibleForTesting
  @GuardedBy("this")
  var treeState: TreeState? = null
    private set

  @get:Synchronized
  @set:Synchronized
  open var renderInfo: RenderInfo = requireNotNull(builder.renderInfo)
    set(renderInfo) {
      invalidateTree()
      field = renderInfo
    }

  var renderState: Int
    get() = _renderState.get()
    set(renderState) = _renderState.set(renderState)

  class Builder
  internal constructor(internal val componentsConfiguration: ComponentsConfiguration) {
    internal var renderInfo: RenderInfo? = null
    internal var layoutHandler: RunnableHandler? = null
    internal var componentTreeMeasureListenerFactory: ComponentTreeMeasureListenerFactory? = null
    internal var lithoVisibilityEventsController: LithoVisibilityEventsController? = null
    internal var stateUpdaterDelegator: StateUpdaterDelegator? = null
    internal var poolScope: PoolScope = PoolScope.None
    internal var acquireTreeStateOnRelease: Boolean = false

    fun renderInfo(renderInfo: RenderInfo?): Builder {
      this.renderInfo = renderInfo ?: ComponentRenderInfo.createEmpty()
      return this
    }

    fun layoutHandler(layoutHandler: RunnableHandler?): Builder {
      this.layoutHandler = layoutHandler
      return this
    }

    fun componentTreeMeasureListenerFactory(
        componentTreeMeasureListenerFactory: ComponentTreeMeasureListenerFactory?
    ): Builder {
      this.componentTreeMeasureListenerFactory = componentTreeMeasureListenerFactory
      return this
    }

    fun lithoVisibilityEventsController(
        lithoVisibilityEventsController: LithoVisibilityEventsController?
    ): Builder {
      this.lithoVisibilityEventsController = lithoVisibilityEventsController
      return this
    }

    fun acquireTreeStateOnRelease(shouldAcquireTreeStateOnRelease: Boolean): Builder {
      acquireTreeStateOnRelease = shouldAcquireTreeStateOnRelease
      return this
    }

    fun stateUpdaterDelegator(stateUpdaterDelegator: StateUpdaterDelegator?): Builder {
      this.stateUpdaterDelegator = stateUpdaterDelegator ?: StateUpdaterDelegator()
      return this
    }

    fun poolScope(poolScope: PoolScope): Builder {
      this.poolScope = poolScope
      return this
    }

    fun build(): ComponentTreeHolder {
      ensureMandatoryParams()
      return ComponentTreeHolder(this)
    }

    private fun ensureMandatoryParams() {
      requireNotNull(renderInfo) { "A RenderInfo must be specified to create ComponentTreeHolder" }
    }
  }

  @UiThread
  @Synchronized
  open fun acquireStateAndReleaseTree() {
    if (shouldAcquireTreeStateOnRelease()) {
      acquireTreeState()
    }
    releaseTree()
  }

  @Synchronized
  open fun invalidateTree() {
    _isTreeValid = false
  }

  @Synchronized
  fun setNewLayoutReadyListener(listener: NewLayoutStateReadyListener?) {
    val componentTree = _componentTree
    if (componentTree != null) {
      componentTree.newLayoutStateReadyListener = listener
    } else {
      pendingNewLayoutListener = listener
    }
  }

  @Synchronized
  fun setPoolScope(poolScope: PoolScope) {
    this.poolScope = poolScope
  }

  open fun computeLayoutSync(
      context: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      size: Size?
  ) {
    val componentTree: ComponentTree
    val component: Component
    val treePropContainer: TreePropContainer?

    synchronized(this) {
      if (renderInfo.rendersView()) {
        // Nothing to do for views.
        return
      }
      lastRequestedWidthSpec = widthSpec
      lastRequestedHeightSpec = heightSpec

      componentTree = ensureComponentTree(context)
      component = renderInfo.component
      treePropContainer = (renderInfo as? TreePropsWrappedRenderInfo)?.treePropContainer
    }

    componentTree.setRootAndSizeSpecSync(component, widthSpec, heightSpec, size, treePropContainer)

    synchronized(this) {
      if (componentTree === this._componentTree && component === renderInfo.component) {
        _isTreeValid = true
        if (size != null) {
          measuredHeight = size.height
        }
      }
    }
  }

  @JvmOverloads
  open fun computeLayoutAsync(
      context: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      measureListener: MeasureListener? = null
  ) {
    val componentTree: ComponentTree
    val component: Component
    val treePropContainer: TreePropContainer?

    synchronized(this) {
      if (renderInfo.rendersView()) {
        // Nothing to do for views.
        return
      }
      lastRequestedWidthSpec = widthSpec
      lastRequestedHeightSpec = heightSpec

      componentTree = ensureComponentTree(context)
      component = renderInfo.component
      treePropContainer = (renderInfo as? TreePropsWrappedRenderInfo)?.treePropContainer
    }

    if (measureListener != null) {
      componentTree.addMeasureListener(measureListener)
    }

    componentTree.setRootAndSizeSpecAsync(component, widthSpec, heightSpec, treePropContainer)

    synchronized(this) {
      if (this._componentTree === componentTree && component === renderInfo.component) {
        _isTreeValid = true
      }
    }
  }

  @Synchronized
  fun addMeasureListener(measureListener: MeasureListener?) {
    _componentTree?.addMeasureListener(measureListener)
  }

  @Synchronized
  fun clearMeasureListener(measureListener: MeasureListener?) {
    _componentTree?.clearMeasureListener(measureListener)
  }

  @Synchronized
  open fun isTreeValidForSizeSpecs(widthSpec: Int, heightSpec: Int): Boolean =
      _isTreeValid && lastRequestedWidthSpec == widthSpec && lastRequestedHeightSpec == heightSpec

  @Synchronized
  open fun updateLayoutHandler(layoutHandler: RunnableHandler?) {
    this.layoutHandler = layoutHandler
    _componentTree?.updateLayoutThreadHandler(layoutHandler)
  }

  @Synchronized
  open fun checkWorkingRangeAndDispatch(
      position: Int,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int
  ) {
    _componentTree?.checkWorkingRangeAndDispatch(
        position,
        firstVisibleIndex,
        lastVisibleIndex,
        firstFullyVisibleIndex,
        lastFullyVisibleIndex)
  }

  @Synchronized
  fun hasCompletedLatestLayout(): Boolean =
      renderInfo.rendersView() ||
          (_componentTree?.hasCompatibleLayout(lastRequestedWidthSpec, lastRequestedHeightSpec) ==
              true)

  @OptIn(ExperimentalLithoApi::class)
  @GuardedBy("this")
  private fun ensureComponentTree(context: ComponentContext): ComponentTree {
    if (_componentTree == null) {
      if (lithoVisibilityEventsController != null) {
        visibilityEventsController = ComponentTreeHolderVisibilityEventsController()
      }

      val treeComponentsConfigurationBuilder =
          ComponentsConfiguration.create(componentsConfiguration)

      renderInfo.logTag?.let(treeComponentsConfigurationBuilder::logTag)
      renderInfo.componentsLogger?.let(treeComponentsConfigurationBuilder::componentsLogger)

      val treeComponentConfiguration = treeComponentsConfigurationBuilder.build()

      val builder =
          ComponentTree.create(context, renderInfo.component, visibilityEventsController)
              .componentsConfiguration(treeComponentConfiguration)
              .layoutThreadHandler(layoutHandler)
              .treeState(treeState)
              .measureListener(componentTreeMeasureListenerFactory?.create(this))

      val stateUpdaterDelegator =
          stateUpdaterDelegator ?: StateUpdaterDelegator().also { stateUpdaterDelegator = it }

      builder.stateUpdater(stateUpdaterDelegator)
      builder.poolScope(poolScope)

      _componentTree =
          builder.build().also { tree ->
            stateUpdaterDelegator.attachStateUpdater(tree)
            pendingNewLayoutListener?.let { tree.newLayoutStateReadyListener = it }
          }
    }
    return checkNotNull(_componentTree)
  }

  /**
   * We may need to wait until the corresponding view is detached before releasing the tree as the
   * view might need to run an animation
   */
  @UiThread
  @Synchronized
  fun releaseTreeImmediatelyOrOnViewDetached() {
    val componentTree = _componentTree
    if (componentTree != null) {
      val lithoView = componentTree.lithoView
      if (lithoView?.isAttachedToWindow == true) {
        lithoView.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
              override fun onViewAttachedToWindow(view: View) = Unit

              override fun onViewDetachedFromWindow(view: View) {
                releaseTree()
                view.removeOnAttachStateChangeListener(this)
              }
            })
      } else {
        releaseTree()
      }
    }
  }

  @UiThread
  @Synchronized
  fun releaseTree() {
    if (_componentTree != null) {
      visibilityEventsController?.let { controller ->
        controller.moveToVisibilityState(LithoVisibilityState.DESTROYED)
        return
      }
      _componentTree?.release()
      _componentTree = null
    }

    _isTreeValid = false
  }

  fun shouldPreventRelease(): Boolean {
    val preventRelease = renderInfo.getCustomAttribute(PREVENT_RELEASE_TAG)
    return (preventRelease as? Boolean) ?: false
  }

  private fun shouldAcquireTreeStateOnRelease(): Boolean {
    val acquireTreeState = renderInfo.getCustomAttribute(ACQUIRE_STATE_HANDLER_ON_RELEASE)
    return (acquireTreeState as? Boolean) ?: acquireTreeStateOnRelease
  }

  @GuardedBy("this")
  private fun acquireTreeState() {
    _componentTree?.let { treeState = it.acquireTreeState() }
  }

  /** Lifecycle controlled by a ComponentTreeHolder. */
  private inner class ComponentTreeHolderVisibilityEventsController :
      LithoVisibilityEventsController, LithoVisibilityEventsListener, AOSPLifecycleOwnerProvider {
    private val lithoVisibilityEventsControllerDelegate = LithoVisibilityEventsControllerDelegate()

    init {
      lithoVisibilityEventsController?.addListener(this)
    }

    override val visibilityState: LithoVisibilityState
      get() = lithoVisibilityEventsControllerDelegate.visibilityState

    override fun onMovedToState(state: LithoVisibilityState) {
      when (state) {
        LithoVisibilityState.HINT_VISIBLE ->
            moveToVisibilityState(LithoVisibilityState.HINT_VISIBLE)
        LithoVisibilityState.HINT_INVISIBLE ->
            moveToVisibilityState(LithoVisibilityState.HINT_INVISIBLE)
        LithoVisibilityState.DESTROYED -> moveToVisibilityState(LithoVisibilityState.DESTROYED)
      }
    }

    @UiThread
    override fun moveToVisibilityState(lithoLifecycle: LithoVisibilityState) {
      ThreadUtils.assertMainThread()
      lithoVisibilityEventsControllerDelegate.moveToVisibilityState(lithoLifecycle)
      if (lithoLifecycle == LithoVisibilityState.DESTROYED) {
        lithoVisibilityEventsController?.removeListener(this)
        _componentTree = null
        _isTreeValid = false
      }
    }

    @Synchronized
    override fun addListener(listener: LithoVisibilityEventsListener) {
      lithoVisibilityEventsControllerDelegate.addListener(listener)
    }

    @Synchronized
    override fun removeListener(listener: LithoVisibilityEventsListener) {
      lithoVisibilityEventsControllerDelegate.removeListener(listener)
    }

    override val lifecycleOwner: LifecycleOwner?
      get() = (lithoVisibilityEventsController as? AOSPLifecycleOwnerProvider)?.lifecycleOwner
  }

  companion object {
    private const val UNINITIALIZED = -1
    private val idGenerator = AtomicInteger(1)
    const val PREVENT_RELEASE_TAG: String = "prevent_release"
    const val ACQUIRE_STATE_HANDLER_ON_RELEASE: String = "acquire_state_handler"
    const val RENDER_UNINITIALIZED: Int = 0
    const val RENDER_ADDED: Int = 1
    const val RENDER_DRAWN: Int = 2

    @JvmStatic fun create(configuration: ComponentsConfiguration): Builder = Builder(configuration)
  }
}
