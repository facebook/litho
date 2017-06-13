/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.view.View;

import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.litho.widget.CardClip;
import com.facebook.litho.widget.Text;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentGlobalKeyTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testComponentKey() {
    Component component = TestDrawableComponent
        .create(mContext)
        .build();
    Assert.assertEquals(component.getKey(), component.getLifecycle().getId() + "");
    Assert.assertNull(component.getGlobalKey());
  }

  @Test
  public void testComponentManualKey() {
    Component component = TestDrawableComponent
        .create(mContext)
        .key("someKey")
        .build();
    Assert.assertEquals(component.getKey(), "someKey");
    Assert.assertNull(component.getGlobalKey());
  }

  @Test
  public void testComponentGlobalKey() {
    Component component = TestDrawableComponent
        .create(mContext)
        .build();
    System.out.println(component.getLifecycle().getId());
    ComponentTree componentTree = ComponentTree.create(mContext, component)
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();
    LithoView lithoView = getLithoView(componentTree);

    Assert.assertEquals(
        lithoView.getMountItemAt(0).getComponent().getGlobalKey(),
        component.getKey());
  }

  @Test
  public void testComponentGlobalKeyManualKey() {
    Component component = TestDrawableComponent
        .create(mContext)
        .key("someKey")
        .build();
    System.out.println(component.getLifecycle().getId());
    ComponentTree componentTree = ComponentTree.create(mContext, component)
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();
    LithoView lithoView = getLithoView(componentTree);

    Assert.assertEquals(
        lithoView.getMountItemAt(0).getComponent().getGlobalKey(),
        "someKey");
  }

  @Test
  public void testMultipleChildrenComponentKey() {
    Component component = getMultipleChildrenComponent();

    int layoutSpecId = component.getLifecycle().getId();
    int nestedLayoutSpecId = layoutSpecId - 1;

    ComponentTree componentTree = ComponentTree.create(mContext, component)
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();
    LithoView lithoView = getLithoView(componentTree);

    // Text
    Assert.assertEquals(layoutSpecId + "[Text2]", getComponentAt(lithoView, 0).getGlobalKey());
    // TestViewComponent in child layout
    Assert.assertEquals(layoutSpecId + "" + nestedLayoutSpecId + "[TestViewComponent1]", getComponentAt(lithoView, 1).getGlobalKey());
    //background in child
    Assert.assertNull(getComponentAt(lithoView, 2).getGlobalKey());
    // CardClip in child
    Assert.assertEquals(layoutSpecId + "" + nestedLayoutSpecId + "[CardClip1]", getComponentAt(lithoView, 3).getGlobalKey());
    // Text in child
    Assert.assertEquals(layoutSpecId + "" + nestedLayoutSpecId + "[Text1]", getComponentAt(lithoView, 4).getGlobalKey());
    // background
    Assert.assertNull(getComponentAt(lithoView, 5).getGlobalKey());
    // CardClip
    Assert.assertEquals(layoutSpecId + "[CardClip2]", getComponentAt(lithoView, 6).getGlobalKey());
    // TestViewComponent
    Assert.assertEquals(layoutSpecId + "[TestViewComponent2]", getComponentAt(lithoView, 7).getGlobalKey());
  }

  private static Component getComponentAt(LithoView lithoView, int index) {
    return lithoView.getMountItemAt(index).getComponent();
  }

  private LithoView getLithoView(ComponentTree componentTree) {
    LithoView lithoView = new LithoView(mContext);
    lithoView.setComponentTree(componentTree);
    lithoView.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    lithoView.layout(
        0,
        0,
        lithoView.getMeasuredWidth(),
        lithoView.getMeasuredHeight());
    return lithoView;
  }

  private static Component getMultipleChildrenComponent() {
    final int color = 0xFFFF0000;
    final Component testGlobalKeyChildComponent = new InlineLayoutSpec() {

      @Override
      @OnCreateLayout
      protected ComponentLayout onCreateLayout(
          ComponentContext c) {

        return Column.create(c)
            .child(TestViewComponent.create(c).key("[TestViewComponent1]"))
            .child(
                Column.create(c)
                    .backgroundColor(color)
                    .child(CardClip.create(c).key("[CardClip1]")))
            .child(Text.create(c).text("Test").key("[Text1]"))
            .build();
      }
    };

    final Component testGlobalKeyChild = new InlineLayoutSpec() {

      @Override
      @OnCreateLayout
      protected ComponentLayout onCreateLayout(
          ComponentContext c) {

        return Column.create(c)
            .child(Text.create(c).text("test").key("[Text2]"))
            .child(testGlobalKeyChildComponent)
            .child(
                Column.create(c)
                    .backgroundColor(color)
                    .child(CardClip.create(c).key("[CardClip2]")))
            .child(TestViewComponent.create(c).key("[TestViewComponent2]"))
            .build();
      }
    };

    return testGlobalKeyChild;
  }
}
