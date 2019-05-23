/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.ComponentContext.NULL_LAYOUT;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.robolectric.RuntimeEnvironment.application;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class WrapperTest {

  @Before
  public void before() {
    ComponentsConfiguration.isEndToEndTestRun = true;
  }

  @After
  public void after() {
    ComponentsConfiguration.isEndToEndTestRun = false;
  }

  @Test
  public void testWrapperWithNullComponentReturnsNullLayout() {
    ComponentContext c = new ComponentContext(application);
    Wrapper wrapper = Wrapper.create(c).delegate(null).build();
    assertThat(NULL_LAYOUT).isEqualTo(wrapper.resolve(c));
  }

  @Test
  public void testWrapperWithALayout() {
    ComponentContext c = new ComponentContext(application);
    Wrapper wrapper =
        Wrapper.create(c)
            .delegate(Row.create(c).child(Text.create(c).text("test")).build())
            .build();

    InternalNode node = (InternalNode) wrapper.resolve(c);
    Component component = node.getRootComponent();
    assertThat(component.getClass()).isEqualTo(Row.class);
  }

  @Test
  public void testWrapper_ShouldBeInInternalNode() {
    ComponentContext c = new ComponentContext(application);
    Component root =
        Column.create(c)
            .child(
                Wrapper.create(c)
                    .delegate(Row.create(c).child(Text.create(c).text("test")).build()))
            .build();

    String key = Wrapper.create(c).delegate(Row.create(c).build()).build().getKey();

    ComponentTree componentTree = measureAndLayoutComponent(c, root);
    LayoutState current = componentTree.getMainThreadLayoutState();
    InternalNode layout = current.getLayoutRoot();

    assertThat(layout).isNotNull();
    assertThat(layout.getChildCount()).isEqualTo(1);
    List<Component> components = layout.getChildAt(0).getComponents();
    assertThat(components.get(1).getClass()).isEqualTo(Wrapper.class);
    assertThat(components.get(1).getGlobalKey()).endsWith(key);
    assertThat(components.get(0).getGlobalKey()).startsWith(components.get(1).getGlobalKey());
  }

  private ComponentTree measureAndLayoutComponent(ComponentContext c, Component root) {
    ComponentTree componentTree = spy(ComponentTree.create(c, root).build());
    LithoView view = new LithoView(c);
    view.setComponentTree(componentTree);
    view.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(view);

    return componentTree;
  }
}
