/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.res.Resources;
import android.graphics.Color;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;

import static com.facebook.yoga.YogaPositionType.ABSOLUTE;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.litho.widget.CardShadowDrawable.getShadowBottom;
import static com.facebook.litho.widget.CardShadowDrawable.getShadowHorizontal;
import static com.facebook.litho.widget.CardShadowDrawable.getShadowTop;

/**
 * A component that renders a given component into a card border with shadow.
 */
@LayoutSpec (isPureRender = true)
class CardSpec {
