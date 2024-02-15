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

package com.facebook.rendercore

import android.os.Build
import android.os.Trace
import kotlin.concurrent.Volatile

object RenderCoreSystrace {

  @JvmField val NO_OP_ARGS_BUILDER: Systracer.ArgsBuilder = NoOpArgsBuilder()

  @Volatile private var instance: Systracer = DefaultTrace()

  @Volatile private var hasStarted = false

  /**
   * Writes a trace message to indicate that a given section of code has begun. This call must be
   * followed by a corresponding call to [.endSection] on the same thread.
   */
  @JvmStatic
  fun beginSection(name: String) {
    hasStarted = true
    instance.beginSection(name)
  }

  /**
   * Writes a trace message to indicate that a given section of code has begun. Must be followed by
   * a call to [.endAsyncSection] using the same tag. Unlike [.beginSection] and [.endSection],
   * asynchronous events do not need to be nested. The name and cookie used to begin an event must
   * be used to end it.
   *
   * Depending on provided [Systracer] instance, this method could vary in behavior and in
   * [DefaultTrace] it is a no-op.
   */
  @JvmStatic
  fun beginAsyncSection(name: String) {
    instance.beginAsyncSection(name)
  }

  /**
   * Writes a trace message to indicate that a given section of code has begun. Must be followed by
   * a call to [.endAsyncSection] using the same tag. Unlike [.beginSection] and [.endSection],
   * asynchronous events do not need to be nested. The name and cookie used to begin an event must
   * be used to end it.
   *
   * Depending on provided [Systracer] instance, this method could vary in behavior and in
   * [DefaultTrace] it is a no-op.
   */
  @JvmStatic
  fun beginAsyncSection(name: String, cookie: Int) {
    instance.beginAsyncSection(name, cookie)
  }

  @JvmStatic
  fun beginSectionWithArgs(name: String): Systracer.ArgsBuilder =
      instance.beginSectionWithArgs(name)

  /**
   * Writes a trace message to indicate that a given section of code has ended. This call must be
   * preceded by a corresponding call to [.beginSection]. Calling this method will mark the end of
   * the most recently begun section of code, so care must be taken to ensure that beginSection /
   * endSection pairs are properly nested and called from the same thread.
   */
  @JvmStatic
  fun endSection() {
    instance.endSection()
  }

  /**
   * Writes a trace message to indicate that the current method has ended. Must be called exactly
   * once for each call to [.beginAsyncSection] using the same tag, name and cookie.
   *
   * Depending on provided [Systracer] instance, this method could vary in behavior and in
   * [DefaultTrace] it is a no-op.
   */
  @JvmStatic
  fun endAsyncSection(name: String) {
    instance.endAsyncSection(name)
  }

  /**
   * Writes a trace message to indicate that the current method has ended. Must be called exactly
   * once for each call to [.beginAsyncSection] using the same tag, name and cookie.
   *
   * Depending on provided [Systracer] instance, this method could vary in behavior and in
   * [DefaultTrace] it is a no-op.
   */
  @JvmStatic
  fun endAsyncSection(name: String, cookie: Int) {
    instance.endAsyncSection(name, cookie)
  }

  @JvmStatic
  fun use(systraceImpl: Systracer) {
    if (hasStarted) {
      // We will not switch the implementation if the trace has already been used in the
      // app lifecycle.
      return
    }
    instance = systraceImpl
  }

  @JvmStatic fun getInstance(): Systracer = instance

  @JvmStatic fun isTracing(): Boolean = instance.isTracing()

  private class DefaultTrace : Systracer {
    override fun beginSection(name: String) {
      if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Trace.beginSection(name)
      }
    }

    override fun beginAsyncSection(name: String) {
      // no-op
    }

    override fun beginAsyncSection(name: String, cookie: Int) {
      // no-op
    }

    override fun beginSectionWithArgs(name: String): Systracer.ArgsBuilder {
      beginSection(name)
      return NO_OP_ARGS_BUILDER
    }

    override fun endSection() {
      if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Trace.endSection()
      }
    }

    override fun endAsyncSection(name: String) {
      // no-op
    }

    override fun endAsyncSection(name: String, cookie: Int) {
      // no-op
    }

    override fun isTracing(): Boolean =
        BuildConfig.DEBUG &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Trace.isEnabled())
  }

  private class NoOpArgsBuilder : Systracer.ArgsBuilder {
    override fun flush() = Unit

    override fun arg(key: String, value: Any): Systracer.ArgsBuilder = this

    override fun arg(key: String, value: Int): Systracer.ArgsBuilder = this

    override fun arg(key: String, value: Long): Systracer.ArgsBuilder = this

    override fun arg(key: String, value: Double): Systracer.ArgsBuilder = this
  }
}
