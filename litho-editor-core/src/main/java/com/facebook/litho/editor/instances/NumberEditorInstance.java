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

import com.facebook.litho.editor.Editor;
import com.facebook.litho.editor.model.EditorNumber;
import com.facebook.litho.editor.model.EditorValue;
import java.lang.reflect.Field;

public class NumberEditorInstance<T extends Number> implements Editor {

  private final Class<T> mClazz;

  public NumberEditorInstance(final Class<T> clazz) {
    mClazz = clazz;
  }

  @Override
  public EditorValue read(Field f, Object node) {
    // If you use Number here it causes an exception when attempting to do implicit conversion
    Object n = EditorUtils.<Object>getNodeUNSAFE(f, node);
    return n == null ? EditorValue.string("null") : EditorValue.number(((Number) n).floatValue());
  }

  @Override
  public boolean write(final Field f, final Object node, final EditorValue values) {
    values.when(
        new EditorValue.DefaultEditorVisitor() {
          @Override
          public Void isNumber(EditorNumber number) {
            Number value = number.value;
            if (mClazz == int.class || mClazz == Integer.class) {
              value = value.intValue();
            } else if (mClazz == float.class || mClazz == Float.class) {
              value = value.floatValue();
            } else if (mClazz == short.class || mClazz == Short.class) {
              value = value.shortValue();
            } else if (mClazz == byte.class || mClazz == Byte.class) {
              value = value.byteValue();
            } else if (mClazz == double.class || mClazz == Double.class) {
              value = value.doubleValue();
            } else if (mClazz == long.class || mClazz == Long.class) {
              value = value.longValue();
            } // else trust the value the user has input is assignable
            // to the one expected by the field
            EditorUtils.setNodeUNSAFE(f, node, value);
            return null;
          }
        });
    return true;
  }
}
