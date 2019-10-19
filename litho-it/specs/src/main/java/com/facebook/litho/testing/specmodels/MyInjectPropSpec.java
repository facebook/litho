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

package com.facebook.litho.testing.specmodels;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.InjectProp;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;

@LayoutSpec
public class MyInjectPropSpec {
  @OnCreateLayout
  public static Component onCreateLayout(
      ComponentContext c,
      @Prop String normalString,
      @InjectProp String injectedString,
      @InjectProp Kettle injectedKettle,
      @InjectProp Text injectedComponent) {
    return Column.create(c).child(injectedComponent).build();
  }

  public static class Kettle {
    public float temperatureCelsius;

    public Kettle(float v) {
      temperatureCelsius = v;
    }
  }
}
