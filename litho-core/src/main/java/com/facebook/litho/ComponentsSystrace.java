/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho;

/**
 * This is intended as a hook into {@code android.os.Trace}, but allows you to provide your own
 * functionality. Use it as
 *
 * <p>{@code ComponentsSystrace.beginSection("tag"); ... ComponentsSystrace.endSection(); } As a
 * default, it simply calls {@code android.os.Trace} (see {@link DefaultComponentsSystrace}). You
 * may supply your own with {@link ComponentsSystrace#provide(Systrace)}.
 */
public class ComponentsSystrace {

  /** Convenience implementation of ArgsBuilder to use when we aren't tracing. */
  public static final ArgsBuilder NO_OP_ARGS_BUILDER = new NoOpArgsBuilder();

  private static Systrace sInstance = new DefaultComponentsSystrace();

  public interface Systrace {
    void beginSection(String name);

    void beginSectionAsync(String name);

    void beginSectionAsync(String name, int cookie);

    ArgsBuilder beginSectionWithArgs(String name);

    void endSection();

    void endSectionAsync(String name);

    void endSectionAsync(String name, int cookie);

    boolean isTracing();
  }

  /** Object that accumulates arguments. */
  public interface ArgsBuilder {

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

  private ComponentsSystrace() {}

  /** This should be called exactly once at app startup, before any Litho work happens. */
  public static void provide(Systrace instance) {
    sInstance = instance;
  }

  /**
   * Writes a trace message to indicate that a given section of code has begun. This call must be
   * followed by a corresponding call to {@link #endSection()} on the same thread.
   */
  public static void beginSection(String name) {
    sInstance.beginSection(name);
  }

  /**
   * Writes a trace message to indicate that a given section of code has begun. Must be followed by
   * a call to {@link #endSectionAsync(String)} using the same tag. Unlike {@link
   * #beginSection(String)} and {@link #endSection()}, asynchronous events do not need to be nested.
   * The name and cookie used to begin an event must be used to end it.
   *
   * <p class="note">Depending on provided {@link Systrace} instance, this method could vary in
   * behavior and in {@link DefaultComponentsSystrace} it is a no-op.
   */
  public static void beginSectionAsync(String name) {
    sInstance.beginSectionAsync(name);
  }

  /**
   * Writes a trace message to indicate that a given section of code has begun. Must be followed by
   * a call to {@link #endSectionAsync(String, int)} using the same tag. Unlike {@link
   * #beginSection(String)} and {@link #endSection()}, asynchronous events do not need to be nested.
   * The name and cookie used to begin an event must be used to end it.
   *
   * <p class="note">Depending on provided {@link Systrace} instance, this method could vary in
   * behavior and in {@link DefaultComponentsSystrace} it is a no-op.
   */
  public static void beginSectionAsync(String name, int cookie) {
    sInstance.beginSectionAsync(name, cookie);
  }

  public static ArgsBuilder beginSectionWithArgs(String name) {
    return sInstance.beginSectionWithArgs(name);
  }

  /**
   * Writes a trace message to indicate that a given section of code has ended. This call must be
   * preceded by a corresponding call to {@link #beginSection(String)}. Calling this method will
   * mark the end of the most recently begun section of code, so care must be taken to ensure that
   * beginSection / endSection pairs are properly nested and called from the same thread.
   */
  public static void endSection() {
    sInstance.endSection();
  }

  /**
   * Writes a trace message to indicate that the current method has ended. Must be called exactly
   * once for each call to {@link #beginSectionAsync(String)} using the same tag, name and cookie.
   *
   * <p class="note">Depending on provided {@link Systrace} instance, this method could vary in
   * behavior and in {@link DefaultComponentsSystrace} it is a no-op.
   */
  public static void endSectionAsync(String name) {
    sInstance.endSectionAsync(name);
  }

  /**
   * Writes a trace message to indicate that the current method has ended. Must be called exactly
   * once for each call to {@link #beginSectionAsync(String, int)} using the same tag, name and
   * cookie.
   *
   * <p class="note">Depending on provided {@link Systrace} instance, this method could vary in
   * behavior and in {@link DefaultComponentsSystrace} it is a no-op.
   */
  public static void endSectionAsync(String name, int cookie) {
    sInstance.endSectionAsync(name, cookie);
  }

  public static boolean isTracing() {
    return sInstance.isTracing();
  }

  private static final class NoOpArgsBuilder implements ArgsBuilder {

    @Override
    public void flush() {}

    @Override
    public ArgsBuilder arg(String key, Object value) {
      return this;
    }

    @Override
    public ArgsBuilder arg(String key, int value) {
      return this;
    }

    @Override
    public ArgsBuilder arg(String key, long value) {
      return this;
    }

    @Override
    public ArgsBuilder arg(String key, double value) {
      return this;
    }
  }
}
