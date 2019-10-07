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
