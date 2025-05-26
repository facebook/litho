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

import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.ListUpdateCallback
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentsReporter
import com.facebook.litho.ComponentsReporter.emitMessage
import com.facebook.litho.ComponentsSystrace.beginSection
import com.facebook.litho.ComponentsSystrace.endSection
import com.facebook.litho.ComponentsSystrace.isTracing
import com.facebook.litho.Diff

/**
 * An implementation of [ListUpdateCallback] that generates the relevant
 * [com.facebook.litho.Component]s when an item is inserted/updated.
 *
 * The user of this API is expected to provide a ComponentRenderer implementation to build a
 * Component from a generic model object.
 */
class RecyclerBinderUpdateCallback<T>(
    private val prevData: List<T>?,
    private val nextData: List<T>?,
    private val componentRenderer: ComponentRenderer<T>,
    private val operationExecutor: OperationExecutor
) : ListUpdateCallback {

  fun interface ComponentRenderer<T> {
    fun render(t: T, idx: Int): RenderInfo
  }

  fun interface OperationExecutor {
    fun executeOperations(c: ComponentContext, operations: List<Operation>)
  }

  private val oldDataSize = prevData?.size ?: 0
  private val _operations: MutableList<Operation> = ArrayList()
  private val placeholders: MutableList<ComponentContainer> = ArrayList()
  private val dataHolders: MutableList<Diff<T>> = ArrayList()

  constructor(
      prevData: List<T>?,
      nextData: List<T>?,
      componentRenderer: ComponentRenderer<T>,
      recyclerBinder: RecyclerBinder
  ) : this(prevData, nextData, componentRenderer, RecyclerBinderOperationExecutor(recyclerBinder))

  init {
    for (i in 0 until oldDataSize) {
      placeholders.add(ComponentContainer(null, false))
      dataHolders.add(Diff(prevData?.get(i), null))
    }
  }

  override fun onInserted(position: Int, count: Int) {
    val placeholders: MutableList<ComponentContainer> = ArrayList(count)
    val dataHolders: MutableList<Diff<*>> = ArrayList(count)
    for (i in 0 until count) {
      val index = position + i
      val componentContainer = ComponentContainer(null, true)
      this.placeholders.add(index, componentContainer)
      placeholders.add(componentContainer)

      val dataHolder: Diff<T> = Diff(null, null)
      this.dataHolders.add(index, dataHolder)
      dataHolders.add(dataHolder)
    }

    _operations.add(Operation(Operation.INSERT, position, -1, placeholders, dataHolders))
  }

  override fun onRemoved(position: Int, count: Int) {
    val dataHolders: MutableList<Diff<*>> = ArrayList(count)
    for (i in 0 until count) {
      placeholders.removeAt(position)

      val dataHolder = this.dataHolders.removeAt(position)
      dataHolders.add(dataHolder)
    }

    _operations.add(Operation(Operation.DELETE, position, count, null, dataHolders))
  }

  override fun onMoved(fromPosition: Int, toPosition: Int) {
    val dataHolders: MutableList<Diff<*>> = ArrayList(1)

    val placeholder = placeholders.removeAt(fromPosition)
    placeholders.add(toPosition, placeholder)

    val dataHolder = this.dataHolders.removeAt(fromPosition)
    dataHolders.add(dataHolder)
    this.dataHolders.add(toPosition, dataHolder)

    _operations.add(Operation(Operation.MOVE, fromPosition, toPosition, null, dataHolders))
  }

  override fun onChanged(position: Int, count: Int, payload: Any?) {
    val placeholders: MutableList<ComponentContainer> = ArrayList()
    val dataHolders: MutableList<Diff<*>> = ArrayList(count)

    for (i in 0 until count) {
      val index = position + i
      val placeholder = this.placeholders[index]
      placeholder.needsComputation = true
      placeholders.add(placeholder)
      dataHolders.add(this.dataHolders[index])
    }

    _operations.add(Operation(Operation.UPDATE, position, -1, placeholders, dataHolders))
  }

  fun applyChangeset(c: ComponentContext) {
    val isTracing = isTracing

    if (nextData != null && nextData.size != placeholders.size) {
      logErrorForInconsistentSize(c)

      // Clear mPlaceholders and mOperations since they aren't matching with mNextData anymore.
      _operations.clear()
      dataHolders.clear()
      placeholders.clear()

      val prevDataHolders: MutableList<Diff<T>> = ArrayList()
      for (i in 0 until oldDataSize) {
        prevDataHolders.add(Diff(prevData?.get(i), null))
      }
      dataHolders.addAll(prevDataHolders)
      _operations.add(Operation(Operation.DELETE, 0, oldDataSize, null, prevDataHolders))

      val dataSize = nextData.size
      val placeholders: MutableList<ComponentContainer> = ArrayList(dataSize)
      val dataHolders: MutableList<Diff<T>> = ArrayList(dataSize)
      for (i in 0 until dataSize) {
        val model = nextData[i]
        if (isTracing) {
          beginSection("renderInfo:" + getModelName(model))
        }
        val renderInfo = componentRenderer.render(model, i)
        if (isTracing) {
          endSection()
        }

        placeholders.add(i, ComponentContainer(renderInfo, false))
        dataHolders.add(Diff(null, model))
      }
      this.placeholders.addAll(placeholders)
      this.dataHolders.addAll(dataHolders)
      _operations.add(Operation(Operation.INSERT, 0, -1, placeholders, dataHolders))
    } else {
      val size = placeholders.size
      for (i in 0 until size) {
        if (placeholders[i].needsComputation) {
          val model = nextData?.get(i)
          if (isTracing) {
            beginSection("renderInfo:" + getModelName(model))
          }
          placeholders[i].renderInfo = model?.let { componentRenderer.render(model, i) }
          if (isTracing) {
            endSection()
          }
          dataHolders[i].next = model
        }
      }
    }

    if (isTracing) {
      beginSection("executeOperations")
    }
    operationExecutor.executeOperations(c, _operations)
    if (isTracing) {
      endSection()
    }
  }

  /** Emit a soft error if the size between mPlaceholders and mNextData aren't the same. */
  private fun logErrorForInconsistentSize(c: ComponentContext) {
    val message = StringBuilder()
    message
        .append("Inconsistent size between mPlaceholders(")
        .append(placeholders.size)
        .append(") and mNextData(")
        .append(nextData?.size)
        .append("); ")

    message.append("mOperations: [")
    run {
      val size = _operations.size
      for (i in 0 until size) {
        val operation = _operations[i]
        message
            .append("[type=")
            .append(operation.type)
            .append(", index=")
            .append(operation.index)
            .append(", toIndex=")
            .append(operation.toIndex)
        if (operation.componentContainers != null) {
          message.append(", count=").append(operation.componentContainers.size)
        }
        message.append("], ")
      }
    }
    message.append("]; ")
    message.append("mNextData: [")
    val size = nextData?.size ?: 0
    for (i in 0 until size) {
      message.append("[").append(nextData?.get(i)).append("], ")
    }
    message.append("]")
    emitMessage(ComponentsReporter.LogLevel.ERROR, INCONSISTENT_SIZE, message.toString())
  }

  @get:VisibleForTesting
  val operations: List<Operation>
    get() = _operations

  class Operation(
      val type: Int,
      val index: Int,
      val toIndex: Int,
      val componentContainers: List<ComponentContainer>?,
      val dataContainers: List<Diff<*>>
  ) {
    companion object {
      const val INSERT: Int = 0
      const val UPDATE: Int = 1
      const val DELETE: Int = 2
      const val MOVE: Int = 3
    }
  }

  class ComponentContainer(var renderInfo: RenderInfo?, var needsComputation: Boolean)

  companion object {
    private const val INCONSISTENT_SIZE = "RecyclerBinderUpdateCallback:InconsistentSize"

    private fun getModelName(model: Any?): String? {
      if (model == null) {
        return ""
      }
      return if (model is DataDiffModelName) model.name
      else "<cls>" + model.javaClass.name + "</cls>"
    }
  }
}
