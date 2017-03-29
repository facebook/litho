/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.view.ViewGroup;

import org.robolectric.annotation.Implements;

/**
 * Robolectric shadow view does not support layout direction so we must implement our custom shadow.
 * We must have ViewGroup and View shadows as Robolectric forces us to have the whole hierarchy.
 */
@Implements(ViewGroup.class)
public class LayoutDirectionViewGroupShadow extends LayoutDirectionViewShadow {

}
