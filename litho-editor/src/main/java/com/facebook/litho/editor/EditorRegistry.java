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

package com.facebook.litho.editor;

import com.facebook.litho.editor.instances.BoolEditorInstance;
import com.facebook.litho.editor.instances.NumberEditorInstance;
import com.facebook.litho.editor.instances.StringEditorInstance;
import com.facebook.litho.editor.model.EditorValue;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A repository of known Editor instances. As Editor is not aware of the Class it is defined for,
 * this registry is meant to enforce that relationship.
 *
 * <p>When it is first loaded it will register editors for basic Java types
 *
 * @see com.facebook.litho.editor.Editor
 */
public final class EditorRegistry {

  private EditorRegistry() {}

  private static final Map<Class<?>, Editor> EDITORS = new HashMap<>();

  private static @Nullable Editor getEditor(final Class<?> c) {
    Class<?> clazz = c;
    while (true) {
      if (EDITORS.containsKey(clazz)) {
        return EDITORS.get(clazz);
      }
      final Class<?> parent = clazz.getSuperclass();
      if (parent == null) {
        break;
      }
      clazz = parent;
    }
    return null;
  }

  public static void registerEditor(final Class<?> c, final Editor e) {
    EDITORS.put(c, e);
  }

  /**
   * Reads an EditorValue for a field if there is an Editor defined for the Class parameter. Returns
   * null otherwise.
   */
  public static @Nullable EditorValue read(final Class<?> c, final Field f, final Object node) {
    final Editor editor = getEditor(c);
    if (editor == null) {
      return null;
    }
    return editor.read(f, node);
  }

  /**
   * Writes an EditorValue into a field if there is an Editor defined for the Class parameter.
   * Returns null otherwise.
   */
  public static @Nullable Boolean write(
      final Class<?> c, final Field f, final Object node, final EditorValue values) {
    final Editor editor = getEditor(c);
    if (editor == null) {
      return null;
    }
    return editor.write(f, node, values);
  }

  static {
    final NumberEditorInstance numberEditor = new NumberEditorInstance();
    registerEditor(Number.class, numberEditor);
    registerEditor(int.class, numberEditor);
    registerEditor(float.class, numberEditor);
    registerEditor(double.class, numberEditor);

    registerEditor(String.class, new StringEditorInstance());

    final BoolEditorInstance boolEditor = new BoolEditorInstance();
    registerEditor(Boolean.class, boolEditor);
    registerEditor(boolean.class, boolEditor);
  }
}
