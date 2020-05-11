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

package com.facebook.litho.testing.espresso;

import static com.facebook.litho.testing.espresso.ComponentHostMatchers.componentHost;
import static com.facebook.litho.testing.espresso.ComponentHostMatchers.componentHostWithText;
import static com.facebook.litho.testing.espresso.ComponentHostMatchers.withContentDescription;
import static com.facebook.litho.testing.espresso.ComponentHostMatchers.withLifecycle;
import static com.facebook.litho.testing.espresso.ComponentHostMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import android.widget.TextView;
import androidx.test.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.runner.AndroidJUnit4;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.widget.Text;
import com.facebook.testing.screenshot.ViewHelpers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests {@link ComponentHostMatchers} */
@RunWith(AndroidJUnit4.class)
public class ComponentHostMatchersTest {

  private LithoView mView;

  @UiThreadTest
  @Before
  public void before() throws Throwable {
    final ComponentContext mComponentContext =
        new ComponentContext(InstrumentationRegistry.getTargetContext());
    final Component mTextComponent =
        MyComponent.create(mComponentContext).text("foobar").customViewTag("zoidberg").build();
    final ComponentTree tree = ComponentTree.create(mComponentContext, mTextComponent).build();
    mView = new LithoView(mComponentContext);
    mView.setComponentTree(tree);
    ViewHelpers.setupView(mView).setExactWidthPx(200).setExactHeightPx(100).layout();
  }

  @Test
  public void testContentDescriptionMatching() throws Throwable {
    assertThat(mView, componentHostWithText("foobar"));
    assertThat(mView, not(componentHostWithText("bar")));
    assertThat(mView, componentHostWithText(containsString("oob")));
  }

  @Test
  public void testIsComponentHost() throws Throwable {
    assertThat(new TextView(InstrumentationRegistry.getTargetContext()), is(not(componentHost())));
    assertThat(mView, is(componentHost()));
  }

  @Test
  public void testIsComponentHostWithMatcher() throws Throwable {
    assertThat(mView, is(componentHost(withText("foobar"))));
    assertThat(mView, is(not(componentHost(withText("blah")))));
  }

  @Test
  public void testContentDescription() throws Throwable {
    assertThat(mView, is(componentHost(withContentDescription("foobar2"))));
  }

  @Test
  public void testMountedComponent() throws Throwable {
    assertThat(mView, is(componentHost(withLifecycle(isA(Text.class)))));
  }
}
