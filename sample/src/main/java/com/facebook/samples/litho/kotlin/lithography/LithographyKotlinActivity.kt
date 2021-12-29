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

package com.facebook.samples.litho.kotlin.lithography

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.samples.litho.NavigatableDemoActivity
import com.facebook.samples.litho.kotlin.lithography.data.DataFetcher
import com.facebook.samples.litho.kotlin.lithography.data.DecadeViewModel
import com.facebook.samples.litho.kotlin.lithography.data.Model
import com.facebook.samples.litho.kotlin.lithography.sections.LithoFeedSection
import kotlin.LazyThreadSafetyMode.NONE

class LithographyKotlinActivity : NavigatableDemoActivity() {

  private val c by lazy(NONE) { ComponentContext(this) }
  private val lithoView by lazy(NONE) { LithoView(c) }
  private val fetcher by lazy(NONE) { DataFetcher(viewModel.model) }
  private val viewModel by lazy(NONE) { ViewModelProvider(this).get(DecadeViewModel::class.java) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel.model.observe(this, Observer { setList(it) })
    setContentView(lithoView)
  }

  private fun setList(model: Model?) {
    model?.let { lithoView.setComponentAsync(createRecyclerComponent(it)) }
  }

  private fun createRecyclerComponent(model: Model): Component =
      RecyclerCollectionComponent.create(c)
          .section(
              LithoFeedSection.create(SectionContext(c))
                  .decades(model.decades)
                  .fetcher(fetcher)
                  .loading(model.isLoading)
                  .build())
          .disablePTR(true)
          .build()
}
