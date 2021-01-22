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
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerEditorInstance implements Editor {

  @Override
  public EditorValue read(Field f, Object node) {
    AtomicInteger atomicInteger = EditorUtils.getNodeUNSAFE(f, node);
    return atomicInteger == null
        ? EditorValue.string("null")
        : EditorValue.number((Integer) atomicInteger.get());
  }

  @Override
  public boolean write(final Field f, final Object node, final EditorValue values) {
    values.when(
        new EditorValue.DefaultEditorVisitor() {
          @Override
          public Void isNumber(final EditorNumber editor) {
            // If the value if non-integer, it gets rounded down.
            EditorUtils.setNodeUNSAFE(f, node, new AtomicInteger(editor.value.intValue()));
            return null;
          }
        });
    return true;
  }
}
