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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.view.View;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.CardClip;
import com.facebook.litho.widget.Text;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LegacyStatelessComponentGlobalKeyTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(false);
    ComponentsConfiguration.useStatelessComponent = true;
    mContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testMultipleChildrenComponentKey() {
    final Component component = getMultipleChildrenComponent();

    int layoutSpecId = component.getTypeId();
    int nestedLayoutSpecId = layoutSpecId - 1;

    final Component column = Column.create(mContext).build();
    final int columnSpecId = column.getTypeId();

    final LithoView lithoView = getLithoView(component);
    final LayoutStateContext layoutStateContext =
        lithoView.getComponentTree().getLayoutStateContext();

    // Text
    final String expectedGlobalKeyText =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "$[Text2]");
    final String globalKeyText = getComponentGlobalKeyAt(lithoView, 0);
    Assert.assertEquals(globalKeyText, expectedGlobalKeyText);
    assertThat(layoutStateContext.getScopedContext(globalKeyText).getGlobalKey())
        .isEqualTo(expectedGlobalKeyText);
    // TestViewComponent in child layout
    final String expectedGlobalKeyTestViewComponent =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, nestedLayoutSpecId, columnSpecId, "$[TestViewComponent1]");
    final String globalKeyTestViewComponent = getComponentGlobalKeyAt(lithoView, 1);
    Assert.assertEquals(globalKeyTestViewComponent, expectedGlobalKeyTestViewComponent);
    assertThat(layoutStateContext.getScopedContext(globalKeyTestViewComponent).getGlobalKey())
        .isEqualTo(expectedGlobalKeyTestViewComponent);
    // background in child
    Assert.assertNull(getComponentGlobalKeyAt(lithoView, 2));
    // CardClip in child
    final String expectedGlobalKeyCardClip =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnSpecId,
            nestedLayoutSpecId,
            columnSpecId,
            columnSpecId,
            "$[CardClip1]");
    final String globalKeyCardClip = getComponentGlobalKeyAt(lithoView, 3);
    Assert.assertEquals(globalKeyCardClip, expectedGlobalKeyCardClip);
    assertThat(layoutStateContext.getScopedContext(globalKeyCardClip).getGlobalKey())
        .isEqualTo(expectedGlobalKeyCardClip);

    // Text in child
    final String expectedGlobalKeyTextChild =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, nestedLayoutSpecId, columnSpecId, "$[Text1]");
    final String globalKeyTextChild = getComponentGlobalKeyAt(lithoView, 4);
    Assert.assertEquals(globalKeyTextChild, expectedGlobalKeyTextChild);
    assertThat(layoutStateContext.getScopedContext(globalKeyTextChild).getGlobalKey())
        .isEqualTo(expectedGlobalKeyTextChild);

    // background
    Assert.assertNull(getComponentGlobalKeyAt(lithoView, 5));
    // CardClip
    final String expectedGlobalKeyCardClip2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, columnSpecId, "$[CardClip2]");
    final String globalKeyCardClip2 = getComponentGlobalKeyAt(lithoView, 6);
    Assert.assertEquals(globalKeyCardClip2, expectedGlobalKeyCardClip2);
    assertThat(layoutStateContext.getScopedContext(globalKeyCardClip2).getGlobalKey())
        .isEqualTo(expectedGlobalKeyCardClip2);

    // TestViewComponent
    final String expectedGlobalKeyTestViewComponent2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, "$[TestViewComponent2]");
    final String globalKeyTestViewComponent2 = getComponentGlobalKeyAt(lithoView, 7);
    Assert.assertEquals(globalKeyTestViewComponent2, expectedGlobalKeyTestViewComponent2);
    assertThat(layoutStateContext.getScopedContext(globalKeyTestViewComponent2).getGlobalKey())
        .isEqualTo(expectedGlobalKeyTestViewComponent2);
  }

  private static String getComponentGlobalKeyAt(LithoView lithoView, int index) {
    return getLayoutOutput(lithoView.getMountItemAt(index)).getKey();
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

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }
}
