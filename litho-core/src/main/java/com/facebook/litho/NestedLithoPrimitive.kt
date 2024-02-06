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
import com.facebook.litho.ComponentContextUtils.buildDefaultLithoConfiguration
import com.facebook.litho.NestedLithoTree.enqueue
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator

fun NestedLithoPrimitive(
    id: Long,
    resolveContext: NestedLithoResolveContext,
    component: Component,
    treeProps: TreePropContainer?,
    updates: List<PendingStateUpdate>?,
): Pair<Primitive, ResolveResult> {

  val (
      treeId,
      androidContext,
      config,
      currentResolveResult,
      stateUpdateRequest,
      errorComponent,
      rootHostReference,
      lifecycleProvider,
  ) = resolveContext

  val currentState = resolveContext.currentResolveResult?.treeState ?: TreeState()
  val updatedState =
      if (updates != null) {
        TreeState(currentState).enqueue(updates)
      } else {
        currentState
      }

  val componentContext =
      ComponentContext(
          androidContext,
          treeProps,
          buildDefaultLithoConfiguration(
              context = androidContext,
              componentsConfig = config,
              renderUnitIdGenerator = RenderUnitIdGenerator(treeId),
          ),
          LithoTree(
              stateUpdater =
                  NestedStateUpdater(
                      state = updatedState,
                      requestUpdate = stateUpdateRequest,
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

  val result =
      NestedLithoTree.resolve(
          treeId,
          componentContext,
          component,
          treeProps,
          updatedState,
          currentResolveResult,
      )

  val mountBehavior =
      MountBehavior(
          id = id,
          contentAllocator = ViewAllocator { LithoRenderTreeView(it) },
      ) {

        // binder to bind the layout state with the Litho Render Tree View
        withDescription("litho-tree") {
          bindWithLayoutData<LayoutState> { content, layoutState ->

            // commit the state for the LayoutState that is going to be mounted
            val newState = layoutState.resolveResult.treeState
            newState.commit()

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

        withDescription("final-unmount") {
          bind(Unit) { content ->
            onUnbind {
              lifecycleProvider.release()
              content.resetLayoutState()
            }
          }
        }
      }

  return Pair(
      Primitive(
          layoutBehavior = LithoLayoutBehavior(result = result),
          mountBehavior = mountBehavior,
      ),
      result,
  )
}

internal class LithoLayoutBehavior(val result: ResolveResult) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    val layoutState =
        NestedLithoTree.layout(
            result,
            sizeConstraints,
            layoutContext.consumePreviousLayoutDataForCurrentNode() as LayoutState?,
        )
    return PrimitiveLayoutResult(
        width = layoutState.width,
        height = layoutState.height,
        layoutData = layoutState,
    )
  }
}

@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
data class NestedLithoResolveContext(
    val treeId: Int,
    val androidContext: Context,
    val config: ComponentsConfiguration = ComponentsConfiguration.defaultInstance,
    val currentResolveResult: ResolveResult?,
    val stateUpdateRequest: (update: PendingStateUpdate) -> Unit,
    val errorComponent: ((Component?) -> Unit) = {}, /* TODO: provide default implementation */
    val rootHostReference: NestedMountedViewReference = NestedMountedViewReference(),
    val lifecycleProvider: NestedLithoTreeLifecycleProvider = NestedLithoTreeLifecycleProvider(),
)
