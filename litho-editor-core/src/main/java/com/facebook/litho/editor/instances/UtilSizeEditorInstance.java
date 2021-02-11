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

import android.util.Size;
import com.facebook.litho.editor.Editor;
import com.facebook.litho.editor.model.EditorString;
import com.facebook.litho.editor.model.EditorValue;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.Locale;

/**
 * An editor for android.util.Size, which is represented by the text "width=WIDTH_NUMBER
 * height=HEIGHT_NUMBER", where both numbers are integers.
 */
public class UtilSizeEditorInstance implements Editor {
  private static final String WIDTH_FIELD_STR = "width";
  private static final String HEIGHT_FIELD_STR = "height";

  @Override
  public EditorValue read(Field f, Object node) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      final Size size = EditorUtils.getNodeUNSAFE(f, node);
      if (size == null) {
        return EditorValue.string("null");
      }
      try {
        return EditorValue.string(
            String.format(
                Locale.ENGLISH,
                "%s=%d %s=%d",
                WIDTH_FIELD_STR,
                size.getWidth(),
                HEIGHT_FIELD_STR,
                size.getHeight()));
      } catch (IllegalFormatException e) {
        return EditorValue.string("null");
      }
    } else {
      return EditorValue.string("null");
    }
  }

  // Assuming 'values' is an EditorString formatted as "width=WIDTH_NUMBER height=HEIGHT_NUMBER",
  // allowing extra whitespaces between 'height', 'width', '=', and both numbers, and assuming both
  // numbers are interes, we write them as the corresponding Size object.
  // In all other cases, nothing changes.
  @Override
  public boolean write(final Field f, final Object node, final EditorValue values) {
    values.when(
        new EditorValue.DefaultEditorVisitor() {
          @Override
          public Void isString(final EditorString editor) {
            final String[] tokens = editor.value.split("=|\\s");
            final ArrayList<String> components = new ArrayList<>();
            for (final String token : tokens) {
              if (!token.isEmpty()) {
                components.add(token);
              }
            }
            if (components.size() != 4
                || !components.get(0).equals(WIDTH_FIELD_STR)
                || !components.get(2).equals(HEIGHT_FIELD_STR)) {
              return null;
            }
            try {
              final int sizeWidth = Integer.parseInt(components.get(1));
              final int sizeHeight = Integer.parseInt(components.get(3));
              if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                EditorUtils.setNodeUNSAFE(f, node, new Size(sizeWidth, sizeHeight));
              }
            } catch (NumberFormatException e) {
              // No-Op if value could not be parsed into a number.
            }
            return null;
          }
        });
    return true;
  }
}
