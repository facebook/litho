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

package com.facebook.litho.codelab

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.LithoView
import com.facebook.litho.TreeState

class MainActivity : AppCompatActivity() {

  var mComponentTree: ComponentTree? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val treeStateViewModel = ViewModelProvider(this).get(TreeStateViewModel::class.java)

    val componentContext = ComponentContext(this)

    /**
     * When creating the ComponentTree, pass it the TreeState that you saved before the app
     * configuration changed. This will restore the state value.
     */
    mComponentTree =
        ComponentTree.create(componentContext, RootComponent.create(componentContext).build())
            .treeState(treeStateViewModel.getTreeState())
            .build()

    val lithoView = LithoView(componentContext)
    lithoView.setComponentTree(mComponentTree)

    setContentView(lithoView)
  }

  override fun onDestroy() {
    super.onDestroy()

    val treeStateViewModel = ViewModelProvider(this).get(TreeStateViewModel::class.java)

    /**
     * Before destroying the activity, save the TreeState so we can restore the state value after
     * the configuration change.
     */
    treeStateViewModel.updateTreeState(mComponentTree)
  }

  class TreeStateViewModel : ViewModel() {
    val treeStateData: MutableLiveData<TreeState> = MutableLiveData<TreeState>()

    fun getTreeState(): TreeState? {
      return treeStateData.getValue()
    }

    fun updateTreeState(componentTree: ComponentTree?) {
      if (componentTree != null) {
        /**
         * The current state values are wrapped in a TreeState that lives on the ComponentTree. call
         * acquireTreeState to obtain a copy.
         */
        treeStateData.setValue(componentTree.acquireTreeState())
      }
    }
  }
}
