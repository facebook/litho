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

import com.facebook.litho.editor.model.EditorValue;
import java.lang.reflect.Field;

/**
 * A class to operate on Component fields from an external editor such as Flipper.
 *
 * <p>It is meant to be implemented for a single class. This pairing Class <-> Implementation cannot
 * be done in plain Java, so we rely on the implementor to pair it with its corresponding class in a
 * Map such as the one in the EditorRegistry.
 *
 * @see com.facebook.litho.editor.EditorRegistry
 */
public interface Editor {
  /**
   * Receives the instance of the corresponding field, and expects the implementor to return an
   * EditorValue representation of it. This structure will be replicated for the values received by
   * {@link Editor#write(Field, Object, EditorValue)}
   */
  EditorValue read(Field f, Object node);

  /**
   * Receives the instance of the corresponding field, and one or multiple values following the
   * structure that was given in {@link Editor#read(Field, Object)}. Those values are meant to be
   * applied back to the corresponding field.
   */
  boolean write(Field f, Object node, EditorValue values);
}
