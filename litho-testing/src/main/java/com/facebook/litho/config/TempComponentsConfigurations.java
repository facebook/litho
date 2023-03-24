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

package com.facebook.litho.config;

import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class TempComponentsConfigurations {

  private static final ComponentsConfiguration.Builder original =
      ComponentsConfiguration.getDefaultComponentsConfigurationBuilder();

  private static final boolean shouldCompareCommonPropsInIsEquivalentTo =
      ComponentsConfiguration.shouldCompareCommonPropsInIsEquivalentTo;

  public static void setShouldAddHostViewForRootComponent(boolean value) {
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.create()
            .shouldAddHostViewForRootComponent(value)
            .shouldAddHostViewForRootComponent(value));
  }

  public static void restoreShouldAddHostViewForRootComponent() {
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(original);
  }

  public static void setShouldCompareCommonPropsInIsEquivalentTo(
      boolean shouldCompareCommonPropsInIsEquivalentTo) {
    ComponentsConfiguration.shouldCompareCommonPropsInIsEquivalentTo =
        shouldCompareCommonPropsInIsEquivalentTo;
  }

  public static void restoreShouldCompareCommonPropsInIsEquivalentTo() {
    ComponentsConfiguration.shouldCompareCommonPropsInIsEquivalentTo =
        shouldCompareCommonPropsInIsEquivalentTo;
  }
}
