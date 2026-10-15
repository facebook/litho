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

package com.facebook.litho.dataflow.springs

/**
 * Data structure for storing spring configuration.
 *
 * This code was forked from the facebook/rebound repository.
 */
class SpringConfig(
    @JvmField var tension: Double,
    @JvmField var friction: Double,
) {

  companion object {
    const val DEFAULT_TENSION: Double = 230.2
    const val DEFAULT_FRICTION: Double = 22.0

    // Taken from the default Quartz Composer spring config of tension=40, friction=7 (in QC units)
    @JvmField var defaultConfig: SpringConfig = SpringConfig(DEFAULT_TENSION, DEFAULT_FRICTION)

    // Taken from the default Quartz Composer spring config of tension=70, friction=11 (in QC units)
    @JvmField var noOvershootConfig: SpringConfig = SpringConfig(338.8, 34.0)
  }
}
