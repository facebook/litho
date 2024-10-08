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

package com.facebook.samples.litho.java.lithography;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.widget.RenderInfo;

/**
 * This is an interface for a piece of data that defines a component to be rendered in the feed.
 * Typically the datum would hold some intrinsic data (Strings or others) and use them to create the
 * component.
 */
public interface Datum {

  RenderInfo createComponent(ComponentContext c);
}
