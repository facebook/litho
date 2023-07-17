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

package com.facebook.litho.editor;

import android.graphics.drawable.ColorDrawable;
import android.util.Pair;
import com.facebook.litho.Style;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.editor.instances.AtomicBooleanEditorInstance;
import com.facebook.litho.editor.instances.AtomicIntegerEditorInstance;
import com.facebook.litho.editor.instances.AtomicReferenceEditorInstance;
import com.facebook.litho.editor.instances.BoolEditorInstance;
import com.facebook.litho.editor.instances.ColorDrawableEditorInstance;
import com.facebook.litho.editor.instances.ComparableColorDrawableEditorInstance;
import com.facebook.litho.editor.instances.GenericEditorInstance;
import com.facebook.litho.editor.instances.ListEditorInstance;
import com.facebook.litho.editor.instances.MapEditorInstance;
import com.facebook.litho.editor.instances.NumberEditorInstance;
import com.facebook.litho.editor.instances.ObjectEditorInstance;
import com.facebook.litho.editor.instances.StringEditorInstance;
import com.facebook.litho.editor.instances.StyleEditorInstance;
import com.facebook.litho.editor.instances.UtilSizeEditorInstance;
import com.facebook.litho.editor.model.EditorString;
import com.facebook.litho.editor.model.EditorValue;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
  private static boolean USE_OBJECT_EDITOR = false;

  private static @Nullable Editor getEditor(final Class<?> c) {

    for (Class<?> key : EDITORS.keySet()) {
      if (key.isAssignableFrom(c)) {
        return EDITORS.get(key);
      }
    }

    if (USE_OBJECT_EDITOR) {
      return ObjectEditorInstance.Companion.getInstance();
    }

    return null;
  }

  public static void enableObjectEditor() {
    USE_OBJECT_EDITOR = true;
  }

  public static void disableObjectEditor() {
    USE_OBJECT_EDITOR = false;
  }

  public static void registerEditor(final Class<?> c, final Editor e) {
    EDITORS.put(c, e);
  }

  public static void registerEditors(final Map<Class<?>, Editor> e) {
    EDITORS.putAll(e);
  }

  /**
   * Reads an EditorValue for a field if there is an Editor defined for the Class parameter. Returns
   * null otherwise.
   */
  public static @Nullable EditorValue read(final Class<?> c, final Field f, final Object node) {
    final Editor editor = getEditor(c);
    if (editor == null) {
      if (node instanceof TransientField) {
        return new EditorString(((TransientField<?>) node).content.toString());
      }
      Object value = Reflection.INSTANCE.getValueUNSAFE(f, node);
      String fallback = value != null ? value.toString() : "null";

      return new EditorString(fallback);
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

  /**
   * This helper gives the EditorValue of a value that is not a field of a class. If the value is a
   * field, use {@link #read(Class, Field, Object)} instead.
   *
   * @param <T> type of the value
   * @param c runtime Class of the value
   * @param value data to update
   * @return the EditorValue representation
   */
  public static @Nullable <T> EditorValue readValueThatIsNotAField(final Class<T> c, T value) {
    return read(c, TransientField.CONTENT_FIELD, new TransientField<>(value));
  }

  public interface WrittenValue<T> {
    @Nullable
    Boolean hasUpdated();

    T value();
  }

  /**
   * This helper writes an EditorValue to a value that is not a field of a class. If the value is a
   * field, use {@link #write(Class, Field, Object, EditorValue)} instead.
   *
   * @param <T> type of the value
   * @param c runtime Class of the value
   * @param value data to update
   * @param values EditorValue used to update the value
   * @return if the field has been updated correctly and the value after passing through the editor
   */
  public static <T> WrittenValue<T> writeValueThatIsNotAField(
      final Class<T> c, T value, final EditorValue values) {
    final TransientField<T> wrapper = new TransientField<>(value);
    final Boolean result = write(c, TransientField.CONTENT_FIELD, wrapper, values);
    return new WrittenValue<T>() {
      @Override
      @Nullable
      public Boolean hasUpdated() {
        return result;
      }

      @Override
      public T value() {
        return wrapper.content;
      }
    };
  }

  /**
   * This class exists for the cases where you have to update a value that is **not** a field. One
   * example are elements inside a Collection
   *
   * <p>Note that updating the whole reference immutably wouldn't affect the original value, just
   * the value of this transient class.
   *
   * @param <T>
   */
  private static final class TransientField<T> {
    public final T content;

    private TransientField(T t) {
      this.content = t;
    }

    public static final Field CONTENT_FIELD = TransientField.class.getDeclaredFields()[0];
  }

  static {
    registerEditor(int.class, new NumberEditorInstance<>(int.class));
    registerEditor(float.class, new NumberEditorInstance<>(float.class));
    registerEditor(double.class, new NumberEditorInstance<>(double.class));
    registerEditor(long.class, new NumberEditorInstance<>(long.class));
    registerEditor(short.class, new NumberEditorInstance<>(short.class));
    registerEditor(byte.class, new NumberEditorInstance<>(byte.class));
    registerEditor(Integer.class, new NumberEditorInstance<>(Integer.class));
    registerEditor(Float.class, new NumberEditorInstance<>(Float.class));
    registerEditor(Double.class, new NumberEditorInstance<>(Double.class));
    registerEditor(Long.class, new NumberEditorInstance<>(Long.class));
    registerEditor(Short.class, new NumberEditorInstance<>(Short.class));
    registerEditor(Byte.class, new NumberEditorInstance<>(Byte.class));

    registerEditor(CharSequence.class, new StringEditorInstance());
    registerEditor(Pair.class, new GenericEditorInstance());

    final BoolEditorInstance boolEditor = new BoolEditorInstance();
    registerEditor(Boolean.class, boolEditor);
    registerEditor(boolean.class, boolEditor);

    registerEditor(AtomicReference.class, new AtomicReferenceEditorInstance());

    registerEditor(AtomicBoolean.class, new AtomicBooleanEditorInstance());
    registerEditor(AtomicInteger.class, new AtomicIntegerEditorInstance());

    registerEditor(java.util.List.class, new ListEditorInstance());
    registerEditor(java.util.Map.class, MapEditorInstance.Companion.getInstance());

    registerEditor(Style.class, StyleEditorInstance.Companion.getInstance());

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      registerEditor(android.util.Size.class, new UtilSizeEditorInstance());
    }

    registerEditor(ColorDrawable.class, new ColorDrawableEditorInstance());
    registerEditor(ComparableColorDrawable.class, new ComparableColorDrawableEditorInstance());
  }
}
