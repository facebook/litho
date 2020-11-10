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

package com.facebook.samples.litho.kotlin

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.facebook.samples.litho.kotlin.demo.DataModels
import com.facebook.samples.litho.kotlin.demo.DemoListActivity
import com.facebook.samples.litho.kotlin.demo.DemoListActivity.Companion.INDICES
import com.facebook.samples.litho.kotlin.demo.DemoListDataModel
import java.util.Arrays

@SuppressLint("Registered")
open class NavigatableDemoActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    intent.getIntArrayExtra(INDICES).takeIf { it != null }?.let {
      supportActionBar?.setDisplayHomeAsUpEnabled(true)
      setTitleFromIndex(it)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean =
      if (item.itemId == android.R.id.home) {
        NavUtils.navigateUpFromSameTask(this)
        true
      } else {
        super.onOptionsItemSelected(item)
      }

  override fun getParentActivityIntent(): Intent? {
    val indices = intent.getIntArrayExtra(DemoListActivity.INDICES) ?: return null

    return Intent(this, DemoListActivity::class.java).also {
      if (indices.size > 1) {
        it.putExtra(DemoListActivity.INDICES, Arrays.copyOf(indices, indices.size - 1))
      }
    }
  }

  private fun setTitleFromIndex(indices: IntArray) {
    var dataModels: List<DemoListDataModel> = DataModels.DATA_MODELS

    for (i in 0 until indices.size - 1) {
      dataModels = dataModels[indices[i]].datamodels ?: dataModels
    }

    dataModels[indices[indices.size - 1]].also { title = it.name }
  }
}
