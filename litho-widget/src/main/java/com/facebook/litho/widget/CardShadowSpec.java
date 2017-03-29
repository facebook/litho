/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.Context;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.ResType;

/**
 * A component that is able to render the card's shadow. Used in the
 * implementation of {@link CardSpec}.
 */
@MountSpec(isPublic = false, isPureRender = true)
class CardShadowSpec {

  @OnCreateMountContent
  static CardShadowDrawable onCreateMountContent(ComponentContext c) {
    return new CardShadowDrawable();
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      CardShadowDrawable cardShadowDrawable,
