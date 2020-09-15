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
import com.facebook.litho.editor.EditorRegistry;
import com.facebook.litho.editor.model.EditorShape;
import com.facebook.litho.editor.model.EditorValue;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

public class AtomicReferenceEditorInstance implements Editor {
  @Override
  public EditorValue read(Field f, Object node) {
    AtomicReference<Object> reference = EditorUtils.getNodeUNSAFE(f, node);
    final Object o = reference.get();
    if (o == null) {
      return EditorValue.string("null");
    }
    final Class<?> oClass = o.getClass();
    final EditorValue editorValue =
        EditorRegistry.readValueThatIsNotAField((Class<Object>) oClass, o);
    return editorValue != null ? editorValue : EditorValue.string(oClass.toString());
  }

  @Override
  public boolean write(final Field f, final Object node, final EditorValue values) {
    final AtomicReference<Object> reference = EditorUtils.getNodeUNSAFE(f, node);
    final Object o = reference.get();
    if (o == null) {
      return true;
    }
    values.when(
        new EditorValue.DefaultEditorVisitor() {
          @Override
          public Void isShape(EditorShape object) {
            EditorValue element = object.value.get(f.getName());
            final EditorRegistry.WrittenValue<Object> newValue =
                EditorRegistry.writeValueThatIsNotAField((Class<Object>) o.getClass(), o, element);
            final Boolean hasUpdated = newValue.hasUpdated();
            if (hasUpdated != null && hasUpdated) {
              reference.set(newValue.value());
            }
            return null;
          }
        });
    return true;
  }
}
