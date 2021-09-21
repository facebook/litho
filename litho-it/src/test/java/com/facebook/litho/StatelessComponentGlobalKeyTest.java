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
import static com.facebook.litho.LithoKeyTestingUtil.getScopedComponentInfos;
import static org.assertj.core.api.Assertions.assertThat;

import android.util.Pair;
import android.view.View;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.logging.TestComponentsReporter;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.CardClip;
import com.facebook.litho.widget.EditText;
import com.facebook.litho.widget.SimpleMountSpecTester;
import com.facebook.litho.widget.Text;
import com.facebook.litho.widget.TextInput;
import com.facebook.rendercore.LogLevel;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class StatelessComponentGlobalKeyTest {

  private static final String mLogTag = "logTag";

  private final boolean mUseStatelessComponentDefault;

  private ComponentContext mContext;
  private TestComponentsReporter mComponentsReporter;

  public StatelessComponentGlobalKeyTest() {
    mUseStatelessComponentDefault = ComponentsConfiguration.useStatelessComponent;
  }

  @Before
  public void setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true);
    ComponentsConfiguration.useStatelessComponent = true;
    mComponentsReporter = new TestComponentsReporter();
    mContext = new ComponentContext(getApplicationContext());
    ComponentsReporter.provide(mComponentsReporter);
  }

  @After
  public void cleanup() {
    ComponentsConfiguration.useStatelessComponent = mUseStatelessComponentDefault;
  }

  @Test
  public void testComponentKey() {
    Component component = SimpleMountSpecTester.create(mContext).build();
    Assert.assertEquals(component.getKey(), component.getTypeId() + "");
  }

  @Test
  public void testComponentManualKey() {
    Component component = SimpleMountSpecTester.create(mContext).key("someKey").build();
    Assert.assertEquals(component.getKey(), "someKey");
  }

  @Test
  public void testRootComponentGlobalKey() {
    final Component component =
        SimpleMountSpecTester.create(mContext).widthDip(10).heightDip(10).build();
    final LithoView lithoView = getLithoView(component);
    final Map<String, List<ScopedComponentInfo>> scopedComponentInfos =
        getScopedComponentInfos(lithoView);

    final ScopedComponentInfo rootScopedComponentInfo =
        scopedComponentInfos.get("SimpleMountSpecTester").get(0);

    final ComponentContext scopedContext = rootScopedComponentInfo.getContext();
    assertThat(scopedContext).isNotNull();
    assertThat(scopedContext.getGlobalKey()).isEqualTo(component.getKey());
  }

  @Test
  public void testRootComponentGlobalKeyManualKey() {
    final Component component =
        SimpleMountSpecTester.create(mContext).widthDip(10).heightDip(10).key("someKey").build();
    final LithoView lithoView = getLithoView(component);
    final Map<String, List<ScopedComponentInfo>> scopedComponentInfos =
        getScopedComponentInfos(lithoView);

    final ScopedComponentInfo rootScopedComponentInfo =
        scopedComponentInfos.get("SimpleMountSpecTester").get(0);

    final ComponentContext scopedContext = rootScopedComponentInfo.getContext();
    assertThat(scopedContext).isNotNull();
    assertThat(scopedContext.getGlobalKey()).isEqualTo("$someKey");
  }

  @Test
  public void testMultipleChildrenComponentKey() {
    final Component component = getMultipleChildrenComponent();

    int layoutSpecId = component.getTypeId();
    int nestedLayoutSpecId = layoutSpecId - 1;

    final Component column = Column.create(mContext).build();
    final int columnSpecId = column.getTypeId();

    final LithoView lithoView = getLithoView(component);

    final Map<String, List<ScopedComponentInfo>> scopedComponentInfos =
        getScopedComponentInfos(lithoView);

    // Text
    final String expectedGlobalKeyText =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "$[Text2]");
    final ScopedComponentInfo scopedComponentInfoText = scopedComponentInfos.get("Text").get(0);
    Assert.assertEquals(scopedComponentInfoText.getContext().getGlobalKey(), expectedGlobalKeyText);
    // TestViewComponent in child layout
    final String expectedGlobalKeyTestViewComponent =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, nestedLayoutSpecId, columnSpecId, "$[TestViewComponent1]");
    final ScopedComponentInfo scopedComponentInfoTestViewComponent =
        scopedComponentInfos.get("TestViewComponent").get(0);
    Assert.assertEquals(
        scopedComponentInfoTestViewComponent.getContext().getGlobalKey(),
        expectedGlobalKeyTestViewComponent);
    // CardClip in child
    final String expectedGlobalKeyCardClip =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId,
            columnSpecId,
            nestedLayoutSpecId,
            columnSpecId,
            columnSpecId,
            "$[CardClip1]");

    final ScopedComponentInfo scopedComponentInfoCardClip =
        scopedComponentInfos.get("CardClip").get(0);
    Assert.assertEquals(
        scopedComponentInfoCardClip.getContext().getGlobalKey(), expectedGlobalKeyCardClip);

    // Text in child
    final String expectedGlobalKeyTextChild =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, nestedLayoutSpecId, columnSpecId, "$[Text1]");
    final ScopedComponentInfo scopedComponentInfoTextChild =
        scopedComponentInfos.get("Text").get(1);
    Assert.assertEquals(
        scopedComponentInfoTextChild.getContext().getGlobalKey(), expectedGlobalKeyTextChild);

    // CardClip
    final String expectedGlobalKeyCardClip2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, columnSpecId, "$[CardClip2]");
    final ScopedComponentInfo scopedComponentInfoCardClip2 =
        scopedComponentInfos.get("CardClip").get(1);
    Assert.assertEquals(
        scopedComponentInfoCardClip2.getContext().getGlobalKey(), expectedGlobalKeyCardClip2);

    // TestViewComponent
    final String expectedGlobalKeyTestViewComponent2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnSpecId, "$[TestViewComponent2]");
    final ScopedComponentInfo scopedComponentInfoTestViewComponent2 =
        scopedComponentInfos.get("TestViewComponent").get(1);
    Assert.assertEquals(
        scopedComponentInfoTestViewComponent2.getContext().getGlobalKey(),
        expectedGlobalKeyTestViewComponent2);
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
        .contains(Pair.create(LogLevel.WARNING, expectedError));
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
    final Map<String, List<ScopedComponentInfo>> scopedComponentInfos =
        getScopedComponentInfos(lithoView);

    final Component column = Column.create(mContext).build();
    final int columnSpecId = column.getTypeId();
    int layoutSpecId = component.getTypeId();

    final String expectedKey0 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "$sameKey");
    final ScopedComponentInfo firstEditTextSCI = scopedComponentInfos.get("EditText").get(0);
    assertThat(firstEditTextSCI.getContext().getGlobalKey()).isEqualTo(expectedKey0);

    final String expectedKey2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "$sameKey!1");
    final ScopedComponentInfo secondEditTextSCI = scopedComponentInfos.get("EditText").get(2);
    assertThat(secondEditTextSCI.getContext().getGlobalKey()).isEqualTo(expectedKey2);

    final String expectedKey3 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "$sameKey!2");
    final ScopedComponentInfo thirdEditTextSCI = scopedComponentInfos.get("EditText").get(3);
    assertThat(thirdEditTextSCI.getContext().getGlobalKey()).isEqualTo(expectedKey3);
  }

  @Test
  public void testSiblingsOfDifferentTypesManualKeyDeduplication() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Text.create(c).text("").key("sameKey").widthDip(10).heightDip(10))
                .child(Text.create(c).text("").widthDip(10).heightDip(10))
                .child(TestViewComponent.create(c).widthDip(10).heightDip(10).key("sameKey"))
                .child(
                    TextInput.create(c).initialText("").key("sameKey").widthDip(10).heightDip(10))
                .build();
          }
        };

    LithoView lithoView = getLithoView(component);

    final Component column = Column.create(mContext).build();
    final int columnSpecId = column.getTypeId();
    final int layoutSpecId = component.getTypeId();

    final Map<String, List<ScopedComponentInfo>> scopedComponentInfos =
        getScopedComponentInfos(lithoView);

    final String expectedKey0 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "$sameKey");
    final ScopedComponentInfo firstTextSCI = scopedComponentInfos.get("Text").get(0);
    assertThat(firstTextSCI.getContext().getGlobalKey()).isEqualTo(expectedKey0);

    final String expectedKey2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "$sameKey!1");
    final ScopedComponentInfo firstTextViewComponentSCI =
        scopedComponentInfos.get("TestViewComponent").get(0);
    assertThat(firstTextViewComponentSCI.getContext().getGlobalKey()).isEqualTo(expectedKey2);

    final String expectedKey3 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnSpecId, "$sameKey!2");
    final ScopedComponentInfo firstTextInputSCI = scopedComponentInfos.get("TextInput").get(0);
    assertThat(firstTextInputSCI.getContext().getGlobalKey()).isEqualTo(expectedKey3);
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
        .contains(Pair.create(LogLevel.WARNING, expectedError));
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
    final Map<String, List<ScopedComponentInfo>> scopedComponentInfos =
        getScopedComponentInfos(lithoView);

    final String expectedKey0 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, textSpecId);
    final ScopedComponentInfo firstTextSCI = scopedComponentInfos.get("Text").get(0);
    assertThat(firstTextSCI.getContext().getGlobalKey()).isEqualTo(expectedKey0);

    final String expectedKey1 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, textSpecId + "!1");
    final ScopedComponentInfo secondTextSCI = scopedComponentInfos.get("Text").get(1);
    assertThat(secondTextSCI.getContext().getGlobalKey()).isEqualTo(expectedKey1);
  }

  @Test
  public void testAutogenSiblingsUniqueKeysSkipsManualKeys() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Text.create(c).text("").widthDip(10).heightDip(10))
                .child(Text.create(c).text("").widthDip(10).heightDip(10))
                .child(Text.create(c).text("").widthDip(10).heightDip(10).key("manual_key"))
                .child(Text.create(c).text("").widthDip(10).heightDip(10))
                .build();
          }
        };

    final LithoView lithoView = getLithoView(component);
    final Map<String, List<ScopedComponentInfo>> scopedComponentInfos =
        getScopedComponentInfos(lithoView);

    final ScopedComponentInfo firstSCI = scopedComponentInfos.get("Text").get(0);
    final ScopedComponentInfo secondSCI = scopedComponentInfos.get("Text").get(1);
    final ScopedComponentInfo fourthSCI = scopedComponentInfos.get("Text").get(3);

    final String firstKey = firstSCI.getContext().getGlobalKey();
    final String secondKey = secondSCI.getContext().getGlobalKey();
    final String fourthKey = fourthSCI.getContext().getGlobalKey();

    assertThat(firstKey).isNotBlank();
    assertThat(secondKey).isEqualTo(firstKey + "!1");
    // The third key is a manual key, so will have no impact on the unique suffix
    assertThat(fourthKey).isEqualTo(firstKey + "!2");
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
    final Map<String, List<ScopedComponentInfo>> scopedComponentInfos =
        getScopedComponentInfos(lithoView);

    final String expectedKey0 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnTypeId, columnTypeId, textSpecId);
    final ScopedComponentInfo firstTextSCI = scopedComponentInfos.get("Text").get(0);
    assertThat(firstTextSCI.getContext().getGlobalKey()).isEqualTo(expectedKey0);
    final String expectedKey1 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnTypeId, columnTypeId + "!1", textSpecId);
    final ScopedComponentInfo secondTextSCI = scopedComponentInfos.get("Text").get(1);
    assertThat(secondTextSCI.getContext().getGlobalKey()).isEqualTo(expectedKey1);
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
    final Map<String, List<ScopedComponentInfo>> scopedComponentInfos =
        getScopedComponentInfos(lithoView);

    final String expectedKey0 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnTypeId, nestedLayoutSpecId, columnTypeId, textSpecId);
    final ScopedComponentInfo firstTextSCI = scopedComponentInfos.get("Text").get(0);
    assertThat(firstTextSCI.getContext().getGlobalKey()).isEqualTo(expectedKey0);

    final String expectedKey1 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(
            layoutSpecId, columnTypeId, nestedLayoutSpecId, columnTypeId, textSpecId + "!1");
    final ScopedComponentInfo secondTextSCI = scopedComponentInfos.get("Text").get(1);
    assertThat(secondTextSCI.getContext().getGlobalKey()).isEqualTo(expectedKey1);

    final String expectedKey2 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, textSpecId);
    final ScopedComponentInfo thirdTextSCI = scopedComponentInfos.get("Text").get(2);
    assertThat(thirdTextSCI.getContext().getGlobalKey()).isEqualTo(expectedKey2);

    final String expectedKey3 =
        ComponentKeyUtils.getKeyWithSeparatorForTest(layoutSpecId, columnTypeId, textSpecId + "!1");
    final ScopedComponentInfo fourthTextSCI = scopedComponentInfos.get("Text").get(3);
    assertThat(fourthTextSCI.getContext().getGlobalKey()).isEqualTo(expectedKey3);
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
