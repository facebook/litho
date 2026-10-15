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

package com.facebook.samples.litho.kotlin.collection

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.rangemanager.LithoCollectionItem
import com.facebook.litho.view.backgroundColor
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.meta.rangemanager.PreparableCollectionItem
import com.meta.rangemanager.RangeManager
import com.meta.rangemanager.list.ListCollectionItemProvider
import com.meta.rangemanager.list.bindToRecyclerView
import com.meta.rangemanager.ui.BindableCollectionItem

/**
 * A sample Activity demonstrating how to use RangeManager with a RecyclerView containing Litho
 * rows.
 *
 * This example shows:
 * 1. RecyclerView as the outer container with Litho-based item views (LithoView)
 * 2. Using RangeManager to pre-compute layouts for items before they become visible
 * 3. Binding pre-computed ComponentTrees to LithoViews for instant rendering
 *
 * The RangeManager prepares items ahead of and behind the visible viewport, reducing layout work on
 * the main thread during scrolling.
 */
class RangeManagerActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val items = createSampleItems()
    val itemProvider = SampleItemProvider(this, items)

    val recyclerView = RecyclerView(this)
    recyclerView.layoutManager = LinearLayoutManager(this)

    // Create RangeManager with 5 items behind and 10 items ahead of visible range.
    // Scroll events are wired automatically; destroy() is called on ON_DESTROY.
    RangeManager.bindToRecyclerView(
        itemProvider = itemProvider,
        recyclerView = recyclerView,
        lifecycleOwner = this,
    )

    val adapter = LithoRecyclerAdapter(itemProvider)
    recyclerView.adapter = adapter

    setContentView(recyclerView)
  }

  private fun createSampleItems(): List<SampleItem> {
    return (0 until 100).map { index ->
      SampleItem(
          id = index,
          title = "Item #$index",
          subtitle = "This is item number $index in the list",
          color = COLORS[index % COLORS.size],
      )
    }
  }

  companion object {
    private val COLORS = listOf(
        0xFFE3F2FD.toInt(),
        0xFFFCE4EC.toInt(),
        0xFFE8F5E9.toInt(),
        0xFFFFF3E0.toInt(),
        0xFFF3E5F5.toInt(),
    )
  }
}

/** Simple data class representing an item in the list. */
@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
internal data class SampleItem(val id: Int, val title: String, val subtitle: String, val color: Int)

/**
 * A Litho component representing a single row in the RecyclerView.
 *
 * Each row displays a title and subtitle with a colored background.
 */
internal class SampleRowComponent(
    private val item: SampleItem,
    private val style: Style = Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    return Column(style = style + Style.backgroundColor(item.color).padding(all = 16.dp)) {
      child(Text(text = item.title, textSize = 18.sp, textColor = Color.BLACK))
      child(
          Text(
              text = item.subtitle,
              textSize = 14.sp,
              textColor = Color.DKGRAY,
              style = Style.padding(top = 4.dp),
          ),
      )
    }
  }
}

/**
 * Implementation of [ListCollectionItemProvider] that provides [LithoCollectionItem] instances for
 * each position in the list.
 *
 * The RangeManager uses this provider to:
 * 1. Get items for preparation when they enter the prepare range
 * 2. Bind prepared items to views when they become visible
 *
 * Items are automatically cached by the base class, so the same instance is returned for the same
 * index until explicitly invalidated.
 */
internal class SampleItemProvider(
    private val context: Context,
    private val items: List<SampleItem>,
) : ListCollectionItemProvider() {

  private val sizeConstraints = SizeConstraints(
      minWidth = 0,
      maxWidth = context.resources.displayMetrics.widthPixels,
      minHeight = 0,
      maxHeight = SizeConstraints.Infinity,
  )

  override fun create(key: Int): PreparableCollectionItem {
    return LithoCollectionItem(
        context = context,
        dataProvider = { items[key] },
        component = { data -> SampleRowComponent(item = data) },
        sizeConstraints = sizeConstraints,
        traceTag = "SampleRow",
    )
  }

  override val size: Int
    get() = items.size
}

/**
 * RecyclerView.Adapter that uses LithoViews as item views.
 *
 * This adapter:
 * - Creates LithoView instances in onCreateViewHolder
 * - Binds pre-computed ComponentTrees to LithoViews in onBindViewHolder
 * - Unbinds when views are recycled
 */
private class LithoRecyclerAdapter(private val itemProvider: SampleItemProvider) :
    RecyclerView.Adapter<LithoViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LithoViewHolder {
    val lithoView = LithoView(parent.context)
    lithoView.layoutParams =
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    return LithoViewHolder(lithoView)
  }

  override fun onBindViewHolder(holder: LithoViewHolder, position: Int) {
    (itemProvider[position] as BindableCollectionItem).bind(holder.lithoView)
  }

  override fun onViewRecycled(holder: LithoViewHolder) {
    val position = holder.bindingAdapterPosition
    if (position != RecyclerView.NO_POSITION) {
      (itemProvider[position] as BindableCollectionItem).unbind()
    }
  }

  override fun getItemCount(): Int = itemProvider.size
}

/** ViewHolder that wraps a LithoView. */
private class LithoViewHolder(val lithoView: LithoView) : RecyclerView.ViewHolder(lithoView)
