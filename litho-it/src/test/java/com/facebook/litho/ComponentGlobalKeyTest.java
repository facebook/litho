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

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.util.Pair;
import android.view.View;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.logging.TestComponentsReporter;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.litho.widget.CardClip;
import com.facebook.litho.widget.EditText;
import com.facebook.litho.widget.Text;
import com.facebook.litho.widget.TreePropTestContainerComponentSpec;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentGlobalKeyTest {

  private static final String mLogTag = "logTag";

  private ComponentContext mContext;
  private TestComponentsReporter mComponentsReporter;

  @Before
  public void setup() {
    mComponentsReporter = new TestComponentsReporter();
    mContext = new ComponentContext(RuntimeEnvironment.application);
    ComponentsReporter.provide(mComponentsReporter);
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
    final Component component =
        TestDrawableComponent.create(mContext).widthDip(10).heightDip(10).build();
    final LithoView lithoView = getLithoView(component);

    Assert.assertEquals(
        lithoView.getMountItemAt(0).getComponent().getGlobalKey(), component.getKey());
  }

  @Test
  public void testRootComponentGlobalKeyManualKey() {
    final Component component =
        TestDrawableComponent.create(mContext).widthDip(10).heightDip(10).key("someKey").build();
    final LithoView lithoView = getLithoView(component);

    Assert.assertEquals(lithoView.getMountItemAt(0).getComponent().getGlobalKey(), "someKey");
  }

  @Test
  public void testMultipleChildrenComponentKey() {
    final Component component = getMultipleChildrenComponent();

    int layoutSpecId = component.getTypeId();
    int nestedLayoutSpecId = layoutSpecId - 1;

    final Component column = Column.create(mContext).build();
    final int columnSpecId = column.getTypeId();

    final LithoView lithoView = getLithoView(component);

    // Text
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnSpecId, "[Text2]"),
        getComponentAt(lithoView, 0).getGlobalKey());
    // TestViewComponent in child layout
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(
            layoutSpecId, columnSpecId, nestedLayoutSpecId, columnSpecId, "[TestViewComponent1]"),
        getComponentAt(lithoView, 1).getGlobalKey());
    // background in child
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

    getLithoView(component);

    final String expectedError =
        "The manual key "
            + "sameKey you are setting on "
            + "this Text is a duplicate and will be changed into a unique one. This will "
            + "result in unexpected behavior if you don't change it.";

    assertThat(mComponentsReporter.getLoggedMessages())
        .contains(Pair.create(ComponentsReporter.LogLevel.WARNING, expectedError));
  }

  @Test
  public void testSiblingsManualKeyDeduplication() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(EditText.create(c).text("").key("sameKey").widthDip(10).heightDip(10))
                .child(EditText.create(c).text("").widthDip(10).heightDip(10))
                .child(EditText.create(c).text("").key("sameKey").widthDip(10).heightDip(10))
                .child(EditText.create(c).text("").key("sameKey").widthDip(10).heightDip(10))
                .build();
          }
        };

    LithoView lithoView = getLithoView(component);

    final Component column = Column.create(mContext).build();
    final int columnSpecId = column.getTypeId();
    int layoutSpecId = component.getTypeId();

    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnSpecId, "sameKey"),
        getComponentAt(lithoView, 0).getGlobalKey());

    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnSpecId, "sameKey!1"),
        getComponentAt(lithoView, 2).getGlobalKey());

    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnSpecId, "sameKey!2"),
        getComponentAt(lithoView, 3).getGlobalKey());
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

    getLithoView(component);

    final String expectedError =
        "The manual key "
            + "sameKey you are setting on "
            + "this Column is a duplicate and will be changed into a unique one. This will "
            + "result in unexpected behavior if you don't change it.";

    assertThat(mComponentsReporter.getLoggedMessages())
        .contains(Pair.create(ComponentsReporter.LogLevel.WARNING, expectedError));
  }

  @Test
  public void testAutogenSiblingsUniqueKeys() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Text.create(mContext).widthDip(10).heightDip(10).text(""))
                .child(Text.create(mContext).widthDip(10).heightDip(10).text(""))
                .build();
          }
        };

    final int layoutSpecId = component.getTypeId();
    final Component text = Text.create(mContext).text("").build();
    final int textSpecId = text.getTypeId();
    final Component column = Column.create(mContext).build();
    final int columnTypeId = column.getTypeId();

    final LithoView lithoView = getLithoView(component);

    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnTypeId, textSpecId),
        getComponentAt(lithoView, 0).getGlobalKey());
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnTypeId, textSpecId + "!1"),
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
                .child(
                    Column.create(mContext)
                        .child(Text.create(c).widthDip(10).heightDip(10).text("")))
                .child(
                    Column.create(mContext)
                        .child(Text.create(c).widthDip(10).heightDip(10).text("")))
                .build();
          }
        };

    final int layoutSpecId = component.getTypeId();
    final Component text = Text.create(mContext).text("").build();
    final int textSpecId = text.getTypeId();
    final Component column = Column.create(mContext).build();
    final int columnTypeId = column.getTypeId();

    final LithoView lithoView = getLithoView(component);

    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnTypeId, columnTypeId, textSpecId),
        getComponentAt(lithoView, 0).getGlobalKey());
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(
            layoutSpecId, columnTypeId, columnTypeId + "!1", textSpecId),
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
                .child(Text.create(mContext).widthDip(10).heightDip(10).text(""))
                .child(Text.create(mContext).widthDip(10).heightDip(10).text(""))
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
                .child(Text.create(mContext).widthDip(10).heightDip(10).text("test"))
                .child(Text.create(mContext).widthDip(10).heightDip(10).text("test"))
                .build();
          }
        };

    final int layoutSpecId = root.getTypeId();
    final int nestedLayoutSpecId = component.getTypeId();
    final Component text = Text.create(mContext).text("").build();
    final int textSpecId = text.getTypeId();
    final Component column = Column.create(mContext).build();
    final int columnTypeId = column.getTypeId();

    LithoView lithoView = getLithoView(root);

    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(
            layoutSpecId, columnTypeId, nestedLayoutSpecId, columnTypeId, textSpecId),
        getComponentAt(lithoView, 0).getGlobalKey());
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(
            layoutSpecId, columnTypeId, nestedLayoutSpecId, columnTypeId, textSpecId + "!1"),
        getComponentAt(lithoView, 1).getGlobalKey());
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnTypeId, textSpecId),
        getComponentAt(lithoView, 2).getGlobalKey());
    Assert.assertEquals(
        ComponentKeyUtils.getKeyWithSeparator(layoutSpecId, columnTypeId, textSpecId + "!1"),
        getComponentAt(lithoView, 3).getGlobalKey());
  }

  @Test
  public void testOwnerGlobalKey() {
    final Component root = getMultipleChildrenComponent();

    final int layoutSpecId = root.getTypeId();
    final int nestedLayoutSpecId = layoutSpecId - 1;
    final int columnSpecId = Column.create(mContext).build().getTypeId();

    final LithoView lithoView = getLithoView(root);

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

  @Test
  public void nestedTreeRemeasureKeyStabilityTest() {
    final Component componentWithoutRemeasure = TreePropTestContainerComponentSpec.create(mContext);
    final LithoView lithoView = getLithoView(componentWithoutRemeasure);
    Assert.assertEquals(
        TreePropTestContainerComponentSpec.EXPECTED_GLOBAL_KEY,
        getComponentAt(lithoView, 1).getOwnerGlobalKey());
  }

  private static Component getComponentAt(LithoView lithoView, int index) {
    return lithoView.getMountItemAt(index).getComponent();
  }

  private LithoView getLithoView(Component component) {
    LithoView lithoView = new LithoView(mContext);
    lithoView.setComponent(component);
    lithoView.measure(
        View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.UNSPECIFIED));
    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());
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
                .child(
                    TestViewComponent.create(c)
                        .widthDip(10)
                        .heightDip(10)
                        .key("[TestViewComponent1]"))
                .child(
                    Column.create(c)
                        .backgroundColor(color)
                        .child(CardClip.create(c).widthDip(10).heightDip(10).key("[CardClip1]")))
                .child(Text.create(c).text("Test").widthDip(10).heightDip(10).key("[Text1]"))
                .build();
          }
        };

    final Component testGlobalKeyChild =
        new InlineLayoutSpec() {

          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {

            return Column.create(c)
                .child(Text.create(c).text("test").widthDip(10).heightDip(10).key("[Text2]"))
                .child(testGlobalKeyChildComponent)
                .child(
                    Column.create(c)
                        .backgroundColor(color)
                        .child(CardClip.create(c).widthDip(10).heightDip(10).key("[CardClip2]")))
                .child(
                    TestViewComponent.create(c)
                        .widthDip(10)
                        .heightDip(10)
                        .key("[TestViewComponent2]"))
                .build();
          }
        };

    return testGlobalKeyChild;
  }
}
