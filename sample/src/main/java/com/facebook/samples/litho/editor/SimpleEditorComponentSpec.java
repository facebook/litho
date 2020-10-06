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

package com.facebook.samples.litho.editor;

import android.graphics.Color;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.editor.Editor;
import com.facebook.litho.editor.EditorRegistry;
import com.facebook.litho.editor.SimpleEditor;
import com.facebook.litho.editor.SimpleEditor.SimpleEditorValue;
import com.facebook.litho.widget.Text;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@LayoutSpec
public class SimpleEditorComponentSpec {

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c,
      StateValue<ImmutableClass> immutableState,
      StateValue<MutableClass> mutableState,
      @Prop Integer number) {

    immutableState.set(new ImmutableClass("Imma", number * 2, Emotions.HAPPY));
    mutableState.set(new MutableClass("Muti", number / 2, Emotions.SAD));
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop Boolean bool,
      @Prop String string,
      @Prop Integer number,
      @Prop ImmutableClass immutableProp,
      @Prop MutableClass mutableProp,
      @State ImmutableClass immutableState,
      @State MutableClass mutableState) {
    return Column.create(c)
        .backgroundColor(Color.WHITE)
        .child(
            Text.create(c)
                .textSizeSp(20)
                .text(
                    "Use Flipper (fbflipper.com) to experiment\nchanging Prop and State at runtime"))
        .child(
            Text.create(c)
                .textSizeSp(12)
                .text("Check the implementation in SimpleEditorComponentSpec\n\n"))
        .child(Text.create(c).textSizeSp(16).text("PROP: VALUE"))
        .child(Text.create(c).textSizeSp(14).text("bool: " + bool))
        .child(Text.create(c).textSizeSp(14).text("string: " + string))
        .child(Text.create(c).textSizeSp(14).text("number: " + number))
        .child(Text.create(c).textSizeSp(14).text("immutable class: " + immutableProp))
        .child(Text.create(c).textSizeSp(14).text("mutable class: " + mutableProp))
        .child(Text.create(c).textSizeSp(16).text("\n\nState: VALUE"))
        .child(Text.create(c).textSizeSp(14).text("immutable class: " + immutableState))
        .child(Text.create(c).textSizeSp(14).text("mutable class: " + mutableState))
        .build();
  }

  public enum Emotions {
    HAPPY,
    SAD;

    static final Set<String> names =
        new TreeSet<String>() {
          {
            for (Emotions e : Emotions.values()) {
              add(e.name());
            }
          }
        };
  }

  static {
    // Globally register these new editors for our own classes
    // This is only necessary once per program
    //
    // Editors for basic types are registered by default
    EditorRegistry.registerEditors(
        new HashMap<Class<?>, Editor>() {
          {
            put(ImmutableClass.class, ImmutableClass.editor());
            put(MutableClass.class, MutableClass.editor());
          }
        });
  }

  public static final class ImmutableClass {
    public final Integer age;
    public final String name;
    public final Emotions emotion;

    public ImmutableClass(String name, Integer age, Emotions emotion) {
      this.age = age;
      this.name = name;
      this.emotion = emotion;
    }

    public static Editor editor() {
      return SimpleEditor.makeImmutable(
          new SimpleEditor.ImmutablePropertyEditor<ImmutableClass>() {
            @Override
            public ImmutableClass writeProperties(
                ImmutableClass value,
                Map<String, String> newStringValues,
                Map<String, Number> newNumberValues,
                Map<String, Boolean> newBoolValues,
                Map<String, String> newPickValues) {
              // Includes all properties, even those that haven't changed
              return new ImmutableClass(
                  newStringValues.get("name"),
                  newNumberValues.get("age").intValue(),
                  Emotions.valueOf(newPickValues.get("emotion")));
            }

            @Override
            public Map<String, SimpleEditorValue> readProperties(final ImmutableClass value) {
              return new HashMap<String, SimpleEditorValue>() {
                {
                  put("age", SimpleEditorValue.number(value.age));
                  put("name", SimpleEditorValue.string(value.name));
                  put("emotion", SimpleEditorValue.pick(Emotions.names, value.emotion.name()));
                }
              };
            }
          });
    }

    @Override
    public String toString() {
      return "{" + "age=" + age + ", name='" + name + '\'' + ", emotion=" + emotion + '}';
    }
  }

  public static final class MutableClass {
    public Integer age;
    public String name;
    public Emotions emotion;

    public MutableClass(String name, Integer age, Emotions emotion) {
      this.age = age;
      this.name = name;
      this.emotion = emotion;
    }

    public static Editor editor() {
      return SimpleEditor.makeMutable(
          new SimpleEditor.DefaultMutablePropertyEditor<MutableClass>() {
            @Override
            public void writePickProperty(MutableClass value, String property, String newValue) {
              if (property.contains("emotion")) {
                value.emotion = Emotions.valueOf(newValue);
              }
            }

            @Override
            public void writeStringProperty(MutableClass value, String property, String newValue) {
              if (property.contains("name")) {
                value.name = newValue;
              }
            }

            @Override
            public void writeNumberProperty(MutableClass value, String property, Number newValue) {
              if (property.contains("age")) {
                value.age = newValue.intValue();
              }
            }

            @Override
            public Map<String, SimpleEditorValue> readProperties(final MutableClass value) {
              return new HashMap<String, SimpleEditorValue>() {
                {
                  put("age", SimpleEditorValue.number(value.age));
                  put("name", SimpleEditorValue.string(value.name));
                  put("emotion", SimpleEditorValue.pick(Emotions.names, value.emotion.name()));
                }
              };
            }
          });
    }

    @Override
    public String toString() {
      return "{" + "age=" + age + ", name='" + name + '\'' + ", emotion=" + emotion + '}';
    }
  }
}
