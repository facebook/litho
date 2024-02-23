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

package com.facebook.litho;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Pair;
import android.util.SparseArray;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Takes care of dynamic Props
 *
 * <p>Usage from {@link DynamicPropsExtension}
 *
 * <p>Unique per {@link DynamicPropsExtension} instance, which calls {@link
 * #onBindComponentToContent(Component, ComponentContext, SparseArray, Object)} and {#link {@link
 * #onUnbindComponent(Component, SparseArray, Object)}} when it binds and unbinds Components
 * respectively.
 *
 * <p>When Component is being bound, a DynamicPropsManager subscribes to {@link DynamicValue}s that
 * its represent dynamic props, and keeps the mounted content the Components is rendered into in
 * sync with {@link DynamicValue#get()} until the Component gets unbound, at which point, the
 * DynamicPropsManager unsubscribes from the DynamicValues.
 */
public class DynamicPropsManager implements DynamicValue.OnValueChangeListener {
  public static final int KEY_ALPHA = 1;
  public static final int KEY_TRANSLATION_X = 2;
  public static final int KEY_TRANSLATION_Y = 3;
  public static final int KEY_SCALE_X = 4;
  public static final int KEY_SCALE_Y = 5;
  public static final int KEY_ELEVATION = 6;
  public static final int KEY_BACKGROUND_COLOR = 7;
  public static final int KEY_ROTATION = 8;
  public static final int KEY_BACKGROUND_DRAWABLE = 9;
  public static final int KEY_FOREGROUND_COLOR = 10;

  private static final DynamicValue<?>[] sEmptyArray = new DynamicValue[0];

  private final Map<DynamicValue<?>, Set<Pair<Component, SparseArray<? extends DynamicValue<?>>>>>
      mDependentComponents = new HashMap<>();
  private final Map<Component, Set<DynamicValue<?>>> mAffectingDynamicValues = new HashMap<>();
  private final Map<Component, Object> mContents = new HashMap<>();

  void onBindComponentToContent(
      final Component component,
      final @Nullable ComponentContext scopedContext,
      final @Nullable SparseArray<? extends DynamicValue<?>> commonDynamicProps,
      final Object content) {
    final boolean hasCommonDynamicPropsToBind =
        hasCommonDynamicPropsToBind(commonDynamicProps, content);

    if (!hasCommonDynamicPropsToBind && !hasCustomDynamicProps(component)) {
      return;
    }

    final Set<DynamicValue<?>> dynamicValues = new HashSet<>();

    if (commonDynamicProps != null && hasCommonDynamicPropsToBind) {
      // Go through all common dynamic props
      for (int i = 0; i < commonDynamicProps.size(); i++) {
        final int key = commonDynamicProps.keyAt(i);
        final DynamicValue<?> value = commonDynamicProps.valueAt(i);

        bindCommonDynamicProp(key, value, (View) content);

        addDependentComponentAndSubscribeIfNeeded(value, new Pair<>(component, commonDynamicProps));
        dynamicValues.add(value);
      }
    }

    final DynamicValue<?>[] customDynamicProps = getCustomDynamicProps(component);
    // Go through all the custom dynamic props
    for (int i = 0; i < customDynamicProps.length; i++) {
      final @Nullable DynamicValue<?> value = customDynamicProps[i];

      try {
        ((SpecGeneratedComponent) component)
            .bindDynamicProp(i, value != null ? value.get() : null, content);

        addDependentComponentAndSubscribeIfNeeded(value, new Pair<>(component, commonDynamicProps));
        dynamicValues.add(value);
      } catch (Exception e) {
        if (scopedContext != null) {
          ComponentUtils.handle(scopedContext, e);
        } else {
          ComponentUtils.rethrow(e);
        }
      }
    }

    mAffectingDynamicValues.put(component, dynamicValues);
    mContents.put(component, content);
  }

  void onUnbindComponent(
      Component component,
      final @Nullable SparseArray<? extends DynamicValue<?>> commonDynamicProps,
      Object content) {
    if (!hasCommonDynamicPropsToBind(commonDynamicProps, content)
        && !hasCustomDynamicProps(component)) {
      return;
    }

    mContents.remove(component);

    final Set<DynamicValue<?>> dynamicValues = mAffectingDynamicValues.get(component);
    if (dynamicValues == null) {
      return;
    }

    for (DynamicValue<?> value : dynamicValues) {
      removeDependentComponentAndUnsubscribeIfNeeded(
          value, new Pair<>(component, commonDynamicProps));
    }

    // Go through all common dynamic props to reset them if they were set
    if (commonDynamicProps != null) {
      for (int i = 0; i < commonDynamicProps.size(); i++) {
        final int key = commonDynamicProps.keyAt(i);
        resetDynamicValues(key, content);
      }
    }

    mAffectingDynamicValues.remove(component);
  }

  private static void resetDynamicValues(int key, Object content) {
    if (!(content instanceof View)) {
      return;
    }

    final View target = (View) content;
    switch (key) {
      case KEY_ALPHA:
        if (target.getAlpha() != 1) {
          target.setAlpha(1);
        }
        break;

      case KEY_TRANSLATION_X:
        if (target.getTranslationX() != 0) {
          target.setTranslationX(0);
        }
        break;

      case KEY_TRANSLATION_Y:
        if (target.getTranslationY() != 0) {
          target.setTranslationY(0);
        }
        break;

      case KEY_SCALE_X:
        if (target.getScaleX() != 1) {
          target.setScaleX(1);
        }
        break;

      case KEY_SCALE_Y:
        if (target.getScaleY() != 1) {
          target.setScaleY(1);
        }
        break;

      case KEY_ELEVATION:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && target.getElevation() != 0) {
          target.setElevation(0);
        }
        break;

      case KEY_ROTATION:
        if (target.getRotation() != 0) {
          target.setRotation(0);
        }
        break;

      case KEY_BACKGROUND_COLOR:
      case KEY_BACKGROUND_DRAWABLE:
        if (target.getBackground() != null) {
          target.setBackground(null);
        }
        break;

      case KEY_FOREGROUND_COLOR:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && target.getForeground() != null) {
          ViewUtils.setViewForeground(target, null);
        }
    }
  }

  private void addDependentComponentAndSubscribeIfNeeded(
      @Nullable DynamicValue<?> value,
      Pair<Component, SparseArray<? extends DynamicValue<?>>> componentWithProps) {
    if (value == null) {
      return;
    }
    Set<Pair<Component, SparseArray<? extends DynamicValue<?>>>> dependentComponents =
        mDependentComponents.get(value);

    if (dependentComponents == null) {
      dependentComponents = new HashSet<>();
      mDependentComponents.put(value, dependentComponents);

      value.attachListener(this);
    }

    dependentComponents.add(componentWithProps);
  }

  private void removeDependentComponentAndUnsubscribeIfNeeded(
      @Nullable DynamicValue<?> value,
      Pair<Component, SparseArray<? extends DynamicValue<?>>> componentWithProps) {
    if (value == null) {
      return;
    }
    final Set<Pair<Component, SparseArray<? extends DynamicValue<?>>>> dependentComponents =
        mDependentComponents.get(value);
    if (dependentComponents == null) {
      return;
    }

    dependentComponents.remove(componentWithProps);

    if (dependentComponents.isEmpty()) {
      mDependentComponents.remove(value);
      value.detach(this);
    }
  }

  private static void bindCommonDynamicProp(int key, @Nullable DynamicValue<?> value, View target) {
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

      case KEY_ROTATION:
        target.setRotation(DynamicPropsManager.<Float>resolve(value));
        break;

      case KEY_BACKGROUND_DRAWABLE:
        target.setBackground(DynamicPropsManager.<Drawable>resolve(value));
        break;

      case KEY_FOREGROUND_COLOR:
        ViewUtils.setViewForeground(target, DynamicPropsManager.<Integer>resolve(value));
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T resolve(DynamicValue<?> dynamicValue) {
    return (T) dynamicValue.get();
  }

  @Override
  public void onValueChange(DynamicValue value) {
    final Set<Pair<Component, SparseArray<? extends DynamicValue<?>>>> dependentComponents =
        mDependentComponents.get(value);
    if (dependentComponents == null) {
      return;
    }

    // It's possible that applying a dynamic prop could bind or unbind a component - snapshot the
    // components here to prevent a ConcurrentModificationException during iteration
    Pair<Component, SparseArray<DynamicValue<?>>>[] dependentComponentSnapshot =
        dependentComponents.toArray(new Pair[dependentComponents.size()]);
    for (Pair<Component, SparseArray<DynamicValue<?>>> componentWithProps :
        dependentComponentSnapshot) {
      final Component component = componentWithProps.first;
      final SparseArray<DynamicValue<?>> commonDynamicProps = componentWithProps.second;
      final Object content = mContents.get(component);
      if (content == null) {
        continue;
      }

      if (hasCommonDynamicPropsToBind(commonDynamicProps, content)) {
        for (int i = 0; i < commonDynamicProps.size(); i++) {
          if (commonDynamicProps.valueAt(i) == value) {
            bindCommonDynamicProp(commonDynamicProps.keyAt(i), value, (View) content);
          }
        }
      }

      final DynamicValue<?>[] dynamicProps = getCustomDynamicProps(component);
      for (int i = 0; i < dynamicProps.length; i++) {
        if (value == dynamicProps[i]) {
          ((SpecGeneratedComponent) component).bindDynamicProp(i, value.get(), content);
        }
      }
    }
  }

  /**
   * Common dynamic props could only be bound to Views. To make it work for the LayoutSpec and
   * MountDrawableSpec components we create a wrapping HostComponent and copy the dynamic props
   * there. Thus DynamicPropsManager should ignore non-MountViewSpecs
   *
   * @param commonDynamicProps to consider
   * @return true if Component has common dynamic props, that DynamicPropsManager should take an
   *     action on
   */
  private static boolean hasCommonDynamicPropsToBind(
      final @Nullable SparseArray<? extends DynamicValue<?>> commonDynamicProps, Object content) {
    return CollectionsUtils.isNotNullOrEmpty(commonDynamicProps) && content instanceof View;
  }

  private static boolean hasCustomDynamicProps(Component component) {
    return component instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) component).getDynamicProps().length > 0;
  }

  private static DynamicValue<?>[] getCustomDynamicProps(Component component) {
    return component instanceof SpecGeneratedComponent
        ? ((SpecGeneratedComponent) component).getDynamicProps()
        : sEmptyArray;
  }

  @VisibleForTesting
  boolean hasCachedContent(Component component) {
    return mContents.containsKey(component);
  }
}
