/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DiffModel;
import com.facebook.litho.specmodels.model.SpecModel;

import com.google.testing.compile.CompilationRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link RenderInfoGenerator}
 */
public class RenderInfoGeneratorTest {

  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private SpecModel mSpecModelWithDiff;
  private SpecModel mSpecModelWithoutDiff;
  private DiffModel mDiffModelNeedsRenderInfo;
  private DiffModel mDiffModelDoesNotNeedRenderInfo;

  @Before
  public void setUp() {
    mSpecModelWithDiff = mock(SpecModel.class);
    mSpecModelWithoutDiff = mock(SpecModel.class);
    mDiffModelNeedsRenderInfo = mock(DiffModel.class);
    mDiffModelDoesNotNeedRenderInfo = mock(DiffModel.class);

    when(mSpecModelWithDiff.getDiffs()).thenReturn(ImmutableList.of(mDiffModelNeedsRenderInfo));
    when(mSpecModelWithDiff.getComponentName()).thenReturn("WithDiffSpec");

    when(mSpecModelWithoutDiff.getDiffs()).thenReturn(ImmutableList.<DiffModel>of());
    when(mSpecModelWithoutDiff.getComponentName()).thenReturn("WithoutDiffSpec");

    when(mDiffModelNeedsRenderInfo.getName()).thenReturn("diffParam1");
    when(mDiffModelNeedsRenderInfo.needsRenderInfoInfra()).thenReturn(true);

    when(mDiffModelDoesNotNeedRenderInfo.getName()).thenReturn("diffParam2");
    when(mDiffModelDoesNotNeedRenderInfo.needsRenderInfoInfra()).thenReturn(false);
  }

  @Test
  public void testGenerateWithDiff() {
    TypeSpecDataHolder dataHolder = RenderInfoGenerator.generate(mSpecModelWithDiff);

    assertThat(dataHolder.getMethodSpecs()).isNotEmpty();
  }

  @Test
  public void testDoNotGenerateWithoutDiff() {
    TypeSpecDataHolder dataHolder = RenderInfoGenerator.generate(mSpecModelWithoutDiff);

    assertThat(dataHolder.getMethodSpecs()).isEmpty();
  }

  @Test
  public void testDoNotGenerateWithDiffThatDoesntNeedIt() {
    when(mSpecModelWithoutDiff.getDiffs())
        .thenReturn(ImmutableList.of(mDiffModelDoesNotNeedRenderInfo));

    TypeSpecDataHolder dataHolder = RenderInfoGenerator.generate(mSpecModelWithoutDiff);

    assertThat(dataHolder.getMethodSpecs()).isEmpty();
  }
}
