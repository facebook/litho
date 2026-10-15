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

package com.facebook.litho.intellij.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

/** Supports storing the application settings in a persistent way. */
@State(
    name = "com.facebook.litho.intellij.settings.AppSettingsState",
    storages = [Storage("LithoIntellijPlugin.xml")],
    reloadable = true,
)
class AppSettingsState : PersistentStateComponent<AppSettingsState.Model> {
  private var model: Model = Model()

  override fun getState(): Model {
    return model
  }

  override fun loadState(model: Model) {
    this.model = model
  }

  class Model {
    @JvmField var resolveRedSymbols: Boolean = false
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): AppSettingsState {
      return ServiceManager.getService(project, AppSettingsState::class.java)
    }
  }
}
