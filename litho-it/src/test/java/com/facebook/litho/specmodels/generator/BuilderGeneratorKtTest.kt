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

package com.facebook.litho.specmodels.generator

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.specmodels.internal.RunMode
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory
import com.google.testing.compile.CompilationRule
import javax.annotation.processing.Messager
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

@LayoutSpec
object VarArgsWildcardPropTestSpec {
  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop(varArg = "color") colors: List<Color>): Component {
    return Column.create(c).build()
  }
}

@LayoutSpec
object WildcardOutPropTestSpec {
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop numWildCard: List<out @JvmWildcard Number>
  ): Component? {
    return Column.create(c).build()
  }
}

@RunWith(JUnit4::class)
class BuilderGeneratorKtTest {

  @Rule @JvmField val compilationRule = CompilationRule()

  private val layoutSpecModelFactory = LayoutSpecModelFactory()
  private val elements: Elements
    get() = compilationRule.elements
  private val types: Types
    get() = compilationRule.types

  private val messager = mock(Messager::class.java)

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
  }

  @Test
  fun specWithWildcardOutProp_generate() {
    val typeElement = elements.getTypeElement(WildcardOutPropTestSpec::class.java.canonicalName)

    val specModel =
        layoutSpecModelFactory.create(
            elements, types, typeElement, messager, RunMode.normal(), null, null)
    val dataHolder = BuilderGenerator.generate(specModel)
    assertThat(dataHolder.typeSpecs.size).isEqualTo(1)
    assertThat(dataHolder.typeSpecs.get(0).toString())
        .isEqualTo(
            """
                    @com.facebook.litho.annotations.Generated
                    public static final class Builder extends com.facebook.litho.Component.Builder<Builder> {
                      WildcardOutPropTest mWildcardOutPropTest;

                      com.facebook.litho.ComponentContext mContext;

                      private final java.lang.String[] REQUIRED_PROPS_NAMES = new String[] {"numWildCard"};

                      private final int REQUIRED_PROPS_COUNT = 1;

                      private final java.util.BitSet mRequired = new java.util.BitSet(REQUIRED_PROPS_COUNT);

                      private void init(com.facebook.litho.ComponentContext context, int defStyleAttr, int defStyleRes,
                          WildcardOutPropTest wildcardOutPropTestRef) {
                        super.init(context, defStyleAttr, defStyleRes, wildcardOutPropTestRef);
                        mWildcardOutPropTest = wildcardOutPropTestRef;
                        mContext = context;
                        mRequired.clear();
                      }

                      @java.lang.Override
                      protected void setComponent(com.facebook.litho.Component component) {
                        mWildcardOutPropTest = (com.facebook.litho.specmodels.generator.WildcardOutPropTest) component;
                      }

                      @com.facebook.litho.annotations.PropSetter(
                          value = "numWildCard",
                          required = true
                      )
                      @com.facebook.litho.annotations.RequiredProp("numWildCard")
                      public Builder numWildCard(@org.jetbrains.annotations.NotNull java.util.List<? extends java.lang.Number> numWildCard) {
                        this.mWildcardOutPropTest.numWildCard = numWildCard;
                        mRequired.set(0);
                        return this;
                      }

                      @java.lang.Override
                      public Builder getThis() {
                        return this;
                      }

                      @java.lang.Override
                      public com.facebook.litho.specmodels.generator.WildcardOutPropTest build() {
                        checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
                        return mWildcardOutPropTest;
                      }
                    }
                    
                    """.trimIndent())
  }

  @Test
  fun specWithVarArgWildcardProp_generate() {
    val typeElement: TypeElement =
        elements.getTypeElement(VarArgsWildcardPropTestSpec::class.java.canonicalName)

    val specModel =
        layoutSpecModelFactory.create(
            elements, types, typeElement, messager, RunMode.normal(), null, null)
    val dataHolder = BuilderGenerator.generate(specModel)
    assertThat(dataHolder.typeSpecs.size).isEqualTo(1)
    assertThat(dataHolder.typeSpecs.get(0).toString())
        .isEqualTo(
            """
                    @com.facebook.litho.annotations.Generated
                    public static final class Builder extends com.facebook.litho.Component.Builder<Builder> {
                      VarArgsWildcardPropTest mVarArgsWildcardPropTest;

                      com.facebook.litho.ComponentContext mContext;

                      private void init(com.facebook.litho.ComponentContext context, int defStyleAttr, int defStyleRes,
                          VarArgsWildcardPropTest varArgsWildcardPropTestRef) {
                        super.init(context, defStyleAttr, defStyleRes, varArgsWildcardPropTestRef);
                        mVarArgsWildcardPropTest = varArgsWildcardPropTestRef;
                        mContext = context;
                      }

                      @java.lang.Override
                      protected void setComponent(com.facebook.litho.Component component) {
                        mVarArgsWildcardPropTest = (com.facebook.litho.specmodels.generator.VarArgsWildcardPropTest) component;
                      }

                      @com.facebook.litho.annotations.PropSetter(
                          value = "colors",
                          required = false
                      )
                      public Builder color(@org.jetbrains.annotations.NotNull android.graphics.Color color) {
                        if (color == null) {
                          return this;
                        }
                        if (this.mVarArgsWildcardPropTest.colors == java.util.Collections.EMPTY_LIST) {
                          this.mVarArgsWildcardPropTest.colors = new java.util.ArrayList<android.graphics.Color>();
                        }
                        this.mVarArgsWildcardPropTest.colors.add(color);
                        return this;
                      }

                      @com.facebook.litho.annotations.PropSetter(
                          value = "colors",
                          required = false
                      )
                      public Builder colors(@org.jetbrains.annotations.NotNull java.util.List<android.graphics.Color> colors) {
                        if (colors == null) {
                          return this;
                        }
                        if (this.mVarArgsWildcardPropTest.colors.isEmpty()) {
                          this.mVarArgsWildcardPropTest.colors = colors;
                        } else {
                          this.mVarArgsWildcardPropTest.colors.addAll(colors);
                        }
                        return this;
                      }

                      @java.lang.Override
                      public Builder getThis() {
                        return this;
                      }

                      @java.lang.Override
                      public com.facebook.litho.specmodels.generator.VarArgsWildcardPropTest build() {
                        return mVarArgsWildcardPropTest;
                      }
                    }
                    
                """.trimIndent())
  }
}
