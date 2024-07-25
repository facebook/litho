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

package com.facebook.rendercore

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.facebook.infer.annotation.ThreadConfined
import com.facebook.rendercore.StateUpdateReceiver.StateUpdate
import com.facebook.rendercore.extensions.RenderCoreExtension
import com.facebook.rendercore.utils.ThreadUtils
import com.facebook.rendercore.utils.VSyncUtils
import java.util.Objects
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.concurrent.ThreadSafe
import kotlin.math.roundToInt

/** todo: javadocs * */
class RenderState<State, RenderContext, StateUpdateType : StateUpdate<*>>
@JvmOverloads
constructor(
    private val context: Context,
    private val delegate: Delegate<State>,
    private val renderContext: RenderContext?,
    val extensions: Array<RenderCoreExtension<*, *>>?,
    private val resolveExecutor: Executor = Executor { r -> ThreadUtils.runOnBackgroundThread(r) }
) : StateUpdateReceiver<StateUpdateType> {

  /**
   * Represents a function capable of creating a tree. The tree is lazy so that the creation can be
   * done inline with the layout pass.
   *
   * @param <State> Represents the State that this tree would like to commit when the tree itself is
   *   committed
   */
  @ThreadSafe
  fun interface ResolveFunc<State, RenderContext, StateUpdateType : StateUpdate<*>> {
    /**
     * Resolves the tree represented by this ResolveFunc. Results for resolve might be cached. The
     * assumption is that multiple resolve calls on a ResolveFunc would return equivalent trees.
     *
     * @param resolveContext
     * @param committedTree
     * @param committedState
     * @param stateUpdatesToApply
     */
    fun resolve(
        resolveContext: ResolveContext<RenderContext, StateUpdateType>,
        committedTree: Node<RenderContext>?,
        committedState: State?,
        stateUpdatesToApply: List<@JvmSuppressWildcards StateUpdateType>
    ): ResolveResult<Node<RenderContext>, State>
  }

  interface Delegate<State> {
    fun commit(
        layoutVersion: Int,
        current: RenderTree?,
        next: RenderTree,
        currentState: State?,
        nextState: State?
    )

    fun commitToUI(tree: RenderTree?, state: State?, frameVersion: Int)
  }

  fun interface HostListener {
    fun onUIRenderTreeUpdated(newRenderTree: RenderTree?)
  }

  private val uiHandler: RenderStateHandler = RenderStateHandler(Looper.getMainLooper())
  val id: Int = ID_GENERATOR.incrementAndGet()

  @ThreadConfined(ThreadConfined.UI) private var hostListener: HostListener? = null

  @get:ThreadConfined(ThreadConfined.UI)
  var uiRenderTree: RenderTree? = null
    private set

  private var latestResolveFunc: ResolveFunc<State, RenderContext, StateUpdateType>? = null
  private var resolveFuture: ResolveFuture<State, RenderContext, StateUpdateType>? = null
  private var layoutFuture: LayoutFuture<State, RenderContext>? = null
  private var resolveVersionCounter = 0
  private var committedResolveVersion = UNSET
  private var committedResolvedTree: Node<RenderContext>? = null
  private var committedState: State? = null
  private var committedResolvedFrameId = UNSET
  private val pendingStateUpdates: MutableList<StateUpdateType> = ArrayList()
  private var committedRenderResult: RenderResult<State, RenderContext>? = null

  private val normalVSyncTime = VSyncUtils.getNormalVsyncTime(context)
  private val frameReferenceTimeNanos = System.nanoTime()

  private var layoutVersionCounter = 0
  private var committedLayoutVersion = UNSET
  private var committedLayoutFrameId = UNSET
  private var sizeConstraints: SizeConstraints = SizeConstraints()
  private var hasSizeConstraints = false
  private val resolveToken = Any()

  @ThreadConfined(ThreadConfined.ANY)
  fun setTree(resolveFunc: ResolveFunc<State, RenderContext, StateUpdateType>?) {
    requestResolve(resolveFunc, doAsync = false)
  }

  @ThreadConfined(ThreadConfined.ANY)
  fun setTreeAsync(
      resolveFunc: ResolveFunc<State, RenderContext, StateUpdateType>?,
  ) {
    requestResolve(resolveFunc, doAsync = true)
  }

  @ThreadConfined(ThreadConfined.ANY)
  override fun enqueueStateUpdate(stateUpdate: StateUpdateType) {
    enqueueStateUpdateInternal(stateUpdate, true)
  }

  @ThreadConfined(ThreadConfined.ANY)
  override fun enqueueStateUpdateSync(stateUpdate: StateUpdateType) {
    enqueueStateUpdateInternal(stateUpdate, false)
  }

  private fun enqueueStateUpdateInternal(stateUpdate: StateUpdateType, async: Boolean) {
    synchronized(this) {
      pendingStateUpdates.add(stateUpdate)
      if (latestResolveFunc == null) {
        return
      }
    }

    uiHandler.removeCallbacksAndMessages(resolveToken)
    uiHandler.postAtTime({ requestResolve(null, async) }, resolveToken, 0)
  }

  private fun getElapsedFrameCount(): Int {
    val elapsedTimeNanos = System.nanoTime() - frameReferenceTimeNanos
    return (elapsedTimeNanos * 1.0 / normalVSyncTime).roundToInt()
  }

  private fun requestResolve(
      resolveFunc: ResolveFunc<State, RenderContext, StateUpdateType>?,
      doAsync: Boolean,
  ) {
    val future: ResolveFuture<State, RenderContext, StateUpdateType>
    synchronized(this) {

      // Resolve was triggered by State Update, but all pendingStateUpdates are already applied.
      if (resolveFunc == null && pendingStateUpdates.isEmpty()) {
        return
      }

      if (resolveFunc != null) {
        latestResolveFunc = resolveFunc
      }
      future =
          ResolveFuture(
              requireNotNull(latestResolveFunc),
              ResolveContext(renderContext, this),
              committedResolvedTree,
              committedState,
              if (pendingStateUpdates.isEmpty()) emptyList() else ArrayList(pendingStateUpdates),
              resolveVersionCounter++,
              getElapsedFrameCount())
      resolveFuture = future
    }
    if (doAsync) {
      resolveExecutor.execute { resolveTreeAndMaybeCommit(future) }
    } else {
      resolveTreeAndMaybeCommit(future)
    }
  }

  private fun resolveTreeAndMaybeCommit(
      future: ResolveFuture<State, RenderContext, StateUpdateType>
  ) {
    val result = future.runAndGet()
    if (maybeCommitResolveResult(result, future)) {
      layoutAndMaybeCommitInternal(null)
    }
  }

  @Synchronized
  private fun maybeCommitResolveResult(
      result: ResolveResult<Node<RenderContext>, State>,
      future: ResolveFuture<State, RenderContext, StateUpdateType>
  ): Boolean {
    // We don't want to compute, layout, or reduce trees while holding a lock. However this means
    // that another thread could compute a layout and commit it before we get to this point. To
    // handle this, we make sure that the committed resolve version is only ever increased, meaning
    // we only go "forward in time" and will eventually get to the latest layout.
    var didCommit = false
    if (future.version > committedResolveVersion) {
      committedResolveVersion = future.version
      committedResolvedTree = result.resolvedNode
      committedResolvedFrameId = future.frameId
      committedState = result.resolvedState
      val appliedUpdates = result.appliedStateUpdates
      if (!appliedUpdates.isNullOrEmpty()) {
        pendingStateUpdates.removeAll(appliedUpdates)
      }
      didCommit = true
    }
    if (resolveFuture == future) {
      resolveFuture = null
    }
    return didCommit
  }

  private fun layoutAndMaybeCommitInternal(measureOutput: IntArray?) {
    val layoutFuture: LayoutFuture<State, RenderContext>
    val previousRenderResult: RenderResult<State, RenderContext>?
    synchronized(this) {
      if (!hasSizeConstraints) {
        return
      }
      val commitedTree =
          requireNotNull(committedResolvedTree) {
            "Tried executing the layout step before resolving a tree"
          }
      val layout = this.layoutFuture
      if (layout == null || layout.tree != commitedTree || !hasSameSpecs(layout, sizeConstraints)) {
        this.layoutFuture =
            LayoutFuture(
                context,
                renderContext,
                commitedTree,
                committedState,
                layoutVersionCounter++,
                committedResolvedFrameId,
                committedRenderResult,
                extensions,
                sizeConstraints)
      }
      layoutFuture = requireNotNull(this.layoutFuture)
      previousRenderResult = committedRenderResult
    }
    val renderResult = layoutFuture.runAndGet()
    var committedNewLayout = false
    synchronized(this) {
      if (hasSameSpecs(layoutFuture, sizeConstraints) &&
          layoutFuture.version > committedLayoutVersion &&
          committedRenderResult != renderResult) {
        committedLayoutVersion = layoutFuture.version
        committedNewLayout = true
        committedLayoutFrameId = layoutFuture.frameId
        committedRenderResult = renderResult
      }
      if (this.layoutFuture == layoutFuture) {
        this.layoutFuture = null
      }
    }
    if (measureOutput != null) {
      measureOutput[0] = renderResult.renderTree.width
      measureOutput[1] = renderResult.renderTree.height
    }
    if (committedNewLayout) {
      delegate.commit(
          layoutFuture.version,
          previousRenderResult?.renderTree,
          renderResult.renderTree,
          previousRenderResult?.state,
          renderResult.state)
      schedulePromoteCommittedTreeToUI()
    }
  }

  @ThreadConfined(ThreadConfined.UI)
  fun measure(sizeConstraints: SizeConstraints, measureOutput: IntArray?) {
    val futureToResolveBeforeMeasuring: ResolveFuture<State, RenderContext, StateUpdateType>?
    synchronized(this) {
      if (!hasSizeConstraints || this.sizeConstraints != sizeConstraints) {
        hasSizeConstraints = true
        this.sizeConstraints = sizeConstraints
      }

      val renderTree = uiRenderTree
      // The current UI tree is compatible. We might just return those values
      if (renderTree != null && hasCompatibleSize(renderTree, sizeConstraints)) {
        if (measureOutput != null) {
          measureOutput[0] = renderTree.width
          measureOutput[1] = renderTree.height
        }
        return
      }
      val renderResult = committedRenderResult
      if (renderResult != null && hasCompatibleSize(renderResult.renderTree, sizeConstraints)) {
        maybePromoteCommittedTreeToUI()
        if (measureOutput != null) {
          // We have a tree that we previously resolved with these contraints. For measuring we can
          // just return it
          measureOutput[0] = renderResult.renderTree.width
          measureOutput[1] = renderResult.renderTree.height
        }
        return
      }

      // We don't have a valid resolve function yet. Let's just bail until then.
      if (latestResolveFunc == null) {
        if (measureOutput != null) {
          measureOutput[0] = 0
          measureOutput[1] = 0
        }
        return
      }

      // If we do have a resolve function we expect to have either a committed resolved tree or a
      // future. If we have neither something has gone wrong with the setTree call.
      futureToResolveBeforeMeasuring =
          if (committedResolvedTree != null) {
            null
          } else {
            Objects.requireNonNull(resolveFuture)
          }
    }
    if (futureToResolveBeforeMeasuring != null) {
      val result = futureToResolveBeforeMeasuring.runAndGet()
      maybeCommitResolveResult(result, futureToResolveBeforeMeasuring)
    }
    layoutAndMaybeCommitInternal(measureOutput)
  }

  @ThreadConfined(ThreadConfined.UI)
  fun attach(hostListener: HostListener) {
    if (this.hostListener != null && this.hostListener != hostListener) {
      throw RuntimeException("Must detach from previous host listener first")
    }
    this.hostListener = hostListener
  }

  @ThreadConfined(ThreadConfined.UI)
  fun detach() {
    hostListener = null
  }

  private fun schedulePromoteCommittedTreeToUI() {
    if (ThreadUtils.isMainThread) {
      maybePromoteCommittedTreeToUI()
    } else {
      if (!uiHandler.hasMessages(PROMOTION_MESSAGE)) {
        uiHandler.sendEmptyMessage(PROMOTION_MESSAGE)
      }
    }
  }

  @ThreadConfined(ThreadConfined.UI)
  private fun maybePromoteCommittedTreeToUI() {
    synchronized(this) {
      delegate.commitToUI(
          committedRenderResult?.renderTree, committedRenderResult?.state, committedLayoutFrameId)
      if (uiRenderTree == committedRenderResult?.renderTree) {
        return
      }
      uiRenderTree = committedRenderResult?.renderTree
    }
    hostListener?.onUIRenderTreeUpdated(uiRenderTree)
  }

  private inner class RenderStateHandler(looper: Looper) : Handler(looper) {
    override fun handleMessage(msg: Message) {
      when (msg.what) {
        PROMOTION_MESSAGE -> maybePromoteCommittedTreeToUI()
        else -> throw RuntimeException("Unknown message: " + msg.what)
      }
    }
  }

  companion object {
    @JvmField var NO_ID: Int = -1
    private const val UNSET: Int = -1
    private const val PROMOTION_MESSAGE: Int = 99
    private val ID_GENERATOR: AtomicInteger = AtomicInteger(0)

    @JvmStatic
    private fun hasCompatibleSize(tree: RenderTree, sizeConstraints: SizeConstraints): Boolean {
      return sizeConstraints.areCompatible(tree.sizeConstraints, Size(tree.width, tree.height))
    }

    @JvmStatic
    private fun <State, RenderContext> hasSameSpecs(
        future: LayoutFuture<State, RenderContext>,
        sizeConstraints: SizeConstraints
    ): Boolean {
      return future.sizeConstraints == sizeConstraints
    }
  }
}
