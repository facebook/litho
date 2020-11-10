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

package com.facebook.litho.codelab

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.LithoView
import com.facebook.litho.StateHandler

class MainActivity : AppCompatActivity() {

  var mComponentTree: ComponentTree? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val mStateHandlerViewModel = ViewModelProviders.of(this).get(StateHandlerViewModel::class.java)

    val componentContext = ComponentContext(this)

    /**
     * When creating the ComponentTree, pass it the StateHandler that you saved before the app
     * configuration changed. This will restore the state value.
     */
    mComponentTree =
        ComponentTree.create(componentContext, RootComponent.create(componentContext).build())
            .stateHandler(mStateHandlerViewModel.getStateHandler())
            .build()

    val lithoView = LithoView(componentContext)
    lithoView.setComponentTree(mComponentTree)

    setContentView(lithoView)
  }

  override fun onDestroy() {
    super.onDestroy()

    val mStateHandlerViewModel = ViewModelProviders.of(this).get(StateHandlerViewModel::class.java)

    /**
     * Before destroying the activity, save the StateHandler so we can restore the state value after
     * the configuration change.
     */
    mStateHandlerViewModel.updateStateHandler(mComponentTree)
  }

  class StateHandlerViewModel : ViewModel() {
    val stateHandlerData: MutableLiveData<StateHandler> = MutableLiveData<StateHandler>()

    fun getStateHandler(): StateHandler? {
      return stateHandlerData.getValue()
    }

    fun updateStateHandler(componentTree: ComponentTree?) {
      if (componentTree != null) {
        /**
         * The current state values are wrapped in a StateHandler that lives on the ComponentTree.
         * call acquireStateHandler to obtain a copy.
         */
        stateHandlerData.setValue(componentTree.acquireStateHandler())
      }
    }
  }
}
