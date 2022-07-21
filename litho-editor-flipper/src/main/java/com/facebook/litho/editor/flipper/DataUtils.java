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

package com.facebook.litho.editor.flipper;

import static com.facebook.flipper.plugins.inspector.InspectorValue.Type.Color;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import com.facebook.flipper.core.FlipperObject;
import com.facebook.flipper.plugins.inspector.InspectorValue;
import com.facebook.flipper.plugins.inspector.Named;
import com.facebook.litho.SpecGeneratedComponent;
import com.facebook.litho.StateContainer;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.drawable.ComparableColorDrawable;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class DataUtils {

  static List<Named<FlipperObject>> getPropData(Object node) {
    final FlipperObject.Builder props = new FlipperObject.Builder();
    List<Named<FlipperObject>> data = new ArrayList<>();

    boolean hasProps = false;

    boolean isSpecComponent = node instanceof SpecGeneratedComponent;

    for (Field f : node.getClass().getDeclaredFields()) {
      f.setAccessible(true);

      Object prop;
      try {
        prop = f.get(node);
      } catch (IllegalAccessException e) {
        continue;
      }
      String propName = f.getName();

      final Prop annotation = f.getAnnotation(Prop.class);
      if (isSpecComponent && annotation == null) {
        // Only expose `@Prop` annotated fields for Spec components
        continue;
      }

      hasProps = true;

      if (prop != null && PropWithInspectorSection.class.isAssignableFrom(prop.getClass())) {
        final AbstractMap.SimpleEntry<String, String> datum =
            ((PropWithInspectorSection) prop).getFlipperLayoutInspectorSection();
        if (datum != null) {
          data.add(new Named<>(datum.getKey(), new FlipperObject(datum.getValue())));
        }
      }

      if (annotation != null) {
        ResType resType = annotation.resType();
        if (resType == ResType.COLOR) {
          props.put(propName, prop == null ? "null" : fromColor((Integer) prop));
          continue;
        } else if (resType == ResType.DRAWABLE) {
          props.put(propName, prop == null ? "null" : fromDrawable((Drawable) prop));
          continue;
        }
      }

      if (prop != null && PropWithDescription.class.isAssignableFrom(prop.getClass())) {
        final Object description =
            ((PropWithDescription) prop).getFlipperLayoutInspectorPropDescription();
        // Treat the description as immutable for now, because it's a "translation" of the
        // actual prop,
        // mutating them is not going to change the original prop.
        if (description instanceof Map<?, ?>) {
          final Map<?, ?> descriptionMap = (Map<?, ?>) description;
          for (Map.Entry<?, ?> entry : descriptionMap.entrySet()) {
            props.put(entry.getKey().toString(), InspectorValue.immutable(entry.getValue()));
          }
        } else {
          props.put(propName, InspectorValue.immutable(description));
        }
        continue;
      }

      props.put(propName, FlipperEditor.makeFlipperField(node, f));
    }

    if (hasProps) {
      data.add(new Named<>("Props", props.build()));
    }

    return data;
  }

  @Nullable
  static FlipperObject getStateData(StateContainer stateContainer) {
    if (stateContainer == null) {
      return null;
    }

    final FlipperObject.Builder state = new FlipperObject.Builder();

    boolean hasState = false;
    for (Field f : stateContainer.getClass().getDeclaredFields()) {
      f.setAccessible(true);

      final State annotation = f.getAnnotation(State.class);
      if (annotation != null) {
        state.put(f.getName(), FlipperEditor.makeFlipperField(stateContainer, f));
        hasState = true;
      }
    }

    return hasState ? state.build() : null;
  }

  static InspectorValue fromDrawable(Drawable d) {
    int color = 0;
    if (d instanceof ColorDrawable) {
      color = ((ColorDrawable) d).getColor();
    } else if (d instanceof ComparableColorDrawable) {
      color = ((ComparableColorDrawable) d).getColor();
    }
    return InspectorValue.mutable(Color, color);
  }

  static InspectorValue fromColor(int color) {
    return InspectorValue.mutable(Color, color);
  }
}
