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

package com.facebook.litho.k2

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/**
 * Responsible for registering all FIR specific extensions for Litho to the compiler.
 *
 * It exposes a shorthand API that makes it easy to register extensions correctly. The API
 * implicitly passes an [FirSession] object to the extension constructor, and also provides
 * mechanisms to pass explicit dependencies as necessary.
 *
 * @see [LithoFirCheckersExtension]
 */
class LithoFirExtensionRegistrar : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +::LithoFirCheckersExtension
  }
}
