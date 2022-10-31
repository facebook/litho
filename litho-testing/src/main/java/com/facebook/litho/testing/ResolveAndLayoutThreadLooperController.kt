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

package com.facebook.litho.testing

import android.os.Looper
import com.facebook.litho.ComponentTree
import org.robolectric.Shadows

class ResolveAndLayoutThreadLooperController : BaseThreadLooperController {

  private lateinit var resolveController: ThreadLooperController
  private lateinit var layoutController: ThreadLooperController

  private var isInitialized = false

  override fun init() {
    resolveController =
        ThreadLooperController(
            Shadows.shadowOf(
                Whitebox.invokeMethod<Any>(
                    ComponentTree::class.java, "getDefaultResolveThreadLooper") as Looper))
    layoutController = ThreadLooperController() // default layout thread thread looper

    resolveController.init()
    layoutController.init()

    isInitialized = true
  }

  override fun clean() {
    if (!isInitialized) {
      return
    }

    resolveController.clean()
    layoutController.clean()

    isInitialized = false
  }

  override fun runOneTaskSync() {
    resolveController.runOneTaskSync()
    layoutController.runOneTaskSync()
  }

  override fun runToEndOfTasksSync() {
    resolveController.runToEndOfTasksSync()
    layoutController.runToEndOfTasksSync()
  }

  override fun runToEndOfTasksAsync(): TimeOutSemaphore? {
    resolveController.runToEndOfTasksAsync()
    return layoutController.runToEndOfTasksAsync()
  }
}
