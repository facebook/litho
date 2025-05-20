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
import android.view.accessibility.AccessibilityManager
import com.facebook.litho.debug.DebugOverlay
import com.facebook.litho.debug.LithoDebugEvent
import com.facebook.litho.debug.LithoDebugEventAttributes.Attribution
import com.facebook.litho.debug.LithoDebugEventAttributes.Root
import com.facebook.litho.stats.LithoStats
import com.facebook.rendercore.debug.DebugEventAttribute.Source
import com.facebook.rendercore.debug.DebugEventAttribute.Version
import com.facebook.rendercore.debug.DebugEventDispatcher

class ResolveTreeFuture
/** TODO(T137275959) */
@Deprecated("Refactor sync render logic to remove sizes from resolved tree future")
constructor(
    private val componentContext: ComponentContext,
    private val component: Component,
    private val treeState: TreeState,
    private val currentRootNode: LithoNode?,
    private val perfEvent: PerfEvent?,
    private val resolveVersion: Int,
    useCancellableFutures: Boolean,
    private val syncWidthSpec: Int,
    private val syncHeightSpec: Int,
    private val componentTreeId: Int,
    private val extraAttribution: String?,
    private val source: Int
) : TreeFuture<ResolveResult>(componentTreeId, useCancellableFutures) {

  private val enableResolveWithoutSizeSpec =
      componentContext.lithoConfiguration.componentsConfig.enableResolveWithoutSizeSpec

  constructor(
      c: ComponentContext,
      component: Component,
      treeState: TreeState,
      currentRootNode: LithoNode?,
      perfEvent: PerfEvent?,
      resolveVersion: Int,
      useCancellableFutures: Boolean,
      componentTreeId: Int,
      extraAttribution: String?,
      source: Int
  ) : this(
      c,
      component,
      treeState,
      currentRootNode,
      perfEvent,
      resolveVersion,
      useCancellableFutures,
      ComponentTree.SIZE_UNINITIALIZED,
      ComponentTree.SIZE_UNINITIALIZED,
      componentTreeId,
      extraAttribution,
      source)

  override fun calculate(): ResolveResult {
    val resolveTraceIdentifier =
        DebugEventDispatcher.generateTraceIdentifier(LithoDebugEvent.ComponentTreeResolve)
    if (resolveTraceIdentifier != null) {
      DebugEventDispatcher.beginTrace(
          resolveTraceIdentifier,
          LithoDebugEvent.ComponentTreeResolve,
          componentTreeId.toString(),
          createDebugAttributes())
    }
    return try {
      resolve(
          componentContext,
          component,
          treeState,
          resolveVersion,
          componentTreeId,
          currentRootNode,
          extraAttribution,
          this,
          perfEvent)
    } finally {
      if (resolveTraceIdentifier != null) {
        DebugEventDispatcher.endTrace(resolveTraceIdentifier)
      }
    }
  }

  override fun getVersion(): Int = resolveVersion

  override fun getDescription(): String = DESCRIPTION

  override fun resumeCalculation(partialResult: ResolveResult?): ResolveResult {
    val resolveTraceIdentifier =
        DebugEventDispatcher.generateTraceIdentifier(LithoDebugEvent.ComponentTreeResume)
    if (resolveTraceIdentifier != null) {
      DebugEventDispatcher.beginTrace(
          resolveTraceIdentifier,
          LithoDebugEvent.ComponentTreeResume,
          componentTreeId.toString(),
          createDebugAttributes())
    }
    return try {
      resume(checkNotNull(partialResult), extraAttribution)
    } finally {
      if (resolveTraceIdentifier != null) {
        DebugEventDispatcher.endTrace(resolveTraceIdentifier)
      }
    }
  }

  private fun createDebugAttributes(): HashMap<String, Any?> {
    val attributes = HashMap<String, Any?>()
    attributes[Root] = component.simpleName
    attributes[Version] = resolveVersion
    attributes[Source] = LayoutState.layoutSourceToString(source)
    attributes[Attribution] = extraAttribution
    return attributes
  }

  override fun isEquivalentTo(that: TreeFuture<*>?): Boolean {
    if (that !is ResolveTreeFuture) {
      return false
    }
    if (component.id != that.component.id) {
      return false
    }
    if (componentContext.treePropContainer !== that.componentContext.treePropContainer) {
      return false
    }

    if (!enableResolveWithoutSizeSpec) {
      if (syncWidthSpec != that.syncWidthSpec) {
        return false
      }

      if (syncHeightSpec != that.syncHeightSpec) {
        return false
      }
    }

    return true
  }

  internal interface ExecutionListener {

    fun onPreExecution(version: Int)

    fun onPostExecution(version: Int, released: Boolean)
  }

  companion object {

    const val DESCRIPTION: String = "resolve"

    /** Function which resolves a new RenderResult. */
    @JvmStatic
    fun resolve(
        context: ComponentContext,
        component: Component,
        state: TreeState,
        version: Int,
        componentTreeId: Int,
        currentRootNode: LithoNode?,
        extraAttribution: String?,
        future: TreeFuture<*>?,
        perfEventLogger: PerfEvent?
    ): ResolveResult {
      LithoStats.incrementResolveCount()
      val isTracing = ComponentsSystrace.isTracing
      return try {
        if (isTracing) {
          if (extraAttribution != null) {
            ComponentsSystrace.beginSection("extra:$extraAttribution")
          }
          ComponentsSystrace.beginSectionWithArgs("resolveTree:${component.simpleName}")
              .arg("treeId", componentTreeId)
              .arg("rootId", component.id)
              .flush()
        }
        state.registerResolveState()
        val rsc =
            ResolveContext(
                componentTreeId,
                MeasuredResultCache(),
                state,
                version,
                component.id,
                AccessibilityUtils.isAccessibilityEnabled(
                    context.androidContext.getSystemService(Context.ACCESSIBILITY_SERVICE)
                        as AccessibilityManager),
                future,
                currentRootNode,
                perfEventLogger,
                context.logger,
                false)
        val previousStateContext = context.calculationStateContext
        val stateProvider = context.stateProvider ?: error("State provider is null in resolve")
        val node: LithoNode?
        try {
          context.renderStateContext = rsc
          stateProvider.enterScope(state)
          node = Resolver.resolveTree(rsc, context, component)
        } finally {
          stateProvider.exitScope(state)
          context.calculationStateContext = previousStateContext
        }
        val outputs: Resolver.Outputs?
        if (rsc.isLayoutInterrupted) {
          outputs = null
        } else {
          outputs = Resolver.collectOutputs(node)
          rsc.cache.freezeCache()
        }
        if (DebugOverlay.isEnabled) {
          DebugOverlay.updateResolveHistory(componentTreeId)
        }
        ResolveResult(
            node,
            context,
            component,
            rsc.cache,
            state,
            rsc.isLayoutInterrupted,
            version,
            rsc.eventHandlers,
            outputs,
            if (rsc.isLayoutInterrupted) rsc else null)
      } finally {
        state.unregisterResolveInitialState()
        if (isTracing) {
          ComponentsSystrace.endSection()
          if (extraAttribution != null) {
            ComponentsSystrace.endSection()
          }
        }
      }
    }

    @JvmStatic
    fun resume(partialResult: ResolveResult, extraAttribution: String?): ResolveResult {
      LithoStats.incrementResumeCount()
      val context = partialResult.context
      val component = partialResult.component
      val resolveVersion = partialResult.version
      check(partialResult.isPartialResult) { "Cannot resume a non-partial result" }
      checkNotNull(partialResult.node) { "Cannot resume a partial result with a null node" }
      checkNotNull(partialResult.contextForResuming) {
        "RenderStateContext cannot be null during resume"
      }
      val isTracing = ComponentsSystrace.isTracing
      return try {
        if (isTracing) {
          if (extraAttribution != null) {
            ComponentsSystrace.beginSection("extra:$extraAttribution")
          }
          ComponentsSystrace.beginSection("resume:${component.simpleName}")
        }
        partialResult.treeState.registerResolveState()
        val previousStateContext = context.calculationStateContext
        val stateProvider = context.stateProvider ?: error("State provider is null in resolve")
        val node: LithoNode
        try {
          context.renderStateContext = partialResult.contextForResuming
          stateProvider.enterScope(partialResult.treeState)
          node = Resolver.resumeResolvingTree(partialResult.contextForResuming, partialResult.node)
        } finally {
          stateProvider.exitScope(partialResult.treeState)
          context.calculationStateContext = previousStateContext
        }
        val outputs = Resolver.collectOutputs(node)
        partialResult.contextForResuming.cache.freezeCache()
        val createdEventHandlers = partialResult.contextForResuming.eventHandlers
        partialResult.treeState.unregisterResolveInitialState()
        ResolveResult(
            node,
            context,
            partialResult.component,
            partialResult.consumeCache(),
            partialResult.treeState,
            false,
            resolveVersion,
            createdEventHandlers,
            outputs,
            null)
      } finally {
        if (isTracing) {
          ComponentsSystrace.endSection()
          if (extraAttribution != null) {
            ComponentsSystrace.endSection()
          }
        }
      }
    }
  }
}
