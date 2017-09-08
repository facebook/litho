/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.annotations;

/**
 * An interface that is annotated with this annotation will be used to generate a matcher based on
 * the underlying spec. This matcher can in turn be used in tests to verify that mounted components
 * contain certain properties.
 *
 * <p>For example:
 *
 * <pre>
 * <code>{@literal @}TestSpec(MyLayoutSpec.class)
 *  public interface TestMyLayoutSpec {}
 * </code>
 * </pre>
 *
 * This will generate a corresponding <code>TestMyLayout</code> class which implements the provided
 * interface. It contains a Matcher for the properties of the specified <code>MyLayoutSpec</code>
 * component spec. This can be used in tests as follows:
 *
 * <pre>
 * <code>{@literal @}Test
 *  public void testMyLayoutSpec() {
 *    final ComponentContext c = mComponentsRule.getContext();
 *    assertThat(c, mComponent)
 *        .has(subComponentWith(c,
 *            TestMyLayoutSpec.matcher(c)
 *                .title(containsString("My Partial Titl"))
 *                .someOtherProperty(14)
 *                .build()));
 *  }
 * </code>
 * </pre>
 */
public @interface TestSpec {
  /**
   * A Component Spec class which is annotated with either {@link LayoutSpec} or {@link MountSpec}.
   */
  Class<?> value();
}
