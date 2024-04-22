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
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.litho.NestedLithoTree.cleanup
import com.facebook.litho.NestedLithoTree.commit
import com.facebook.litho.NestedLithoTree.enqueue
import com.facebook.litho.NestedLithoTree.runEffects
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator

/**
 * Returns a [Primitive], and the [ResolveResult] that renders an embedded Litho component. This API
 * should only be used when embedding Litho in a non Litho RC framework, like Bloks. This API
 * accepts the current state and pending state updates.
 */
fun NestedLithoPrimitive(
    renderUnitId: Long,
    resolveContext: NestedLithoResolveContext,
    component: Component,
    treeProps: TreePropContainer?,
    currentState: TreeState,
    updates: List<PendingStateUpdate>?,
): Pair<Primitive, ResolveResult> {
  val updatedState =
      TreeState(currentState).also {
        if (!updates.isNullOrEmpty()) {
          it.enqueue(updates)
        }
      }

  return NestedLithoPrimitive(
      renderUnitId = renderUnitId,
      resolveContext = resolveContext,
      component = component,
      treeProps = treeProps,
      updatedState = updatedState,
  )
}

/**
 * Returns a [Primitive], and the [ResolveResult] that renders an embedded Litho component. This API
 * should only be used when embedding Litho in a non Litho RC framework, like Bloks. This API
 * accepts the updated state; this API should only be used if the client framework applies state
 * updates before resolving the component to a Primitive.
 */
fun NestedLithoPrimitive(
    renderUnitId: Long,
    resolveContext: NestedLithoResolveContext,
    component: Component,
    treeProps: TreePropContainer?,
    updatedState: TreeState,
): Pair<Primitive, ResolveResult> {

  val (
      nestedTreeId,
      androidContext,
      config,
      currentResolveResult,
      stateUpdateRequest,
      stateCommitListener,
      errorComponent,
      rootHostReference,
      lifecycleProvider,
  ) = resolveContext

  val componentContext: ComponentContext =
      createdNestedTreeComponentContext(
          treeId = nestedTreeId.id,
          androidContext = androidContext,
          treeProps = treeProps,
          lithoConfiguration = config,
          updatedState = updatedState,
          updater = stateUpdateRequest,
          rootHostReference = rootHostReference,
          errorComponent = errorComponent,
          lifecycleProvider = lifecycleProvider,
      )

  val result =
      NestedLithoTree.resolve(
          nestedTreeId.id,
          componentContext,
          component,
          treeProps,
          updatedState,
          currentResolveResult,
      )

  val mountBehavior =
      MountBehavior(
          id = renderUnitId,
          contentAllocator = ViewAllocator { LithoRenderTreeView(it) },
      ) {

        // binder to clean up the content before returning it to the pool
        withDescription("final-unmount") {
          bind(nestedTreeId.id) { content ->
            content.onClean = {
              rootHostReference.mountedView = null
              content.currentLayoutState?.cleanup()
              lifecycleProvider.release()
            }
            onUnbind { content.clean() }
          }
        }

        // binder to bind the layout state with the Litho Render Tree View
        withDescription("litho-tree") {
          bindWithLayoutData<LayoutState> { content, layoutState ->

            // commit the state for the LayoutState that is going to be mounted
            val newState = layoutState.resolveResult.treeState

            stateCommitListener?.commit(newState)
            layoutState.commit()
            layoutState.runEffects()

            content.setLayoutState(layoutState, newState)

            onUnbind {}
          }
        }

        withDescription("root-host-reference") {
          bind(rootHostReference) { content ->
            rootHostReference.mountedView = content
            onUnbind { rootHostReference.mountedView = null }
          }
        }
      }

  return Pair(
      Primitive(
          layoutBehavior =
              LithoLayoutBehavior(
                  result = result,
                  cacheKey = resolveContext.treeId,
                  resolveContext.usePreviousLayoutState),
          mountBehavior = mountBehavior,
      ),
      result,
  )
}

fun createdNestedTreeComponentContext(
    treeId: Int,
    androidContext: Context,
    treeProps: TreePropContainer?,
    lithoConfiguration: LithoConfiguration,
    updatedState: TreeState,
    updater: StateUpdateRequester,
    rootHostReference: NestedMountedViewReference,
    errorComponent: (Component?) -> Unit,
    lifecycleProvider: NestedLithoTreeLifecycleProvider,
): ComponentContext {
  return ComponentContext(
      androidContext,
      treeProps,
      lithoConfiguration,
      LithoTree(
          stateUpdater =
              NestedStateUpdater(
                  state = updatedState,
                  updater = updater,
              ),
          mountedViewReference = rootHostReference,
          errorComponentReceiver = errorComponent,
          lithoTreeLifecycleProvider = lifecycleProvider,
          treeId,
      ),
      "nested-tree-root",
      null,
      null,
      null,
  )
}

internal class LithoLayoutBehavior(
    val result: ResolveResult,
    private val cacheKey: NestedTreeId,
    private val usePreviousLayoutState: Boolean,
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    val previousLayoutState: LayoutState? =
        if (usePreviousLayoutState) {
          layoutContext.layoutCache[cacheKey]
        } else {
          null
        }

    val layoutState = NestedLithoTree.layout(result, sizeConstraints, previousLayoutState)

    if (usePreviousLayoutState) {
      layoutContext.layoutCache.put(cacheKey, layoutState)
    }

    return PrimitiveLayoutResult(
        width = layoutState.width,
        height = layoutState.height,
        layoutData = layoutState,
    )
  }
}

@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
data class NestedLithoResolveContext(
    val treeId: NestedTreeId,
    val androidContext: Context,
    val config: LithoConfiguration,
    val currentResolveResult: ResolveResult?,
    val stateUpdateRequest: StateUpdateRequester,
    val stateCommitListener: StateCommitListener? = null,
    val errorComponent: ((Component?) -> Unit) = { /* TODO: provide default implementation */ },
    val rootHostReference: NestedMountedViewReference = NestedMountedViewReference(),
    val lifecycleProvider: NestedLithoTreeLifecycleProvider = NestedLithoTreeLifecycleProvider(),
    val usePreviousLayoutState: Boolean,
)

class NestedTreeId(val id: Int)

fun interface StateUpdateRequester {
  fun request(update: PendingStateUpdate)
}

fun interface StateCommitListener {
  fun commit(treeState: TreeState)
}
