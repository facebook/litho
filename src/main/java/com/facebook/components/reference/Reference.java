/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.ResourceResolver;

/**
 * Represents a unique instance of a reference that is driven by its matching
 * {@link ReferenceLifecycle} subclass. Use {@link Reference#acquire(ComponentContext, Reference)}
 * to acquire the underlying resource and
 * {@link Reference#release(ComponentContext, Object, Reference)} to release it when
 * it's not needed anymore.
 */
public abstract class Reference<L> {

