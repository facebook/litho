/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import androidx.lifecycle.ViewModelProviders
import com.facebook.litho.Component
import com.facebook.litho.LithoView
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.samples.litho.kotlin.NavigatableDemoActivity
import com.facebook.samples.litho.kotlin.lithography.data.DataFetcher
import com.facebook.samples.litho.kotlin.lithography.data.DecadeViewModel
import com.facebook.samples.litho.kotlin.lithography.data.Model
import com.facebook.samples.litho.kotlin.lithography.sections.LithoFeedSection

class LithographyActivity : NavigatableDemoActivity() {

  private val sectionContext: SectionContext by lazy { SectionContext(this) }
  private val lithoView: LithoView by lazy { LithoView(sectionContext) }
  private val fetcher: DataFetcher by lazy { DataFetcher(viewModel.model) }
  private val viewModel: DecadeViewModel by lazy {
    ViewModelProviders.of(this).get(DecadeViewModel::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel.model.observe(this, Observer { setList(it) })
    setContentView(lithoView)
  }

  private fun setList(model: Model?) {
    model?.let {
      if (lithoView.componentTree == null) {
        lithoView.setComponent(createRecyclerComponent(it))
      } else {
        lithoView.setComponentAsync(createRecyclerComponent(it))
      }
    }
  }

  private fun createRecyclerComponent(model: Model): Component =
      RecyclerCollectionComponent
          .create(sectionContext)
          .section(
              LithoFeedSection.create(sectionContext)
                  .decades(model.decades)
                  .fetcher(fetcher)
                  .loading(model.isLoading)
                  .build())
          .disablePTR(true)
          .build()
}
