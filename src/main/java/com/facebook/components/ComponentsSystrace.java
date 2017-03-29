/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

/**
 * This is intended as a hook into {@code android.os.Trace}, but allows you to provide your own
 * functionality.  Use it as
 * <p>
 *   {@code
 *      ComponentsSystrace.beginSection("tag");
 *      ...
 *      ComponentsSystrace.endSection();
 *   }
 * </p>
 *
 * As a default, it simply calls {@code android.os.Trace} (see {@link DefaultComponentsSystrace}).
 * You may supply your own with {@link ComponentsSystrace#provide(Systrace)}.
 */
public class ComponentsSystrace {

  private static volatile Systrace sInstance = null;

  public interface Systrace {
    void beginSection(String name);
    void endSection();
  }

  private ComponentsSystrace() {
  }

  public static void provide(Systrace instance) {
    sInstance = instance;
  }

  public static void beginSection(String name) {
