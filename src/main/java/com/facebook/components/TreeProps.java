/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.support.v4.util.SimpleArrayMap;

import com.facebook.litho.annotations.TreeProp;
import com.facebook.infer.annotation.ThreadConfined;

/**
 * A data structure to store tree props.
 * @see {@link TreeProp}.
 */
@ThreadConfined(ThreadConfined.ANY)
public class TreeProps {

