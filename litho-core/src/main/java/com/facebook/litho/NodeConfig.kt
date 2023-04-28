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

import com.facebook.litho.yoga.LithoYogaFactory
import com.facebook.yoga.YogaConfig
import com.facebook.yoga.YogaNode
import kotlin.jvm.JvmField

/** A helper class that defines a configurable sizes for Pooling. */
object NodeConfig {

  /**
   * Custom factory for Yoga nodes. Used to enable direct byte buffers to set Yoga style properties
   * (rather than JNI)
   */
  @JvmField @Volatile var yogaNodeFactory: InternalYogaNodeFactory? = null

  /** Allows access to the internal YogaConfig instance */
  @get:JvmStatic val yogaConfig: YogaConfig = LithoYogaFactory.createYogaConfig()

  @JvmStatic
  fun createYogaNode(): YogaNode {
    return yogaNodeFactory?.create(yogaConfig) ?: LithoYogaFactory.createYogaNode(yogaConfig)
  }

  /**
   * Toggles a Yoga setting on whether to print debug logs to adb.
   *
   * @param enable whether to print logs or not
   */
  @JvmStatic
  @Synchronized
  fun setPrintYogaDebugLogs(enable: Boolean) {
    yogaConfig.setPrintTreeFlag(enable)
  }

  fun interface InternalYogaNodeFactory {
    fun create(config: YogaConfig): YogaNode
  }
}
