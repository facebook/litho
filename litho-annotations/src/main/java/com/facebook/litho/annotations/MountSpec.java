/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class that is annotated with this annotation will be used to create a component that renders
 * something, in the form of either a Drawable or a View.
 * <p>A class that is annotated with {@link MountSpec} must implement a method with the
 * {@link OnMount} annotation. It may also implement methods with the following annotations:
 * - {@link OnLoadStyle}
 * - {@link OnEvent}
 * - {@link OnPrepare}
 * - {@link OnMeasure}
 * - {@link OnBoundsDefined}
 * - {@link OnMount}
