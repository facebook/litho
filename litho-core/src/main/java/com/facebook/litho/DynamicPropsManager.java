/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Takes care of dynamic Props
 *
 * <p>Usage from {@link MountState}
 *
 * <p>Unique per {@link MountState} instance, which calls {@link
 * #onBindComponentToContent(Component, Object)} and {#link {@link #onUnbindComponent(Component)}}
 * when it binds and unbinds Components respectively. When Component is being bound, a
 * DynamicPropsManager subscribes to {@link DynamicValue}s that its represent dynamic props, and
 * keeps the mounted content the Components is rendered into in sync with {@link
 * DynamicValue#mValue} until the Component gets unbound, at which point, the DynamicPropsManager
 * unsubscribes from the DynamicValues.
 */
class DynamicPropsManager implements DynamicValue.OnValueChangeListener {
  private final Map<DynamicValue<?>, Set<Component>> mDependentComponents = new HashMap<>();
  private final Map<Component, Set<DynamicValue<?>>> mAffectingDynamicValues = new HashMap<>();
  private final Map<Component, Object> mContents = new HashMap<>();

  void onBindComponentToContent(Component component, Object content) {
    final DynamicValue[] dynamicProps = component.getDynamicProps();

    if (dynamicProps.length == 0) {
      return;
    }

    final Set<DynamicValue<?>> dynamicValues = new HashSet<>();

    // Go through all the other dynamic props
    for (int i = 0; i < dynamicProps.length; i++) {
      final DynamicValue<?> value = dynamicProps[i];

      component.bindDynamicProp(i, value.get(), content);

      addDependentComponentAndSubscribeIfNeeded(value, component);

      dynamicValues.add(value);
    }

    mAffectingDynamicValues.put(component, dynamicValues);
    mContents.put(component, content);
  }

  void onUnbindComponent(Component component) {
    mContents.remove(component);

    final Set<DynamicValue<?>> dynamicValues = mAffectingDynamicValues.get(component);
    if (dynamicValues == null) {
      return;
    }

    for (DynamicValue<?> value : dynamicValues) {
      removeDependentComponentAndUnsubscribeIfNeeded(value, component);
    }
  }

  private void addDependentComponentAndSubscribeIfNeeded(
      DynamicValue<?> value, Component component) {
    Set<Component> dependentComponents = mDependentComponents.get(value);

    if (dependentComponents == null) {
      dependentComponents = new HashSet<>();
      mDependentComponents.put(value, dependentComponents);

      value.attachListener(this);
    }

    dependentComponents.add(component);
  }

  private void removeDependentComponentAndUnsubscribeIfNeeded(
      DynamicValue<?> value, Component component) {
    final Set<Component> dependentComponents = mDependentComponents.get(value);
    dependentComponents.remove(component);

    if (dependentComponents.isEmpty()) {
      mDependentComponents.remove(value);

      value.detach(this);
    }
  }

  @Override
  public void onValueChange(DynamicValue value) {
    final Set<Component> dependentComponents = mDependentComponents.get(value);

    for (Component component : dependentComponents) {
      final Object content = mContents.get(component);

      final DynamicValue[] dynamicProps = component.getDynamicProps();
      for (int i = 0; i < dynamicProps.length; i++) {
        if (value == dynamicProps[i]) {
          component.bindDynamicProp(i, value.get(), content);
        }
      }
    }
  }
}
