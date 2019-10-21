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

package com.facebook.litho.testing;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Helpers for dealing with reflection. Hopefully only in tests. */
public class ReflectionHelper {

  private ReflectionHelper() {}

  /**
   * Convenience version of {@link #setFinalStatic(Field, Object)}.
   *
   * @see #setFinalStatic(Field, Object)
   */
  public static void setFinalStatic(Class<?> clazz, String fieldName, Object newValue)
      throws NoSuchFieldException, IllegalAccessException {
    final Field declaredField = clazz.getDeclaredField(fieldName);
    setFinalStatic(declaredField, newValue);
  }

  /**
   * Set a static final field. N.B. This will only work if the given field <b>has not been read
   * before</b>. This is due to internal caching in the JVM for fields.
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
