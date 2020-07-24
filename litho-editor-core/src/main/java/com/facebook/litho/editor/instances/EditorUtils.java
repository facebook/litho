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

package com.facebook.litho.editor.instances;

import java.lang.reflect.Field;

public final class EditorUtils {
  private EditorUtils() {}

  @SuppressWarnings("unchecked")
  public static <T> T getNodeUNSAFE(Field f, Object node) {
    try {
      boolean oldAccessibility = f.isAccessible();
      f.setAccessible(true);
      T value = (T) f.get(node);
      f.setAccessible(oldAccessibility);
      return value;
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> void setNodeUNSAFE(Field f, Object node, T value) {
    try {
      boolean oldAccessibility = f.isAccessible();
      f.setAccessible(true);
      f.set(node, value);
      f.setAccessible(oldAccessibility);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    }
  }
}
