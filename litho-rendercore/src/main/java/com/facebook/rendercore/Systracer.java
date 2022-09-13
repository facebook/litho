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

package com.facebook.rendercore;

import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public interface Systracer {

  void beginSection(String name);

  void beginAsyncSection(String name);

  void beginAsyncSection(String name, int cookie);

  ArgsBuilder beginSectionWithArgs(String name);

  void endSection();

  void endAsyncSection(String name);

  void endAsyncSection(String name, int cookie);

  boolean isTracing();

  /** Object that accumulates arguments for beginSectionWithArgs. */
  interface ArgsBuilder {

    /**
     * Write the full message to the Systrace buffer.
     *
     * <p>You must call this to log the trace message.
     */
    void flush();

    /**
     * Logs an argument whose value is any object. It will be stringified with {@link
     * String#valueOf(Object)}.
     */
    ArgsBuilder arg(String key, Object value);

    /**
     * Logs an argument whose value is an int. It will be stringified with {@link
     * String#valueOf(int)}.
     */
    ArgsBuilder arg(String key, int value);

    /**
     * Logs an argument whose value is a long. It will be stringified with {@link
     * String#valueOf(long)}.
     */
    ArgsBuilder arg(String key, long value);

    /**
     * Logs an argument whose value is a double. It will be stringified with {@link
     * String#valueOf(double)}.
     */
    ArgsBuilder arg(String key, double value);
  }
}
