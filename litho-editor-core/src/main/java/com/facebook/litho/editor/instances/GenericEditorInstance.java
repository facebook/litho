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

package com.facebook.litho.editor.instances;

import com.facebook.litho.editor.Editor;
import com.facebook.litho.editor.EditorRegistry;
import com.facebook.litho.editor.Reflection;
import com.facebook.litho.editor.model.EditorBool;
import com.facebook.litho.editor.model.EditorNumber;
import com.facebook.litho.editor.model.EditorString;
import com.facebook.litho.editor.model.EditorValue;
import java.lang.reflect.Field;
import java.util.HashMap;
import javax.annotation.Nullable;

public class GenericEditorInstance implements Editor {

  interface GenericEditorValueWriter {
    boolean write(Object object, Field field, Object fieldValue, EditorValue value);
  }

  @Override
  public EditorValue read(Field f, Object node) {
    final Object object = Reflection.INSTANCE.getValueUNSAFE(f, node);
    if (object == null) {
      return EditorValue.string("null");
    }

    Field[] fields = object.getClass().getDeclaredFields();
    HashMap<String, EditorValue> map = new HashMap<>();

    for (Field field : fields) {
      Object value = Reflection.INSTANCE.getValueUNSAFE(field, object);
      if (value != null) {
        EditorValue editorValue = EditorRegistry.read(value.getClass(), field, object);
        if (editorValue != null) {
          map.put(field.getName(), editorValue);
        } else {
          map.put(field.getName(), EditorValue.string(value.toString()));
        }
      } else {
        map.put(field.getName(), EditorValue.string("null"));
      }
    }

    return EditorValue.shape(map);
  }

  @Override
  public boolean write(Field f, Object node, EditorValue v) {
    final Object object = Reflection.INSTANCE.getValueUNSAFE(f, node);
    v.whenPrimitive(
        new EditorValue.DefaultEditorPrimitiveVisitor() {

          private boolean write(
              @Nullable Object obj,
              String[] path,
              EditorValue editorValue,
              GenericEditorValueWriter writer) {
            if (obj == null) {
              return false;
            }
            Class<?> clazz = obj.getClass();

            for (int i = 0; i < path.length; ++i) {
              Field field = Reflection.INSTANCE.geFieldUNSAFE(clazz, path[i]);
              if (field == null) {
                return false;
              }
              Object value = Reflection.INSTANCE.getValueUNSAFE(field, obj);
              if (value == null) {
                return false;
              }

              if (i == path.length - 1) {
                writer.write(obj, field, value, editorValue);
                return true;
              }

              clazz = value.getClass();
              obj = value;
            }

            return false;
          }

          @Override
          public boolean isBool(String[] path, EditorBool bool) {
            return this.write(
                object,
                path,
                bool,
                (obj, field, fieldValue, editorValue) -> {
                  if (fieldValue instanceof Boolean) {
                    Reflection.INSTANCE.setValueUNSAFE(
                        field, obj, ((EditorBool) editorValue).value);

                    return true;
                  }
                  return false;
                });
          }

          @Override
          public boolean isNumber(String[] path, EditorNumber number) {
            return this.write(
                object,
                path,
                number,
                (obj, field, fieldValue, editorValue) -> {
                  if (fieldValue instanceof Number) {
                    Reflection.INSTANCE.setValueUNSAFE(
                        field, obj, ((EditorNumber) editorValue).value);

                    return true;
                  }
                  return false;
                });
          }

          @Override
          public boolean isString(String[] path, EditorString string) {
            return this.write(
                object,
                path,
                string,
                (obj, field, fieldValue, editorValue) -> {
                  if (fieldValue instanceof CharSequence) {
                    Reflection.INSTANCE.setValueUNSAFE(
                        field, obj, ((EditorString) editorValue).value);

                    return true;
                  }
                  return false;
                });
          }
        });
    return true;
  }
}
