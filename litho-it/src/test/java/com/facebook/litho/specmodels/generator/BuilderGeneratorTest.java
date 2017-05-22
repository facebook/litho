/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.specmodels.generator;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;

import com.google.testing.compile.CompilationRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests {@link BuilderGenerator}
 */
public class BuilderGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  static class TestSpec {
    @PropDefault protected static boolean arg0 = true;

    @OnCreateLayout
    public void testDelegateMethod(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @Prop(optional = true) boolean arg3,
        @Prop(varArg = "name") List<String> names) {
    }

    @OnEvent(Object.class)
    public void testEventMethod(@Prop boolean arg0) {}

    @OnUpdateState
    public void testUpdateStateMethod() {}
  }

  private SpecModel mSpecModel;

  @Before
  public void setUp() {
    Elements elements = mCompilationRule.getElements();
    TypeElement typeElement = elements.getTypeElement(TestSpec.class.getCanonicalName());
    mSpecModel = LayoutSpecModelFactory.create(elements, typeElement, null);
  }

  @Test
  public void testGenerate() {
    TypeSpecDataHolder dataHolder = BuilderGenerator.generate(mSpecModel);

    assertThat(dataHolder.getMethodSpecs()).hasSize(2);
    assertThat(dataHolder.getMethodSpecs().get(0).toString()).isEqualTo(
        "public static Builder create(com.facebook.litho.ComponentContext context) {\n" +
        "  return create(context, 0, 0);\n" +
        "}\n");
    assertThat(dataHolder.getMethodSpecs().get(1).toString()).isEqualTo(
        "public static Builder create(com.facebook.litho.ComponentContext context, int defStyleAttr,\n" +
            "    int defStyleRes) {\n" +
            "  Builder builder = sBuilderPool.acquire();\n" +
            "  if (builder == null) {\n" +
            "    builder = new Builder();\n" +
            "  }\n" +
            "  builder.init(context, defStyleAttr, defStyleRes, new TestImpl());\n" +
            "  return builder;\n" +
            "}\n");

    assertThat(dataHolder.getFieldSpecs()).hasSize(1);
    assertThat(dataHolder.getFieldSpecs().get(0).toString()).isEqualTo(
        "private static final android.support.v4.util.Pools.SynchronizedPool<Builder> sBuilderPool = new android.support.v4.util.Pools.SynchronizedPool<Builder>(2);\n");

    assertThat(dataHolder.getTypeSpecs()).hasSize(1);
    assertThat(dataHolder.getTypeSpecs().get(0).toString()).isEqualTo(
        "public static class Builder extends com.facebook.litho.Component.Builder<com.facebook.litho.specmodels.generator.BuilderGeneratorTest.Test> {\n" +
        "  private static final java.lang.String[] REQUIRED_PROPS_NAMES = new String[] {\"arg0\", \"arg4\"};\n" +
        "\n" +
        "  private static final int REQUIRED_PROPS_COUNT = 2;\n" +
        "\n" +
        "  TestImpl mTestImpl;\n" +
        "\n" +
        "  com.facebook.litho.ComponentContext mContext;\n" +
        "\n" +
        "  private java.util.BitSet mRequired = new java.util.BitSet(REQUIRED_PROPS_COUNT);\n" +
        "\n" +
        "  private void init(com.facebook.litho.ComponentContext context, int defStyleAttr, int defStyleRes,\n" +
        "      TestImpl testImpl) {\n" +
        "    super.init(context, defStyleAttr, defStyleRes, testImpl);\n" +
        "    mTestImpl = testImpl;\n" +
        "    mContext = context;\n" +
        "    mRequired.clear();\n" +
        "  }\n" +
        "\n" +
        "  public Builder arg0(boolean arg0) {\n" +
        "    this.mTestImpl.arg0 = arg0;\n" +
        "    mRequired.set(0);\n" +
        "    return this;\n" +
        "  }\n" +
        "\n" +
        "  public Builder arg3(boolean arg3) {\n" +
        "    this.mTestImpl.arg3 = arg3;\n" +
        "    return this;\n" +
        "  }\n" +
        "\n" +
        "  public Builder name(java.lang.String name) {\n" +
        "    if (this.mTestImpl.arg4 == null) {\n" +
        "      this.mTestImpl.arg4 = new java.util.ArrayList<java.lang.String>();\n" +
        "    }\n" +
        "    this.mTestImpl.arg4.add(name);\n" +
        "    mRequired.set(1);\n" +
        "    return this;\n" +
        "  }\n" +
        "\n" +
        "  public Builder key(java.lang.String key) {\n" +
        "    super.setKey(key);\n" +
        "    return this;\n" +
        "  }\n" +
        "\n" +
        "  @java.lang.Override\n" +
        "  public com.facebook.litho.Component<com.facebook.litho.specmodels.generator.BuilderGeneratorTest.Test> build() {\n" +
        "    checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);\n" +
        "    TestImpl testImpl = mTestImpl;\n" +
        "    release();\n" +
        "    return testImpl;\n" +
        "  }\n" +
        "\n" +
        "  @java.lang.Override\n" +
        "  protected void release() {\n" +
        "    super.release();\n" +
        "    mTestImpl = null;\n" +
        "    mContext = null;\n" +
        "    sBuilderPool.release(this);\n" +
        "  }\n" +
        "}\n");
  }
}
