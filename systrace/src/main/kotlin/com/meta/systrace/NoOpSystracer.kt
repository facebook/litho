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

package com.meta.systrace

class NoOpSystracer : Systracer {
  override fun beginSection(name: String) {}

  override fun beginAsyncSection(name: String) {}

  override fun beginAsyncSection(name: String, cookie: Int) {}

  override fun beginSectionWithArgs(name: String): Systracer.ArgsBuilder = NoOpArgsBuilder

  override fun endSection() {}

  override fun endAsyncSection(name: String) {}

  override fun endAsyncSection(name: String, cookie: Int) {}

  override fun isTracing(): Boolean = false
}

private object NoOpArgsBuilder : Systracer.ArgsBuilder {
  override fun flush() = Unit

  override fun arg(key: String, value: Any): Systracer.ArgsBuilder = this

  override fun arg(key: String, value: Int): Systracer.ArgsBuilder = this

  override fun arg(key: String, value: Long): Systracer.ArgsBuilder = this

  override fun arg(key: String, value: Double): Systracer.ArgsBuilder = this
}
