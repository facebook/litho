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

import android.os.Build;
import android.os.Trace;

public final class RenderCoreSystrace {

  public interface IRenderCoreSystrace {

    void beginSection(String name);

    void beginAsyncSection(String name);

    void beginAsyncSection(String name, int cookie);

    ArgsBuilder beginSectionWithArgs(String name);

    void endSection();

    void endAsyncSection(String name);

    void endAsyncSection(String name, int cookie);

    boolean isEnabled();
  }

  /** Object that accumulates arguments for beginSectionWithArgs. */
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

  public static final ArgsBuilder NO_OP_ARGS_BUILDER = new NoOpArgsBuilder();

  private static volatile IRenderCoreSystrace sInstance = new DefaultTrace();
  private static volatile boolean sHasStarted = false;

  /**
   * Writes a trace message to indicate that a given section of code has begun. This call must be
   * followed by a corresponding call to {@link #endSection()} on the same thread.
   */
  public static void beginSection(String name) {
    sHasStarted = true;
    sInstance.beginSection(name);
  }

  /**
   * Writes a trace message to indicate that a given section of code has begun. Must be followed by
   * a call to {@link #endAsyncSection(String)} using the same tag. Unlike {@link
   * #beginSection(String)} and {@link #endSection()}, asynchronous events do not need to be nested.
   * The name and cookie used to begin an event must be used to end it.
   *
   * <p class="note">Depending on provided {@link IRenderCoreSystrace} instance, this method could
   * vary in behavior and in {@link DefaultTrace} it is a no-op.
   */
  public static void beginAsyncSection(String name) {
    sInstance.beginAsyncSection(name);
  }

  /**
   * Writes a trace message to indicate that a given section of code has begun. Must be followed by
   * a call to {@link #endAsyncSection(String, int)} using the same tag. Unlike {@link
   * #beginSection(String)} and {@link #endSection()}, asynchronous events do not need to be nested.
   * The name and cookie used to begin an event must be used to end it.
   *
   * <p class="note">Depending on provided {@link IRenderCoreSystrace} instance, this method could
   * vary in behavior and in {@link DefaultTrace} it is a no-op.
   */
  public static void beginAsyncSection(String name, int cookie) {
    sInstance.beginAsyncSection(name, cookie);
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
   * once for each call to {@link #beginAsyncSection(String)} using the same tag, name and cookie.
   *
   * <p class="note">Depending on provided {@link IRenderCoreSystrace} instance, this method could
   * vary in behavior and in {@link DefaultTrace} it is a no-op.
   */
  public static void endAsyncSection(String name) {
    sInstance.endAsyncSection(name);
  }

  /**
   * Writes a trace message to indicate that the current method has ended. Must be called exactly
   * once for each call to {@link #beginAsyncSection(String, int)} using the same tag, name and
   * cookie.
   *
   * <p class="note">Depending on provided {@link IRenderCoreSystrace} instance, this method could
   * vary in behavior and in {@link DefaultTrace} it is a no-op.
   */
  public static void endAsyncSection(String name, int cookie) {
    sInstance.endAsyncSection(name, cookie);
  }

  public static void use(IRenderCoreSystrace systraceImpl) {
    if (sHasStarted) {
      // We will not switch the implementation if the trace has already been used in the
      // app lifecycle.
      return;
    }

    sInstance = systraceImpl;
  }

  public static boolean isEnabled() {
    return sInstance.isEnabled();
  }

  private static final class DefaultTrace implements IRenderCoreSystrace {

    @Override
    public void beginSection(String name) {
      if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Trace.beginSection(name);
      }
    }

    @Override
    public void beginAsyncSection(String name) {
      // no-op
    }

    @Override
    public void beginAsyncSection(String name, int cookie) {
      // no-op
    }

    @Override
    public ArgsBuilder beginSectionWithArgs(String name) {
      beginSection(name);
      return NO_OP_ARGS_BUILDER;
    }

    @Override
    public void endSection() {
      if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Trace.endSection();
      }
    }

    @Override
    public void endAsyncSection(String name) {
      // no-op
    }

    @Override
    public void endAsyncSection(String name, int cookie) {
      // no-op
    }

    @Override
    public boolean isEnabled() {
      return BuildConfig.DEBUG
          && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
          && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Trace.isEnabled());
    }
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
