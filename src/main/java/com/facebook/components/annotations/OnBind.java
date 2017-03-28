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
 * A method annotation used in classes that are annotated with {@link MountSpec}.
 * <p>Methods annotated with {@link OnBind} take an Android Context as the first parameter,
 * the Object that the MountSpec mounts as the second parameter, followed by any number of
 * {@link Prop}s.
 * <p>The method should return void.
 * This callback will be invoked every time the mounted object is about to become active after being
 * mounted. This method can be called multiple times after onMount with the same mounted content.
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnBind {

}
