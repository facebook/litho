/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.litho.widget.Image;
import com.facebook.litho.widget.Text;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class ComponentSelectorTest {

  private final InlineLayoutSpec mNullSpec =
      new InlineLayoutSpec() {

        @Override
        protected ComponentLayout onCreateLayout(ComponentContext c) {
          return null;
        }
      };

  @Test
  public void testComponentSelectorSelectsNonNullLayout() throws Exception {
    ComponentContext c = new ComponentContext(application);

    ComponentLayout nullLayout = Wrapper.create(c).delegate(mNullSpec).build();
    ComponentLayout textLayout = Text.create(c).text("Hello World").buildWithLayout();

    ComponentLayout actual =
        ComponentSelector.create(c).tryToRender(nullLayout).tryToRender(textLayout).build();

    assertThat(actual).isEqualTo(textLayout);
  }

  @Test
  public void testComponentSelectorSelectsFirstNonNullLayout() throws Exception {
    ComponentContext c = new ComponentContext(application);

    ComponentLayout nullLayout = Wrapper.create(c).delegate(mNullSpec).build();
    ComponentLayout imageLayout = Image.create(c).drawable(null).buildWithLayout();
    ComponentLayout textLayout = Text.create(c).text("Hello World").buildWithLayout();

    ComponentLayout actual =
        ComponentSelector.create(c)
            .tryToRender(nullLayout)
            .tryToRender(imageLayout)
            .tryToRender(textLayout)
            .build();

    assertThat(actual).isEqualTo(imageLayout);
  }

  @Test
  public void testComponentSelectorSelectsCorrectLayoutWithNullArguments() throws Exception {
    ComponentContext c = new ComponentContext(application);

    ComponentLayout nullLayout = Wrapper.create(c).delegate(mNullSpec).build();
    ComponentLayout imageLayout = Image.create(c).drawable(null).buildWithLayout();
    ComponentLayout textLayout = Text.create(c).text("Hello World").buildWithLayout();

    ComponentLayout actual =
        ComponentSelector.create(c)
            .tryToRender(getNullLayout())
            .tryToRender(getNullLayoutBuilder())
            .tryToRender(getNullComponent())
            .tryToRender(getNullComponentBuilder())
            .tryToRender(nullLayout)
            .tryToRender(imageLayout)
            .tryToRender(textLayout)
            .build();

    assertThat(actual).isEqualTo(imageLayout);
  }

  @Test
  public void testComponentSelectorSelectsLastLayoutWhenNoneRender() throws Exception {
    ComponentContext c = new ComponentContext(application);

    ComponentLayout nullLayout = Wrapper.create(c).delegate(mNullSpec).build();
    ComponentLayout nullLayout2 = Wrapper.create(c).delegate(mNullSpec).build();
    ComponentLayout nullLayout3 = Wrapper.create(c).delegate(mNullSpec).build();

    ComponentLayout actual =
        ComponentSelector.create(c)
            .tryToRender(nullLayout)
            .tryToRender(nullLayout2)
            .tryToRender(nullLayout3)
            .build();

    // Currently, all 3 layouts point to the same object and the behavior would be identical
    // regardless of which layout is selected. So this test is moot. However, this implementation
    // detail could change in the future.
    assertThat(actual).isEqualTo(nullLayout3);
  }

  @Test
  public void testComponentSelectorWithNoArgumentsReturnsNull() throws Exception {
    ComponentContext c = new ComponentContext(application);

    ComponentLayout actual = ComponentSelector.create(c).build();

    assertThat(actual).isNull();
  }

  private static ComponentLayout getNullLayout() {
    return null;
  }

  private static ComponentLayout.Builder getNullLayoutBuilder() {
    return null;
  }

  private static Component getNullComponent() {
    return null;
  }

  private static Component.Builder getNullComponentBuilder() {
    return null;
  }
}
