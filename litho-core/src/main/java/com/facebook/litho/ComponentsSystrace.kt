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

import com.facebook.rendercore.Systracer
import com.facebook.rendercore.Systracer.ArgsBuilder

/**
 * This is intended as a hook into `android.os.Trace`, but allows you to provide your own
 * functionality. Use it as
 *
 * `ComponentsSystrace.beginSection("tag"); ... ComponentsSystrace.endSection(); ` in Java. Or as
 *
 * `ComponentsSystrace.trace("tag") { ... }` in Kotlin. As a default, it simply calls
 * `android.os.Trace` (see [DefaultComponentsSystrace]). You may supply your own with
 * [ComponentsSystrace.provide].
 */
object ComponentsSystrace {
  @JvmStatic
  var systrace: Systracer = DefaultComponentsSystrace()
    private set

  /** This should be called exactly once at app startup, before any Litho work happens. */
  @JvmStatic
  fun provide(instance: Systracer) {
    systrace = instance
  }

  /**
   * Writes a trace message to indicate that a given section of code has begun. This call must be
   * followed by a corresponding call to [.endSection] on the same thread.
   */
  @JvmStatic
  fun beginSection(name: String) {
    systrace.beginSection(name)
  }

  /**
   * Writes a trace message to indicate that a given section of code has begun. Must be followed by
   * a call to [.endAsyncSection] using the same tag. Unlike [ ][.beginSection] and [.endSection],
   * asynchronous events do not need to be nested. The name and cookie used to begin an event must
   * be used to end it.
   *
   * Depending on provided [Systracer] instance, this method could vary in behavior and in
   * [DefaultComponentsSystrace] it is a no-op.
   */
  @JvmStatic
  fun beginAsyncSection(name: String) {
    systrace.beginAsyncSection(name)
  }

  /**
   * Writes a trace message to indicate that a given section of code has begun. Must be followed by
   * a call to [.endAsyncSection] using the same tag. Unlike [ ][.beginSection] and [.endSection],
   * asynchronous events do not need to be nested. The name and cookie used to begin an event must
   * be used to end it.
   *
   * Depending on provided [Systracer] instance, this method could vary in behavior and in
   * [DefaultComponentsSystrace] it is a no-op.
   */
  @JvmStatic
  fun beginAsyncSection(name: String, cookie: Int) {
    systrace.beginAsyncSection(name, cookie)
  }

  @JvmStatic
  fun beginSectionWithArgs(name: String): ArgsBuilder {
    return systrace.beginSectionWithArgs(name)
  }

  /**
   * Writes a trace message to indicate that a given section of code has ended. This call must be
   * preceded by a corresponding call to [.beginSection]. Calling this method will mark the end of
   * the most recently begun section of code, so care must be taken to ensure that beginSection /
   * endSection pairs are properly nested and called from the same thread.
   */
  @JvmStatic
  fun endSection() {
    systrace.endSection()
  }

  /**
   * Writes a trace message to indicate that the current method has ended. Must be called exactly
   * once for each call to [.beginAsyncSection] using the same tag, name and cookie.
   *
   * Depending on provided [Systracer] instance, this method could vary in behavior and in
   * [DefaultComponentsSystrace] it is a no-op.
   */
  @JvmStatic
  fun endAsyncSection(name: String) {
    systrace.endAsyncSection(name)
  }

  /**
   * Writes a trace message to indicate that the current method has ended. Must be called exactly
   * once for each call to [.beginAsyncSection] using the same tag, name and cookie.
   *
   * Depending on provided [Systracer] instance, this method could vary in behavior and in
   * [DefaultComponentsSystrace] it is a no-op.
   */
  @JvmStatic
  fun endAsyncSection(name: String, cookie: Int) {
    systrace.endAsyncSection(name, cookie)
  }

  /**
   * A convenience Kotlin API that can be used instead of manually calling
   * [ComponentsSystrace.beginSection] and [ComponentsSystrace.endSection].
   *
   * Starts the tracing section, executes the given [tracingBlock] and ends the tracing section.
   */
  @JvmStatic
  inline fun <T> trace(name: String, tracingBlock: () -> T): T {
    try {
      if (isTracing) beginSection(name)
      return tracingBlock()
    } finally {
      if (isTracing) endSection()
    }
  }

  @JvmStatic
  val isTracing: Boolean
    @JvmStatic get() = systrace.isTracing
}
