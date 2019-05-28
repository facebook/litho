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

import android.os.Build;
import android.util.SparseArray;
import android.view.View;
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
  static final int KEY_ALPHA = 1;
  static final int KEY_TRANSLATION_X = 2;
  static final int KEY_TRANSLATION_Y = 3;
  static final int KEY_SCALE_X = 4;
  static final int KEY_SCALE_Y = 5;
  static final int KEY_ELEVATION = 6;
  static final int KEY_BACKGROUND_COLOR = 7;

  private final Map<DynamicValue<?>, Set<Component>> mDependentComponents = new HashMap<>();
  private final Map<Component, Set<DynamicValue<?>>> mAffectingDynamicValues = new HashMap<>();
  private final Map<Component, Object> mContents = new HashMap<>();

  void onBindComponentToContent(Component component, Object content) {
    final boolean hasCommonDynamicPropsToBind = hasCommonDynamicPropsToBind(component);
    final boolean hasCustomDynamicProps = component.getDynamicProps().length > 0;

    if (!hasCommonDynamicPropsToBind && !hasCustomDynamicProps) {
      return;
    }

    final Set<DynamicValue<?>> dynamicValues = new HashSet<>();

    if (hasCommonDynamicPropsToBind) {
      final SparseArray<DynamicValue<?>> commonDynamicProps = component.getCommonDynamicProps();

      // Go through all common dynamic props
      for (int i = 0; i < commonDynamicProps.size(); i++) {
        final int key = commonDynamicProps.keyAt(i);
        final DynamicValue<?> value = commonDynamicProps.valueAt(i);

        bindCommonDynamicProp(key, value, (View) content);

        addDependentComponentAndSubscribeIfNeeded(value, component);
        dynamicValues.add(value);
      }
    }

    final DynamicValue[] dynamicProps = component.getDynamicProps();
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

  private void bindCommonDynamicProp(int key, DynamicValue<?> value, View target) {
    switch (key) {
      case KEY_ALPHA:
        target.setAlpha(DynamicPropsManager.<Float>resolve(value));
        break;

      case KEY_TRANSLATION_X:
        target.setTranslationX(DynamicPropsManager.<Float>resolve(value));
        break;

      case KEY_TRANSLATION_Y:
        target.setTranslationY(DynamicPropsManager.<Float>resolve(value));
        break;

      case KEY_SCALE_X:
        target.setScaleX(DynamicPropsManager.<Float>resolve(value));
        break;

      case KEY_SCALE_Y:
        target.setScaleY(DynamicPropsManager.<Float>resolve(value));
        break;

      case KEY_ELEVATION:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          target.setElevation(DynamicPropsManager.<Float>resolve(value));
        }
        break;

      case KEY_BACKGROUND_COLOR:
        target.setBackgroundColor(DynamicPropsManager.<Integer>resolve(value));
        break;
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T resolve(DynamicValue<?> dynamicValue) {
    return (T) dynamicValue.get();
  }

  @Override
  public void onValueChange(DynamicValue value) {
    final Set<Component> dependentComponents = mDependentComponents.get(value);

    for (Component component : dependentComponents) {
      final Object content = mContents.get(component);

      if (hasCommonDynamicPropsToBind(component)) {
        final SparseArray<DynamicValue<?>> commonDynamicProps = component.getCommonDynamicProps();

        for (int i = 0; i < commonDynamicProps.size(); i++) {
          if (commonDynamicProps.valueAt(i) == value) {
            bindCommonDynamicProp(commonDynamicProps.keyAt(i), value, (View) content);
          }
        }
      }

      final DynamicValue[] dynamicProps = component.getDynamicProps();
      for (int i = 0; i < dynamicProps.length; i++) {
        if (value == dynamicProps[i]) {
          component.bindDynamicProp(i, value.get(), content);
        }
      }
    }
  }

  /**
   * Common dynamic props could only be bound to Views. To make it work for the LayoutSpec and
   * MountDrawableSpec components we create a wrapping HostComponent and copy the dynamic props
   * there. Thus DynamicPropsManager should ignore non-MountViewSpecs
   *
   * @param component Component to consider
   * @return true if Component has common dynamic props, that DynamicPropsManager should take an
   *     action on
   */
  private static boolean hasCommonDynamicPropsToBind(Component component) {
    return component.hasCommonDynamicProps() && Component.isMountViewSpec(component);
  }
}
