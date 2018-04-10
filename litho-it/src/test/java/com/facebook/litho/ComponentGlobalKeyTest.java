/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.FrameworkLogEvents.EVENT_WARNING;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MESSAGE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  private static final String mLogTag = "logTag";

  private ComponentContext mContext;
  private ComponentsLogger mComponentsLogger;

  @Before
  public void setup() {
    mComponentsLogger = mock(BaseComponentsLogger.class);
    when(mComponentsLogger.newEvent(any(int.class))).thenCallRealMethod();
    when(mComponentsLogger.newPerformanceEvent(any(int.class))).thenCallRealMethod();
    when(mComponentsLogger.getKeyCollisionStackTraceBlacklist()).thenCallRealMethod();
    when(mComponentsLogger.getKeyCollisionStackTraceKeywords()).thenCallRealMethod();
    mContext = new ComponentContext(RuntimeEnvironment.application, mLogTag, mComponentsLogger);
  }

  @Test
  public void testComponentKey() {
    Component component = TestDrawableComponent.create(mContext).build();
    Assert.assertEquals(component.getKey(), component.getTypeId() + "");
    Assert.assertNull(component.getGlobalKey());
  }

  @Test
  public void testComponentManualKey() {
    Component component = TestDrawableComponent.create(mContext).key("someKey").build();
    Assert.assertEquals(component.getKey(), "someKey");
    Assert.assertNull(component.getGlobalKey());
  }

  @Test
  public void testRootComponentGlobalKey() {
    final Component component = TestDrawableComponent.create(mContext).build();
    final ComponentTree componentTree =
        ComponentTree.create(mContext, component)
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();
    final LithoView lithoView = getLithoView(componentTree);

    Assert.assertEquals(
        lithoView.getMountItemAt(0).getComponent().getGlobalKey(), component.getKey());
  }

  @Test
  public void testRootComponentGlobalKeyManualKey() {
    final Component component = TestDrawableComponent.create(mContext).key("someKey").build();
    final ComponentTree componentTree =
        ComponentTree.create(mContext, component)
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();
    final LithoView lithoView = getLithoView(componentTree);

    Assert.assertEquals(lithoView.getMountItemAt(0).getComponent().getGlobalKey(), "someKey");
  }

  @Test
  public void testMultipleChildrenComponentKey() {
    final Component component = getMultipleChildrenComponent();

    int layoutSpecId = component.getTypeId();
    int nestedLayoutSpecId = layoutSpecId - 1;

    final Component column = Column.create(mContext).build();
    final int columnSpecId = column.getTypeId();

    final ComponentTree componentTree =
        ComponentTree.create(mContext, component)
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();
    final LithoView lithoView = getLithoView(componentTree);

    // Text
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnSpecId, "[Text2]"),
        getComponentAt(lithoView, 0).getGlobalKey());
    // TestViewComponent in child layout
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(
            layoutSpecId, columnSpecId, nestedLayoutSpecId, columnSpecId, "[TestViewComponent1]"),
        getComponentAt(lithoView, 1).getGlobalKey());
    //background in child
    Assert.assertNull(getComponentAt(lithoView, 2).getGlobalKey());
    // CardClip in child
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(
            layoutSpecId,
            columnSpecId,
            nestedLayoutSpecId,
            columnSpecId,
            columnSpecId,
            "[CardClip1]"),
        getComponentAt(lithoView, 3).getGlobalKey());
    // Text in child
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(
            layoutSpecId, columnSpecId, nestedLayoutSpecId, columnSpecId, "[Text1]"),
        getComponentAt(lithoView, 4).getGlobalKey());
    // background
    Assert.assertNull(getComponentAt(lithoView, 5).getGlobalKey());
    // CardClip
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(
            layoutSpecId, columnSpecId, columnSpecId, "[CardClip2]"),
        getComponentAt(lithoView, 6).getGlobalKey());
    // TestViewComponent
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnSpecId, "[TestViewComponent2]"),
        getComponentAt(lithoView, 7).getGlobalKey());
  }

  @Test
  public void testSiblingsUniqueKeyRequirement() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Text.create(c).text("").key("sameKey"))
                .child(Text.create(c).text("").key("sameKey"))
                .build();
          }
        };

    final ComponentTree componentTree =
        ComponentTree.create(mContext, component)
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();
    getLithoView(componentTree);

    final LogEvent event = mComponentsLogger.newEvent(EVENT_WARNING);

    final String expectedError =
        "The manual key "
            + "sameKey you are setting on "
            + "this Text is a duplicate and will be changed into a unique one. This will "
            + "result in unexpected behavior if you don't change it.";

    event.addParam(PARAM_MESSAGE, expectedError);

    verify(mComponentsLogger).log(eq(event));
  }

  @Test
  public void testColumnSiblingsUniqueKeyRequirement() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Column.create(c).key("sameKey"))
                .child(Column.create(c).key("sameKey"))
                .build();
          }
        };

    final ComponentTree componentTree =
        ComponentTree.create(mContext, component)
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();
    getLithoView(componentTree);

    final LogEvent event = mComponentsLogger.newEvent(EVENT_WARNING);

    final String expectedError =
        "The manual key "
            + "sameKey you are setting on "
            + "this Column is a duplicate and will be changed into a unique one. This will "
            + "result in unexpected behavior if you don't change it.";

    event.addParam(PARAM_MESSAGE, expectedError);

    verify(mComponentsLogger).log(eq(event));
  }

  @Test
  public void testAutogenSiblingsUniqueKeys() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Text.create(mContext).text(""))
                .child(Text.create(mContext).text(""))
                .build();
          }
        };

    final int layoutSpecId = component.getTypeId();
    final Component text = Text.create(mContext).text("").build();
    final int textSpecId = text.getTypeId();
    final Component column = Column.create(mContext).build();
    final int columnTypeId = column.getTypeId();

    final ComponentTree componentTree =
        ComponentTree.create(mContext, component)
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();
    final LithoView lithoView = getLithoView(componentTree);

    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnTypeId, textSpecId),
        getComponentAt(lithoView, 0).getGlobalKey());
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnTypeId, textSpecId + "!0"),
        getComponentAt(lithoView, 1).getGlobalKey());
  }

  @Test
  public void testAutogenColumnSiblingsUniqueKeys() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Column.create(mContext).child(Text.create(c).text("")))
                .child(Column.create(mContext).child(Text.create(c).text("")))
                .build();
          }
        };

    final int layoutSpecId = component.getTypeId();
    final Component text = Text.create(mContext).text("").build();
    final int textSpecId = text.getTypeId();
    final Component column = Column.create(mContext).build();
    final int columnTypeId = column.getTypeId();

    final ComponentTree componentTree =
        ComponentTree.create(mContext, component)
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();
    final LithoView lithoView = getLithoView(componentTree);

    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnTypeId, columnTypeId, textSpecId),
        getComponentAt(lithoView, 0).getGlobalKey());
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(
            layoutSpecId, columnTypeId, columnTypeId + "!0", textSpecId),
        getComponentAt(lithoView, 1).getGlobalKey());
  }

  @Test
  public void testAutogenSiblingsUniqueKeysNested() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Text.create(mContext).text(""))
                .child(Text.create(mContext).text(""))
                .build();
          }
        };

    final Component root =
        new InlineLayoutSpec() {
          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(component)
                .child(Text.create(mContext).text(""))
                .child(Text.create(mContext).text(""))
                .build();
          }
        };

    final int layoutSpecId = root.getTypeId();
    final int nestedLayoutSpecId = component.getTypeId();
    final Component text = Text.create(mContext).text("").build();
    final int textSpecId = text.getTypeId();
    final Component column = Column.create(mContext).build();
    final int columnTypeId = column.getTypeId();

    final ComponentTree componentTree =
        ComponentTree.create(mContext, root).incrementalMount(false).layoutDiffing(false).build();
    LithoView lithoView = getLithoView(componentTree);

    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(
            layoutSpecId, columnTypeId, nestedLayoutSpecId, columnTypeId, textSpecId),
        getComponentAt(lithoView, 0).getGlobalKey());
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(
            layoutSpecId, columnTypeId, nestedLayoutSpecId, columnTypeId, textSpecId + "!0"),
        getComponentAt(lithoView, 1).getGlobalKey());
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnTypeId, textSpecId),
        getComponentAt(lithoView, 2).getGlobalKey());
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnTypeId, textSpecId + "!0"),
        getComponentAt(lithoView, 3).getGlobalKey());
  }

  @Test
  public void testOwnerGlobalKey() {
    final Component root = getMultipleChildrenComponent();

    final int layoutSpecId = root.getTypeId();
    final int nestedLayoutSpecId = layoutSpecId - 1;
    final int columnSpecId = Column.create(mContext).build().getTypeId();

    final ComponentTree componentTree =
        ComponentTree.create(mContext, root).incrementalMount(false).layoutDiffing(false).build();
    final LithoView lithoView = getLithoView(componentTree);

    final String rootGlobalKey = ComponentKeyUtils.getKeyWithSeparator(layoutSpecId);
    final String nestedLayoutGlobalKey =
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnSpecId, nestedLayoutSpecId);

    // Text
    Assert.assertEquals(rootGlobalKey, getComponentAt(lithoView, 0).getOwnerGlobalKey());

    // TestViewComponent in child layout
    Assert.assertEquals(nestedLayoutGlobalKey, getComponentAt(lithoView, 1).getOwnerGlobalKey());

    // CardClip in child
    Assert.assertEquals(nestedLayoutGlobalKey, getComponentAt(lithoView, 3).getOwnerGlobalKey());

    // Text in child
    Assert.assertEquals(nestedLayoutGlobalKey, getComponentAt(lithoView, 4).getOwnerGlobalKey());

    // CardClip
    Assert.assertEquals(rootGlobalKey, getComponentAt(lithoView, 6).getOwnerGlobalKey());

    // TestViewComponent
    Assert.assertEquals(rootGlobalKey, getComponentAt(lithoView, 7).getOwnerGlobalKey());
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
    final Component testGlobalKeyChildComponent =
        new InlineLayoutSpec() {

          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {

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

    final Component testGlobalKeyChild =
        new InlineLayoutSpec() {

          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {

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
