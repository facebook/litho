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
import com.facebook.litho.editor.model.EditorBool;
import com.facebook.litho.editor.model.EditorValue;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

public class AtomicBooleanEditorInstance implements Editor {

  @Override
  public EditorValue read(Field f, Object node) {
    AtomicBoolean atomicBoolean = EditorUtils.getNodeUNSAFE(f, node);
    return atomicBoolean == null
        ? EditorValue.string("null")
        : EditorValue.bool((Boolean) atomicBoolean.get());
  }

  @Override
  public boolean write(final Field f, final Object node, final EditorValue values) {
    values.when(
        new EditorValue.DefaultEditorVisitor() {
          @Override
          public Void isBool(final EditorBool bool) {
            EditorUtils.setNodeUNSAFE(f, node, new AtomicBoolean(bool.value));
            return null;
          }
        });
    return true;
  }
}
