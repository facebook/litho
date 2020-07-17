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
import com.facebook.litho.editor.model.EditorString;
import com.facebook.litho.editor.model.EditorValue;
import java.lang.reflect.Field;

public class StringEditorInstance implements Editor {

  @Override
  public EditorValue read(Field f, Object node) {
    CharSequence value = EditorUtils.getNodeUNSAFE(f, node);
    value = value == null ? "null" : value;
    return EditorValue.string(value.toString());
  }

  @Override
  public boolean write(final Field f, final Object node, final EditorValue values) {
    values.whenPrimitive(
        new EditorValue.DefaultEditorPrimitiveVisitor() {
          @Override
          public boolean isString(String[] path, EditorString string) {
            EditorUtils.setNodeUNSAFE(f, node, string.value);
            return true;
          }
        });
    return true;
  }
}
