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

package com.facebook.litho.testing.shadows;

import android.view.ViewGroup;
import org.robolectric.annotation.Implements;

/**
 * Robolectric shadow view does not support layout direction so we must implement our custom shadow.
 * We must have ViewGroup and View shadows as Robolectric forces us to have the whole hierarchy.
 */
@Implements(ViewGroup.class)
public class LayoutDirectionViewGroupShadow extends LayoutDirectionViewShadow {}
