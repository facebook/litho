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

package com.facebook.litho.k1

import com.facebook.litho.k1.diagnostics.LithoFeHookUsageChecker
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.registerInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * A K1-based extension responsible for contributing additional checkers, among others, that will be
 * called when the compiler checks code.
 *
 * @see [LithoFeHookUsageChecker]
 */
class LithoFeCheckersContributor : StorageComponentContainerContributor {

  override fun registerModuleComponents(
      container: StorageComponentContainer,
      platform: TargetPlatform,
      moduleDescriptor: ModuleDescriptor
  ) {
    container.registerInstance(LithoFeHookUsageChecker())
  }
}
