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

package com.facebook.litho.testing.treeprop;

import android.content.Context;
import android.graphics.drawable.Drawable;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.TreeProp;

/** Used in TreePropTest. */
@MountSpec
public class TreePropTestMountSpec {

  @OnPrepare
  static void onPrepare(
      ComponentContext c,
      @TreeProp TreePropNumberType propA,
      @Prop TreePropTestResult resultPropA) {
    resultPropA.mProp = propA;
  }

  @OnCreateMountContent
  static Drawable onCreateMountContent(Context c) {
    return null;
  }
}
