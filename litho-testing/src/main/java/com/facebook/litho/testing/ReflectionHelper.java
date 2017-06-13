/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Helpers for dealing with reflection. Hopefully only in tests.
 */
public class ReflectionHelper {

  private ReflectionHelper() {}

  /**
   * Convenience version of {@link #setFinalStatic(Field, Object)}.
   * @see #setFinalStatic(Field, Object)
   */
  public static void setFinalStatic(Class<?> clazz, String fieldName, Object newValue)
      throws NoSuchFieldException, IllegalAccessException {
    final Field declaredField = clazz.getDeclaredField(fieldName);
    setFinalStatic(declaredField, newValue);
  }

  /**
   * Set a static final field.
   * N.B. This will only work if the given field <b>has not been read before</b>. This is
   * due to internal caching in the JVM for fields.
   *
   * @throws Exception
   */
  public static void setFinalStatic(Field field, Object newValue)
      throws NoSuchFieldException, IllegalAccessException {
    field.setAccessible(true);

    // remove final modifier from field
    final Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

    field.set(null, newValue);
  }
}
