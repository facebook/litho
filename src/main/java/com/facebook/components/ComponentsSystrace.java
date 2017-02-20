// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

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
    getInstance().beginSection(name);
  }

  public static void endSection() {
    getInstance().endSection();
  }

  private static Systrace getInstance() {
    if (sInstance == null) {
      synchronized (ComponentsSystrace.class) {
        if (sInstance == null) {
          sInstance = new DefaultComponentsSystrace();
        }
      }
    }
    return sInstance;
  }
}
