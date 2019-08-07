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

package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.Section;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.google.testing.compile.CompilationRule;
import java.util.List;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests {@link BuilderGenerator} */
@RunWith(JUnit4.class)
public class BuilderGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  @Mock Messager mMessager;

  private final LayoutSpecModelFactory mLayoutSpecModelFactory = new LayoutSpecModelFactory();

  @LayoutSpec
  static class TestSpec {
    @PropDefault protected static boolean arg0 = true;

    @PropDefault(resType = ResType.DIMEN_SIZE, resId = 12345)
    protected static float arg5;

    @OnCreateLayout
    public void testDelegateMethod(
        @Prop boolean arg0,
        @Prop Section section,
        @State int arg1,
        @Param Object arg2,
        @Prop(optional = true) boolean arg3,
        @Prop(varArg = "name") List<String> names,
        @Prop(optional = true) float arg5) {}

    @OnEvent(Object.class)
    public void testEventMethod(@Prop boolean arg0) {}

    @OnUpdateState
    public void testUpdateStateMethod() {}
  }

  @LayoutSpec
  static class TestResTypeWithVarArgsSpec {
    @OnCreateLayout
    public void resTypeWithVarArgs(
        @Prop(varArg = "size", resType = ResType.DIMEN_TEXT) List<Float> sizes) {}
  }

  @LayoutSpec
  static class TestDimenResTypeWithBoxFloatArgSpec {
    @OnCreateLayout
    public void dimenResTypeWithBoxFloatArg(@Prop(resType = ResType.DIMEN_TEXT) Float size) {}
  }

  @LayoutSpec
  static class TestKotlinVarArgSpec {
    public static final TestKotlinVarArgSpec INSTANCE = null;

    @OnCreateLayout
    public final Component onCreateLayout(
        ComponentContext c,
        @Prop(varArg = "number") java.util.List<? extends java.lang.Number> numbers) {
      return null;
    }
  }

  private SpecModel mSpecModel;
  private SpecModel mResTypeVarArgsSpecModel;
  private SpecModel mDimenResTypeWithBoxFloatArgSpecModel;
  private SpecModel mKotlinWildcardsVarArgBuildersSpecModel;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();

    TypeElement typeElement = elements.getTypeElement(TestSpec.class.getCanonicalName());
    mSpecModel =
        mLayoutSpecModelFactory.create(
            elements, types, typeElement, mMessager, RunMode.normal(), null, null);

    TypeElement resWithVarArgsElement =
        elements.getTypeElement(TestResTypeWithVarArgsSpec.class.getCanonicalName());
    mResTypeVarArgsSpecModel =
        mLayoutSpecModelFactory.create(
            elements, types, resWithVarArgsElement, mMessager, RunMode.normal(), null, null);

    TypeElement dimenResTypeWithBoxFloatArgElement =
        elements.getTypeElement(TestDimenResTypeWithBoxFloatArgSpec.class.getCanonicalName());
    mDimenResTypeWithBoxFloatArgSpecModel =
        mLayoutSpecModelFactory.create(
            elements,
            types,
            dimenResTypeWithBoxFloatArgElement,
            mMessager,
            RunMode.normal(),
            null,
            null);

    TypeElement typeElementKotlinVarArgsWildcards =
        elements.getTypeElement(TestKotlinVarArgSpec.class.getCanonicalName());
    mKotlinWildcardsVarArgBuildersSpecModel =
        mLayoutSpecModelFactory.create(
            elements,
            types,
            typeElementKotlinVarArgsWildcards,
            mMessager,
            RunMode.normal(),
            null,
            null);
  }

  @Test
  public void testGenerate() {
    TypeSpecDataHolder dataHolder = BuilderGenerator.generate(mSpecModel);

    assertThat(dataHolder.getMethodSpecs()).hasSize(2);
    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "public static Builder create(com.facebook.litho.ComponentContext context) {\n"
                + "  return create(context, 0, 0);\n"
                + "}\n");
    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "public static Builder create(com.facebook.litho.ComponentContext context, int defStyleAttr,\n"
                + "    int defStyleRes) {\n"
                + "  final Builder builder = new Builder();\n"
                + "  Test instance = new Test();\n"
                + "  builder.init(context, defStyleAttr, defStyleRes, instance);\n"
                + "  return builder;\n"
                + "}\n");

    assertThat(dataHolder.getFieldSpecs()).hasSize(0);
    assertThat(dataHolder.getTypeSpecs()).hasSize(1);
    assertThat(dataHolder.getTypeSpecs().get(0).toString())
        .isEqualTo(
            "public static final class Builder extends com.facebook.litho.Component.Builder<Builder> {\n"
                + "  Test mTest;\n"
                + "\n"
                + "  com.facebook.litho.ComponentContext mContext;\n"
                + "\n"
                + "  private final java.lang.String[] REQUIRED_PROPS_NAMES = new String[] {\"arg0\", \"names\", \"section\"};\n"
                + "\n"
                + "  private final int REQUIRED_PROPS_COUNT = 3;\n"
                + "\n"
                + "  private final java.util.BitSet mRequired = new java.util.BitSet(REQUIRED_PROPS_COUNT);\n"
                + "\n"
                + "  private void init(com.facebook.litho.ComponentContext context, int defStyleAttr, int defStyleRes,\n"
                + "      Test testRef) {\n"
                + "    super.init(context, defStyleAttr, defStyleRes, testRef);\n"
                + "    mTest = testRef;\n"
                + "    mContext = context;\n"
                + "    initPropDefaults();\n"
                + "    mRequired.clear();\n"
                + "  }\n"
                + "\n"
                + "  void initPropDefaults() {\n"
                + "    this.mTest.arg5 = mResourceResolver.resolveDimenSizeRes(12345);\n"
                + "  }\n"
                + "\n"
                + "  public Builder arg0(boolean arg0) {\n"
                + "    this.mTest.arg0 = arg0;\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder arg3(boolean arg3) {\n"
                + "    this.mTest.arg3 = arg3;\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder arg5(float arg5) {\n"
                + "    this.mTest.arg5 = arg5;\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder name(java.lang.String name) {\n"
                + "    if (name == null) {\n"
                + "      return this;\n"
                + "    }\n"
                + "    if (this.mTest.names == null) {\n"
                + "      this.mTest.names = new java.util.ArrayList<java.lang.String>();\n"
                + "    }\n"
                + "    this.mTest.names.add(name);\n"
                + "    mRequired.set(1);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder names(java.util.List<java.lang.String> names) {\n"
                + "    if (names == null) {\n"
                + "      return this;\n"
                + "    }\n"
                + "    if (this.mTest.names == null || this.mTest.names.isEmpty()) {\n"
                + "      this.mTest.names = names;\n"
                + "    } else {\n"
                + "      this.mTest.names.addAll(names);\n"
                + "    }\n"
                + "    mRequired.set(1);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder section(com.facebook.litho.sections.Section section) {\n"
                + "    this.mTest.section = section;\n"
                + "    mRequired.set(2);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder section(com.facebook.litho.sections.Section.Builder<?> sectionBuilder) {\n"
                + "    this.mTest.section = sectionBuilder == null ? null : sectionBuilder.build();\n"
                + "    mRequired.set(2);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public Builder getThis() {\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public com.facebook.litho.specmodels.generator.BuilderGeneratorTest.Test build() {\n"
                + "    checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);\n"
                + "    return mTest;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testResTypeWithVarArgs() {
    TypeSpecDataHolder dataHolder = BuilderGenerator.generate(mResTypeVarArgsSpecModel);
    assertThat(dataHolder.getTypeSpecs()).hasSize(1);
    assertThat(dataHolder.getTypeSpecs().get(0).toString())
        .isEqualTo(
            "public static final class Builder extends com.facebook.litho.Component.Builder<Builder> {\n"
                + "  TestResTypeWithVarArgs mTestResTypeWithVarArgs;\n"
                + "\n"
                + "  com.facebook.litho.ComponentContext mContext;\n"
                + "\n"
                + "  private final java.lang.String[] REQUIRED_PROPS_NAMES = new String[] {\"sizes\"};\n"
                + "\n"
                + "  private final int REQUIRED_PROPS_COUNT = 1;\n"
                + "\n"
                + "  private final java.util.BitSet mRequired = new java.util.BitSet(REQUIRED_PROPS_COUNT);\n"
                + "\n"
                + "  private void init(com.facebook.litho.ComponentContext context, int defStyleAttr, int defStyleRes,\n"
                + "      TestResTypeWithVarArgs testResTypeWithVarArgsRef) {\n"
                + "    super.init(context, defStyleAttr, defStyleRes, testResTypeWithVarArgsRef);\n"
                + "    mTestResTypeWithVarArgs = testResTypeWithVarArgsRef;\n"
                + "    mContext = context;\n"
                + "    mRequired.clear();\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizePx(@androidx.annotation.Px float size) {\n"
                + "    if (this.mTestResTypeWithVarArgs.sizes == null) {\n"
                + "      this.mTestResTypeWithVarArgs.sizes = new java.util.ArrayList<java.lang.Float>();\n"
                + "    }\n"
                + "    final float res = size;\n"
                + "    this.mTestResTypeWithVarArgs.sizes.add(res);\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizesPx(java.util.List<java.lang.Float> sizes) {\n"
                + "    if (sizes == null) {\n"
                + "      return this;\n"
                + "    }\n"
                + "    if (this.mTestResTypeWithVarArgs.sizes == null) {\n"
                + "      this.mTestResTypeWithVarArgs.sizes = new java.util.ArrayList<java.lang.Float>();\n"
                + "    }\n"
                + "    for (int i = 0; i < sizes.size(); i++) {\n"
                + "      final float res = sizes.get(i);\n"
                + "      this.mTestResTypeWithVarArgs.sizes.add(res);\n"
                + "    }\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizeDip(@androidx.annotation.Dimension(unit = androidx.annotation.Dimension.DP) float dip) {\n"
                + "    if (this.mTestResTypeWithVarArgs.sizes == null) {\n"
                + "      this.mTestResTypeWithVarArgs.sizes = new java.util.ArrayList<java.lang.Float>();\n"
                + "    }\n"
                + "    final float res = mResourceResolver.dipsToPixels(dip);\n"
                + "    this.mTestResTypeWithVarArgs.sizes.add(res);\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizesDip(java.util.List<java.lang.Float> dips) {\n"
                + "    if (dips == null) {\n"
                + "      return this;\n"
                + "    }\n"
                + "    if (this.mTestResTypeWithVarArgs.sizes == null) {\n"
                + "      this.mTestResTypeWithVarArgs.sizes = new java.util.ArrayList<java.lang.Float>();\n"
                + "    }\n"
                + "    for (int i = 0; i < dips.size(); i++) {\n"
                + "      final float res = mResourceResolver.dipsToPixels(dips.get(i));\n"
                + "      this.mTestResTypeWithVarArgs.sizes.add(res);\n"
                + "    }\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizeSp(@androidx.annotation.Dimension(unit = androidx.annotation.Dimension.SP) float sip) {\n"
                + "    if (this.mTestResTypeWithVarArgs.sizes == null) {\n"
                + "      this.mTestResTypeWithVarArgs.sizes = new java.util.ArrayList<java.lang.Float>();\n"
                + "    }\n"
                + "    final float res = mResourceResolver.sipsToPixels(sip);\n"
                + "    this.mTestResTypeWithVarArgs.sizes.add(res);\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizesSp(java.util.List<java.lang.Float> sips) {\n"
                + "    if (sips == null) {\n"
                + "      return this;\n"
                + "    }\n"
                + "    if (this.mTestResTypeWithVarArgs.sizes == null) {\n"
                + "      this.mTestResTypeWithVarArgs.sizes = new java.util.ArrayList<java.lang.Float>();\n"
                + "    }\n"
                + "    for (int i = 0; i < sips.size(); i++) {\n"
                + "      final float res = mResourceResolver.sipsToPixels(sips.get(i));\n"
                + "      this.mTestResTypeWithVarArgs.sizes.add(res);\n"
                + "    }\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizeRes(@androidx.annotation.DimenRes int resId) {\n"
                + "    if (this.mTestResTypeWithVarArgs.sizes == null) {\n"
                + "      this.mTestResTypeWithVarArgs.sizes = new java.util.ArrayList<java.lang.Float>();\n"
                + "    }\n"
                + "    final float res = mResourceResolver.resolveDimenSizeRes(resId);\n"
                + "    this.mTestResTypeWithVarArgs.sizes.add(res);\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizesRes(java.util.List<java.lang.Integer> resIds) {\n"
                + "    if (resIds == null) {\n"
                + "      return this;\n"
                + "    }\n"
                + "    if (this.mTestResTypeWithVarArgs.sizes == null) {\n"
                + "      this.mTestResTypeWithVarArgs.sizes = new java.util.ArrayList<java.lang.Float>();\n"
                + "    }\n"
                + "    for (int i = 0; i < resIds.size(); i++) {\n"
                + "      final float res = mResourceResolver.resolveDimenSizeRes(resIds.get(i));\n"
                + "      this.mTestResTypeWithVarArgs.sizes.add(res);\n"
                + "    }\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizeAttr(@androidx.annotation.AttrRes int attrResId,\n"
                + "      @androidx.annotation.DimenRes int defResId) {\n"
                + "    if (this.mTestResTypeWithVarArgs.sizes == null) {\n"
                + "      this.mTestResTypeWithVarArgs.sizes = new java.util.ArrayList<java.lang.Float>();\n"
                + "    }\n"
                + "    final float res = mResourceResolver.resolveDimenSizeAttr(attrResId, defResId);\n"
                + "    this.mTestResTypeWithVarArgs.sizes.add(res);\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizeAttr(@androidx.annotation.AttrRes int attrResId) {\n"
                + "    if (this.mTestResTypeWithVarArgs.sizes == null) {\n"
                + "      this.mTestResTypeWithVarArgs.sizes = new java.util.ArrayList<java.lang.Float>();\n"
                + "    }\n"
                + "    final float res = mResourceResolver.resolveDimenSizeAttr(attrResId, 0);\n"
                + "    this.mTestResTypeWithVarArgs.sizes.add(res);\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizesAttr(java.util.List<java.lang.Integer> attrResIds,\n"
                + "      @androidx.annotation.DimenRes int defResId) {\n"
                + "    if (attrResIds == null) {\n"
                + "      return this;\n"
                + "    }\n"
                + "    if (this.mTestResTypeWithVarArgs.sizes == null) {\n"
                + "      this.mTestResTypeWithVarArgs.sizes = new java.util.ArrayList<java.lang.Float>();\n"
                + "    }\n"
                + "    for (int i = 0; i < attrResIds.size(); i++) {\n"
                + "      final float res = mResourceResolver.resolveDimenSizeAttr(attrResIds.get(i), defResId);\n"
                + "      this.mTestResTypeWithVarArgs.sizes.add(res);\n"
                + "    }\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizesAttr(java.util.List<java.lang.Integer> attrResIds) {\n"
                + "    if (attrResIds == null) {\n"
                + "      return this;\n"
                + "    }\n"
                + "    if (this.mTestResTypeWithVarArgs.sizes == null) {\n"
                + "      this.mTestResTypeWithVarArgs.sizes = new java.util.ArrayList<java.lang.Float>();\n"
                + "    }\n"
                + "    for (int i = 0; i < attrResIds.size(); i++) {\n"
                + "      final float res = mResourceResolver.resolveDimenSizeAttr(attrResIds.get(i), 0);\n"
                + "      this.mTestResTypeWithVarArgs.sizes.add(res);\n"
                + "    }\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public Builder getThis() {\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public com.facebook.litho.specmodels.generator.BuilderGeneratorTest.TestResTypeWithVarArgs build() {\n"
                + "    checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);\n"
                + "    return mTestResTypeWithVarArgs;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testDimenResTypeWithBoxFloatArgSpecModel() {
    TypeSpecDataHolder dataHolder =
        BuilderGenerator.generate(mDimenResTypeWithBoxFloatArgSpecModel);
    assertThat(dataHolder.getTypeSpecs()).hasSize(1);
    assertThat(dataHolder.getTypeSpecs().get(0).toString())
        .isEqualTo(
            "public static final class Builder extends com.facebook.litho.Component.Builder<Builder> {\n"
                + "  TestDimenResTypeWithBoxFloatArg mTestDimenResTypeWithBoxFloatArg;\n"
                + "\n"
                + "  com.facebook.litho.ComponentContext mContext;\n"
                + "\n"
                + "  private final java.lang.String[] REQUIRED_PROPS_NAMES = new String[] {\"size\"};\n"
                + "\n"
                + "  private final int REQUIRED_PROPS_COUNT = 1;\n"
                + "\n"
                + "  private final java.util.BitSet mRequired = new java.util.BitSet(REQUIRED_PROPS_COUNT);\n"
                + "\n"
                + "  private void init(com.facebook.litho.ComponentContext context, int defStyleAttr, int defStyleRes,\n"
                + "      TestDimenResTypeWithBoxFloatArg testDimenResTypeWithBoxFloatArgRef) {\n"
                + "    super.init(context, defStyleAttr, defStyleRes, testDimenResTypeWithBoxFloatArgRef);\n"
                + "    mTestDimenResTypeWithBoxFloatArg = testDimenResTypeWithBoxFloatArgRef;\n"
                + "    mContext = context;\n"
                + "    mRequired.clear();\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizePx(@androidx.annotation.Px float size) {\n"
                + "    this.mTestDimenResTypeWithBoxFloatArg.size = size;\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizeDip(@androidx.annotation.Dimension(unit = androidx.annotation.Dimension.DP) float dip) {\n"
                + "    this.mTestDimenResTypeWithBoxFloatArg.size = (float) mResourceResolver.dipsToPixels(dip);\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizeSp(@androidx.annotation.Dimension(unit = androidx.annotation.Dimension.SP) float sip) {\n"
                + "    this.mTestDimenResTypeWithBoxFloatArg.size = (float) mResourceResolver.sipsToPixels(sip);\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizeRes(@androidx.annotation.DimenRes int resId) {\n"
                + "    this.mTestDimenResTypeWithBoxFloatArg.size = (float) mResourceResolver.resolveDimenSizeRes(resId);\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizeAttr(@androidx.annotation.AttrRes int attrResId,\n"
                + "      @androidx.annotation.DimenRes int defResId) {\n"
                + "    this.mTestDimenResTypeWithBoxFloatArg.size = (float) mResourceResolver.resolveDimenSizeAttr(attrResId, defResId);\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder sizeAttr(@androidx.annotation.AttrRes int attrResId) {\n"
                + "    this.mTestDimenResTypeWithBoxFloatArg.size = (float) mResourceResolver.resolveDimenSizeAttr(attrResId, 0);\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public Builder getThis() {\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public com.facebook.litho.specmodels.generator.BuilderGeneratorTest.TestDimenResTypeWithBoxFloatArg build() {\n"
                + "    checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);\n"
                + "    return mTestDimenResTypeWithBoxFloatArg;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testKotlinVarArgWildcardsGenerate() {
    TypeSpecDataHolder dataHolder =
        BuilderGenerator.generate(mKotlinWildcardsVarArgBuildersSpecModel);
    assertThat(dataHolder.getTypeSpecs()).hasSize(1);
    assertThat(dataHolder.getTypeSpecs().get(0).toString())
        .isEqualTo(
            "public static final class Builder extends com.facebook.litho.Component.Builder<Builder> {\n"
                + "  TestKotlinVarArg mTestKotlinVarArg;\n"
                + "\n"
                + "  com.facebook.litho.ComponentContext mContext;\n"
                + "\n"
                + "  private final java.lang.String[] REQUIRED_PROPS_NAMES = new String[] {\"numbers\"};\n"
                + "\n"
                + "  private final int REQUIRED_PROPS_COUNT = 1;\n"
                + "\n"
                + "  private final java.util.BitSet mRequired = new java.util.BitSet(REQUIRED_PROPS_COUNT);\n"
                + "\n"
                + "  private void init(com.facebook.litho.ComponentContext context, int defStyleAttr, int defStyleRes,\n"
                + "      TestKotlinVarArg testKotlinVarArgRef) {\n"
                + "    super.init(context, defStyleAttr, defStyleRes, testKotlinVarArgRef);\n"
                + "    mTestKotlinVarArg = testKotlinVarArgRef;\n"
                + "    mContext = context;\n"
                + "    mRequired.clear();\n"
                + "  }\n"
                + "\n"
                + "  public Builder number(java.lang.Number number) {\n"
                + "    if (number == null) {\n"
                + "      return this;\n"
                + "    }\n"
                + "    if (this.mTestKotlinVarArg.numbers == null) {\n"
                + "      this.mTestKotlinVarArg.numbers = new java.util.ArrayList<java.lang.Number>();\n"
                + "    }\n"
                + "    this.mTestKotlinVarArg.numbers.add(number);\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  public Builder numbers(java.util.List<java.lang.Number> numbers) {\n"
                + "    if (numbers == null) {\n"
                + "      return this;\n"
                + "    }\n"
                + "    if (this.mTestKotlinVarArg.numbers == null || this.mTestKotlinVarArg.numbers.isEmpty()) {\n"
                + "      this.mTestKotlinVarArg.numbers = numbers;\n"
                + "    } else {\n"
                + "      this.mTestKotlinVarArg.numbers.addAll(numbers);\n"
                + "    }\n"
                + "    mRequired.set(0);\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public Builder getThis() {\n"
                + "    return this;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public com.facebook.litho.specmodels.generator.BuilderGeneratorTest.TestKotlinVarArg build() {\n"
                + "    checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);\n"
                + "    return mTestKotlinVarArg;\n"
                + "  }\n"
                + "}\n");
  }
}
