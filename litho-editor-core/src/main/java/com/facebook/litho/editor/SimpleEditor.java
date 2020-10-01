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

import com.facebook.litho.editor.instances.EditorUtils;
import com.facebook.litho.editor.model.EditorArray;
import com.facebook.litho.editor.model.EditorBool;
import com.facebook.litho.editor.model.EditorNumber;
import com.facebook.litho.editor.model.EditorPick;
import com.facebook.litho.editor.model.EditorShape;
import com.facebook.litho.editor.model.EditorString;
import com.facebook.litho.editor.model.EditorValue;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

public final class SimpleEditor {

  private SimpleEditor() {}

  // Constructors

  public static <T> Editor makeMutable(final MutablePropertyEditor<T> propertyEditor) {
    return new Editor() {
      @Override
      public EditorValue read(Field f, Object node) {
        return makeValue(f, node, propertyEditor);
      }

      @Override
      public boolean write(final Field f, final Object node, final EditorValue values) {
        final T value = EditorUtils.getNodeUNSAFE(f, node);
        values.when(
            new EditorValue.DefaultEditorVisitor() {
              @Override
              public Void isShape(EditorShape object) {
                final EditorValue editorValue = object.value.get(f.getName());
                return editorValue != null
                    ? editorValue.when(writeMutable(propertyEditor, value))
                    : null;
              }
            });
        return true;
      }
    };
  }

  public static <T> Editor makeImmutable(final ImmutablePropertyEditor<T> propertyEditor) {
    return new Editor() {
      @Override
      public EditorValue read(Field f, Object node) {
        return makeValue(f, node, propertyEditor);
      }

      @Override
      public boolean write(final Field f, final Object node, final EditorValue values) {
        final T value = EditorUtils.getNodeUNSAFE(f, node);
        values.when(
            new EditorValue.DefaultEditorVisitor() {
              @Override
              public Void isShape(EditorShape object) {
                return object
                    .value
                    .get(f.getName())
                    .when(writeImmutable(f, node, propertyEditor, value));
              }
            });
        return true;
      }
    };
  }

  // Read data

  private static <T> EditorValue makeValue(Field f, Object node, PropertyReader<T> propertyReader) {
    T value = EditorUtils.getNodeUNSAFE(f, node);
    final Map<String, SimpleEditorValue> properties = propertyReader.readProperties(value);
    Map<String, EditorValue> shape = new HashMap<>();
    for (Map.Entry<String, SimpleEditorValue> property : properties.entrySet()) {
      shape.put(property.getKey(), property.getValue().value);
    }
    return EditorValue.shape(shape);
  }

  // Write data

  private static <T> EditorValue.DefaultEditorVisitor writeMutable(
      final MutablePropertyEditor<T> propertyEditor, final T value) {
    return new EditorValue.DefaultEditorVisitor() {
      @Override
      public Void isShape(EditorShape shape) {
        final Map<String, SimpleEditorValue> properties = propertyEditor.readProperties(value);
        final Map<String, EditorValue> updates = shape.value;
        for (final Map.Entry<String, EditorValue> updatedProperty : updates.entrySet()) {
          final SimpleEditorValue newValue =
              SimpleEditorValue.fromEditorValueOrNull(updatedProperty.getValue());
          final SimpleEditorValue oldValue = properties.get(updatedProperty.getKey());
          if (newValue != null && oldValue != null && newValue.type == oldValue.type) {
            newValue.value.whenPrimitive(
                new EditorValue.EditorPrimitiveVisitor() {
                  @Override
                  public boolean isNumber(String[] path, EditorNumber number) {
                    propertyEditor.writeNumberProperty(
                        value, updatedProperty.getKey(), number.value);
                    return true;
                  }

                  @Override
                  public boolean isString(String[] path, EditorString string) {
                    propertyEditor.writeStringProperty(
                        value, updatedProperty.getKey(), string.value);
                    return false;
                  }

                  @Override
                  public boolean isBool(String[] path, EditorBool bool) {
                    propertyEditor.writeBoolProperty(value, updatedProperty.getKey(), bool.value);
                    return false;
                  }

                  @Override
                  public boolean isPick(String[] path, EditorPick pick) {
                    propertyEditor.writePickProperty(
                        value, updatedProperty.getKey(), pick.selected);
                    return false;
                  }
                });
          }
        }
        return null;
      }
    };
  }

  private static <T> EditorValue.EditorVisitor<Void> writeImmutable(
      final Field f,
      final Object node,
      final ImmutablePropertyEditor<T> propertyEditor,
      final T value) {
    return new EditorValue.DefaultEditorVisitor() {
      @Override
      public Void isShape(EditorShape shape) {
        final Map<String, SimpleEditorValue> properties = propertyEditor.readProperties(value);
        final Map<String, String> stringProperties = new HashMap<>();
        final Map<String, Number> numberProperties = new HashMap<>();
        final Map<String, Boolean> boolProperties = new HashMap<>();
        final Map<String, String> pickProperties = new HashMap<>();
        for (final Map.Entry<String, SimpleEditorValue> property : properties.entrySet()) {
          property
              .getValue()
              .value
              .whenPrimitive(
                  classifyValue(
                      property.getKey(),
                      stringProperties,
                      numberProperties,
                      boolProperties,
                      pickProperties));
        }
        final Map<String, EditorValue> updates = shape.value;
        for (Map.Entry<String, EditorValue> updatedProperty : updates.entrySet()) {
          final SimpleEditorValue newValue =
              SimpleEditorValue.fromEditorValueOrNull(updatedProperty.getValue());
          final String propertyKey = updatedProperty.getKey();
          final SimpleEditorValue oldValue = properties.get(propertyKey);
          if (newValue != null && oldValue != null && newValue.type == oldValue.type) {
            newValue.value.whenPrimitive(
                classifyValue(
                    propertyKey,
                    stringProperties,
                    numberProperties,
                    boolProperties,
                    pickProperties));
          }
        }
        final T newValue =
            propertyEditor.writeProperties(
                value, stringProperties, numberProperties, boolProperties, pickProperties);
        EditorUtils.setNodeUNSAFE(f, node, newValue);
        return null;
      }
    };
  }

  private static EditorValue.EditorPrimitiveVisitor classifyValue(
      final String key,
      final Map<String, String> stringProperties,
      final Map<String, Number> numberProperties,
      final Map<String, Boolean> boolProperties,
      final Map<String, String> pickProperties) {
    return new EditorValue.EditorPrimitiveVisitor() {
      @Override
      public boolean isNumber(String[] path, EditorNumber number) {
        numberProperties.put(key, number.value);
        return false;
      }

      @Override
      public boolean isString(String[] path, EditorString string) {
        stringProperties.put(key, string.value);
        return false;
      }

      @Override
      public boolean isBool(String[] path, EditorBool bool) {
        boolProperties.put(key, bool.value);
        return false;
      }

      @Override
      public boolean isPick(String[] path, EditorPick pick) {
        pickProperties.put(key, pick.selected);
        return false;
      }
    };
  }

  // Types

  public interface PropertyReader<T> {
    Map<String, SimpleEditorValue> readProperties(T value);
  }

  public interface MutablePropertyEditor<T> extends PropertyReader<T> {
    void writeStringProperty(T value, String property, String newValue);

    void writeNumberProperty(T value, String property, Number newValue);

    void writeBoolProperty(T value, String property, boolean newValue);

    void writePickProperty(T value, String property, String newValue);
  }

  public abstract static class DefaultMutablePropertyEditor<T> implements MutablePropertyEditor<T> {

    @Override
    public void writeStringProperty(T value, String property, String newValue) {}

    @Override
    public void writeNumberProperty(T value, String property, Number newValue) {}

    @Override
    public void writeBoolProperty(T value, String property, boolean newValue) {}
  }

  public interface ImmutablePropertyEditor<T> extends PropertyReader<T> {
    T writeProperties(
        T value,
        Map<String, String> newStringValues,
        Map<String, Number> newNumberValues,
        Map<String, Boolean> newBoolValues,
        Map<String, String> newPickValues);
  }

  private static final int PRIMITIVE_TYPE_NUMBER = 0;
  private static final int PRIMITIVE_TYPE_STRING = 1;
  private static final int PRIMITIVE_TYPE_BOOL = 2;
  private static final int PRIMITIVE_TYPE_PICK = 3;

  /** A class that wraps either a number, string or bool */
  public static final class SimpleEditorValue {

    public final EditorValue value;

    public final int type;

    private SimpleEditorValue(EditorValue value, int type) {
      this.value = value;
      this.type = type;
    }

    // Constructors

    public static SimpleEditorValue number(Number n) {
      return new SimpleEditorValue(new EditorNumber(n), PRIMITIVE_TYPE_NUMBER);
    }

    public static SimpleEditorValue string(String s) {
      return new SimpleEditorValue(new EditorString(s), PRIMITIVE_TYPE_STRING);
    }

    public static SimpleEditorValue bool(boolean b) {
      return new SimpleEditorValue(new EditorBool(b), PRIMITIVE_TYPE_BOOL);
    }

    public static SimpleEditorValue pick(Set<String> otherValues, String selected) {
      return new SimpleEditorValue(new EditorPick(otherValues, selected), PRIMITIVE_TYPE_PICK);
    }

    public static @Nullable SimpleEditorValue fromEditorValueOrNull(EditorValue v) {
      return v.when(asPrimitive);
    }

    private static final EditorValue.EditorVisitor</*@Nullable*/ SimpleEditorValue> asPrimitive =
        new EditorValue.EditorVisitor</*@Nullable*/ SimpleEditorValue>() {
          @Override
          public @Nullable SimpleEditorValue isShape(EditorShape object) {
            return null;
          }

          @Override
          public @Nullable SimpleEditorValue isArray(EditorArray array) {
            return null;
          }

          @Override
          public @Nullable SimpleEditorValue isPick(EditorPick pick) {
            return SimpleEditorValue.pick(pick.values, pick.selected);
          }

          @Override
          public @Nullable SimpleEditorValue isNumber(EditorNumber number) {
            return SimpleEditorValue.number(number.value);
          }

          @Override
          public @Nullable SimpleEditorValue isString(EditorString string) {
            return SimpleEditorValue.string(string.value);
          }

          @Override
          public @Nullable SimpleEditorValue isBool(EditorBool bool) {
            return SimpleEditorValue.bool(bool.value);
          }
        };
  }
}
