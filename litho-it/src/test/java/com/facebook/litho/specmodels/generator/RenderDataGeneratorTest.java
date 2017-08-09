/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DiffModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.google.testing.compile.CompilationRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Tests {@link RenderDataGenerator} */
public class RenderDataGeneratorTest {

  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private SpecModel mSpecModelWithDiff;
  private SpecModel mSpecModelWithoutDiff;
  private DiffModel mDiffModelNeedsRenderData;
  private DiffModel mDiffModelDoesNotNeedRenderData;

  @Before
  public void setUp() {
    mSpecModelWithDiff = mock(SpecModel.class);
    mSpecModelWithoutDiff = mock(SpecModel.class);
    mDiffModelNeedsRenderData = mock(DiffModel.class);
    mDiffModelDoesNotNeedRenderData = mock(DiffModel.class);

    when(mSpecModelWithDiff.getDiffs()).thenReturn(ImmutableList.of(mDiffModelNeedsRenderData));
    when(mSpecModelWithDiff.getComponentName()).thenReturn("WithDiffSpec");

    when(mSpecModelWithoutDiff.getDiffs()).thenReturn(ImmutableList.<DiffModel>of());
    when(mSpecModelWithoutDiff.getComponentName()).thenReturn("WithoutDiffSpec");

    when(mDiffModelNeedsRenderData.getName()).thenReturn("diffParam1");
    when(mDiffModelNeedsRenderData.needsRenderDataInfra()).thenReturn(true);

    when(mDiffModelDoesNotNeedRenderData.getName()).thenReturn("diffParam2");
    when(mDiffModelDoesNotNeedRenderData.needsRenderDataInfra()).thenReturn(false);
  }

  @Test
  public void testGenerateWithDiff() {
    TypeSpecDataHolder dataHolder = RenderDataGenerator.generate(mSpecModelWithDiff);

    assertThat(dataHolder.getMethodSpecs()).isNotEmpty();
  }

  @Test
  public void testDoNotGenerateWithoutDiff() {
    TypeSpecDataHolder dataHolder = RenderDataGenerator.generate(mSpecModelWithoutDiff);

    assertThat(dataHolder.getMethodSpecs()).isEmpty();
  }

  @Test
  public void testDoNotGenerateWithDiffThatDoesntNeedIt() {
    when(mSpecModelWithoutDiff.getDiffs())
        .thenReturn(ImmutableList.of(mDiffModelDoesNotNeedRenderData));

    TypeSpecDataHolder dataHolder = RenderDataGenerator.generate(mSpecModelWithoutDiff);

    assertThat(dataHolder.getMethodSpecs()).isEmpty();
  }
}
