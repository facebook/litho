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
 * A class that is annotated with this annotation will be used to create a component lifecycle that
 * is made up of other components.
 * <p>A class that is annotated with {@link LayoutSpec} must implement a method with the
 * {@link OnCreateLayout} annotation. It may also implement methods with the following annotations:
 * - {@link OnLoadStyle}
 * - {@link OnEvent}
 * <p>If you wish to create a component that mounts its own content, then use {@link MountSpec}
 * instead.
 * <p>For example:
 * <pre>
 * {@code
 *
 * @LayoutSpec
 * public class MyComponentSpec {
 *
 *   @OnCreateLayout
 *   protected ComponentLayout onCreateLayout(LayoutContext c, @Prop MyProp prop) {
 *       return Container.create(c)
 *           .flexDirection(ROW)
 *           .alignItems(FLEX_START)
 *           .child(someChild1)
 *           .child(someChild2)
 *           .build();
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface LayoutSpec {
