/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.squareup.javapoet.ClassName;

/**
 * Constants used in {@link SpecModel}s.
 */
public interface ClassNames {
  ClassName OBJECT = ClassName.bestGuess("java.lang.Object");
  ClassName STRING = ClassName.bestGuess("java.lang.String");

  ClassName VIEW = ClassName.bestGuess("android.view.View");
  ClassName DRAWABLE =
      ClassName.bestGuess("android.graphics.drawable.Drawable");

  ClassName ACCESSIBILITY_NODE =
      ClassName.bestGuess("android.support.v4.view.accessibility.AccessibilityNodeInfoCompat");

  ClassName STRING_RES = ClassName.bestGuess("android.support.annotation.StringRes");
  ClassName INT_RES = ClassName.bestGuess("android.support.annotation.IntegerRes");
  ClassName BOOL_RES = ClassName.bestGuess("android.support.annotation.BoolRes");
  ClassName COLOR_RES = ClassName.bestGuess("android.support.annotation.ColorRes");
  ClassName COLOR_INT = ClassName.bestGuess("android.support.annotation.ColorInt");
  ClassName DIMEN_RES = ClassName.bestGuess("android.support.annotation.DimenRes");
  ClassName ATTR_RES = ClassName.bestGuess("android.support.annotation.AttrRes");
  ClassName DRAWABLE_RES = ClassName.bestGuess("android.support.annotation.DrawableRes");
  ClassName ARRAY_RES = ClassName.bestGuess("android.support.annotation.ArrayRes");
  ClassName DIMENSION = ClassName.bestGuess("android.support.annotation.Dimension");
  ClassName PX = ClassName.bestGuess("android.support.annotation.Px");

  ClassName SYNCHRONIZED_POOL =
      ClassName.bestGuess("android.support.v4.util.Pools.SynchronizedPool");

  ClassName LAYOUT_SPEC = ClassName.bestGuess("com.facebook.litho.annotations.LayoutSpec");
  ClassName MOUNT_SPEC = ClassName.bestGuess("com.facebook.litho.annotations.MountSpec");

  ClassName OUTPUT = ClassName.bestGuess("com.facebook.litho.Output");
  ClassName DIFF = ClassName.bestGuess("com.facebook.litho.Diff");
  ClassName SIZE = ClassName.bestGuess("com.facebook.litho.Size");

  ClassName ANIMATION = ClassName.bestGuess("com.facebook.litho.Transition");

  ClassName COMPONENTS_CONFIGURATION =
      ClassName.bestGuess("com.facebook.litho.config.ComponentsConfiguration");

  ClassName COMPONENT_CONTEXT = ClassName.bestGuess("com.facebook.litho.ComponentContext");
  ClassName COMPONENT_LAYOUT = ClassName.bestGuess("com.facebook.litho.ComponentLayout");
  ClassName COMPONENT_LAYOUT_BUILDER =
      ClassName.bestGuess("com.facebook.litho.ComponentLayout.Builder");
  ClassName COMPONENT_LAYOUT_CONTAINER_BUILDER =
      ClassName.bestGuess("com.facebook.litho.ComponentLayout.ContainerBuilder");

  ClassName COMPONENT = ClassName.bestGuess("com.facebook.litho.Component");
  ClassName COMPONENT_BUILDER = ClassName.bestGuess("com.facebook.litho.Component.Builder");
  ClassName COMPONENT_BUILDER_WITH_LAYOUT =
      ClassName.bestGuess("com.facebook.litho.Component.BuilderWithLayout");
  ClassName COMPONENT_LIFECYCLE = ClassName.bestGuess("com.facebook.litho.ComponentLifecycle");
  ClassName COMPONENT_LIFECYCLE_MOUNT_TYPE =
      ClassName.bestGuess("com.facebook.litho.ComponentLifecycle.MountType");

  ClassName REFERENCE = ClassName.bestGuess("com.facebook.litho.reference.Reference");
  ClassName REFERENCE_BUILDER =
      ClassName.bestGuess("com.facebook.litho.reference.Reference.Builder");
  ClassName REFERENCE_LIFECYCLE =
      ClassName.bestGuess("com.facebook.litho.reference.ReferenceLifecycle");

  ClassName TREE_PROPS = ClassName.bestGuess("com.facebook.litho.TreeProps");

  ClassName STATE_VALUE = ClassName.bestGuess("com.facebook.litho.StateValue");
  ClassName COMPONENT_STATE_UPDATE =
      ClassName.bestGuess("com.facebook.litho.ComponentLifecycle.StateUpdate");
  ClassName STATE_CONTAINER_COMPONENT =
      ClassName.bestGuess("com.facebook.litho.ComponentLifecycle.StateContainer");

  ClassName EVENT_DISPATCHER =
      ClassName.bestGuess("com.facebook.litho.EventDispatcher");
  ClassName HAS_EVENT_DISPATCHER_CLASSNAME =
      ClassName.bestGuess("com.facebook.litho.HasEventDispatcher");
  ClassName EVENT_HANDLER = ClassName.bestGuess("com.facebook.litho.EventHandler");
