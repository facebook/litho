// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.view.View;

import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.widget.CardClip;
import com.facebook.components.widget.Text;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestViewComponent;

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
        .build();
    ComponentView componentView = getComponentView(componentTree);

    Assert.assertEquals(
        componentView.getMountItemAt(0).getComponent().getGlobalKey(),
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
        .build();
    ComponentView componentView = getComponentView(componentTree);

    Assert.assertEquals(
        componentView.getMountItemAt(0).getComponent().getGlobalKey(),
        "someKey");
  }

  @Test
  public void testMultipleChildrenComponentKey() {
    Component component = getMultipleChildrenComponent();

    int layoutSpecId = component.getLifecycle().getId();
    int nestedLayoutSpecId = layoutSpecId - 1;

    ComponentTree componentTree = ComponentTree.create(mContext, component)
        .incrementalMount(false)
        .build();
    ComponentView componentView = getComponentView(componentTree);

    // Text
    Assert.assertEquals(layoutSpecId + "[Text2]", getComponentAt(componentView, 0).getGlobalKey());
    // TestViewComponent in child layout
    Assert.assertEquals(layoutSpecId + "" + nestedLayoutSpecId + "[TestViewComponent1]", getComponentAt(componentView, 1).getGlobalKey());
    //background in child
    Assert.assertNull(getComponentAt(componentView, 2).getGlobalKey());
    // CardClip in child
    Assert.assertEquals(layoutSpecId + "" + nestedLayoutSpecId + "[CardClip1]", getComponentAt(componentView, 3).getGlobalKey());
    // Text in child
    Assert.assertEquals(layoutSpecId + "" + nestedLayoutSpecId + "[Text1]", getComponentAt(componentView, 4).getGlobalKey());
    // background
    Assert.assertNull(getComponentAt(componentView, 5).getGlobalKey());
    // CardClip
    Assert.assertEquals(layoutSpecId + "[CardClip2]", getComponentAt(componentView, 6).getGlobalKey());
    // TestViewComponent
    Assert.assertEquals(layoutSpecId + "[TestViewComponent2]", getComponentAt(componentView, 7).getGlobalKey());
  }

  private static Component getComponentAt(ComponentView componentView, int index) {
    return componentView.getMountItemAt(index).getComponent();
  }

  private ComponentView getComponentView(ComponentTree componentTree) {
    ComponentView componentView = new ComponentView(mContext);
    componentView.setComponent(componentTree);
    componentView.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    componentView.layout(
        0,
        0,
        componentView.getMeasuredWidth(),
        componentView.getMeasuredHeight());
    return componentView;
  }

  private static Component getMultipleChildrenComponent() {
    final int color = 0xFFFF0000;
    final Component testGlobalKeyChildComponent = new InlineLayoutSpec() {

      @Override
      @OnCreateLayout
      protected ComponentLayout onCreateLayout(
          ComponentContext c) {

        return Container.create(c)
            .child(TestViewComponent.create(c).key("[TestViewComponent1]"))
            .child(
                Container.create(c)
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

        return Container.create(c)
            .child(Text.create(c).text("test").key("[Text2]"))
            .child(testGlobalKeyChildComponent)
            .child(
                Container.create(c)
                    .backgroundColor(color)
                    .child(CardClip.create(c).key("[CardClip2]")))
            .child(TestViewComponent.create(c).key("[TestViewComponent2]"))
            .build();
      }
    };

    return testGlobalKeyChild;
  }
}
