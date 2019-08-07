/*
 * Copyright 2019-present Facebook, Inc.
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
package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaEdge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;

@RunWith(ComponentsTestRunner.class)
public class EdgesTest {

  private Edges mEdges;

  @Before
  public void setup() {
    mEdges = new Edges();
  }

  @Test
  public void testInsertingOneEdgeMultipleTimes() {
    mEdges.set(YogaEdge.TOP, 1);
    mEdges.set(YogaEdge.TOP, 2);
    mEdges.set(YogaEdge.TOP, 3);
    mEdges.set(YogaEdge.TOP, 4);
    mEdges.set(YogaEdge.TOP, 5);

    long bits = ~0;
    bits &= ~((long) (0xF) << (YogaEdge.TOP.intValue() * 4));
    bits |= ((long) 0 << (YogaEdge.TOP.intValue() * 4));
    assertThat(getEdgesToValuesIndex()).isEqualTo(bits);

    assertThat(getValuesArray().length).isEqualTo(2);
    assertThat(getValuesArray()[0]).isEqualTo(5);
    assertThat(YogaConstants.isUndefined(getValuesArray()[1])).isTrue();
  }

  @Test
  public void testUnsettingAnEdge() {
    mEdges.set(YogaEdge.TOP, 1);
    mEdges.set(YogaEdge.TOP, 2);
    mEdges.set(YogaEdge.TOP, YogaConstants.UNDEFINED);

    long bits = ~0;
    assertThat(getEdgesToValuesIndex()).isEqualTo(bits);

    assertThat(getValuesArray().length).isEqualTo(2);
    assertThat(YogaConstants.isUndefined(getValuesArray()[0])).isTrue();
    assertThat(YogaConstants.isUndefined(getValuesArray()[1])).isTrue();
  }

  @Test
  public void testUnsettingNotTheFirstEdge() {
    mEdges.set(YogaEdge.TOP, 1);
    mEdges.set(YogaEdge.LEFT, 2);
    mEdges.set(YogaEdge.LEFT, YogaConstants.UNDEFINED);

    long bits = ~0;
    bits &= ~((long) (0xF) << (YogaEdge.TOP.intValue() * 4));
    bits |= ((long) 0 << (YogaEdge.TOP.intValue() * 4));
    assertThat(getEdgesToValuesIndex()).isEqualTo(bits);

    assertThat(getValuesArray().length).isEqualTo(2);
    assertThat(getValuesArray()[0]).isEqualTo(1);
    assertThat(getValuesArray()[1]).isNaN();
  }

  @Test
  public void testSettingMultipleEdgesIncreasesTheArray() {
    mEdges.set(YogaEdge.TOP, 1);
    mEdges.set(YogaEdge.LEFT, 2);
    mEdges.set(YogaEdge.ALL, 5);

    long bits = ~0;
    bits &= ~((long) (0xF) << (YogaEdge.TOP.intValue() * 4));
    bits &= ~((long) (0xF) << (YogaEdge.LEFT.intValue() * 4));
    bits &= ~((long) (0xF) << (YogaEdge.ALL.intValue() * 4));
    bits |= ((long) 0 << (YogaEdge.TOP.intValue() * 4));
    bits |= ((long) 1 << (YogaEdge.LEFT.intValue() * 4));
    bits |= ((long) 2 << (YogaEdge.ALL.intValue() * 4));
    assertThat(getEdgesToValuesIndex()).isEqualTo(bits);

    assertThat(getValuesArray().length).isEqualTo(4);
    assertThat(getValuesArray()[0]).isEqualTo(1);
    assertThat(getValuesArray()[1]).isEqualTo(2);
    assertThat(getValuesArray()[2]).isEqualTo(5);
    assertThat(getValuesArray()[3]).isNaN();
  }

  @Test
  public void testUnsettingAndSettingNewEdgesReusesArraySpace() {
    mEdges.set(YogaEdge.TOP, 1);
    mEdges.set(YogaEdge.LEFT, 2);
    mEdges.set(YogaEdge.ALL, 5);
    mEdges.set(YogaEdge.LEFT, YogaConstants.UNDEFINED);
    mEdges.set(YogaEdge.BOTTOM, 4);

    long bits = ~0;
    bits &= ~((long) (0xF) << (YogaEdge.TOP.intValue() * 4));
    bits &= ~((long) (0xF) << (YogaEdge.ALL.intValue() * 4));
    bits &= ~((long) (0xF) << (YogaEdge.BOTTOM.intValue() * 4));
    bits |= ((long) 0 << (YogaEdge.TOP.intValue() * 4));
    bits |= ((long) 1 << (YogaEdge.BOTTOM.intValue() * 4));
    bits |= ((long) 2 << (YogaEdge.ALL.intValue() * 4));
    assertThat(getEdgesToValuesIndex()).isEqualTo(bits);

    assertThat(getValuesArray().length).isEqualTo(4);
    assertThat(getValuesArray()[0]).isEqualTo(1);
    assertThat(getValuesArray()[1]).isEqualTo(4);
    assertThat(getValuesArray()[2]).isEqualTo(5);
    assertThat(getValuesArray()[3]).isNaN();
  }

  @Test
  public void testAliasesAndResolveGetter() {
    mEdges.set(YogaEdge.ALL, 10);

    assertThat(mEdges.getRaw(YogaEdge.LEFT)).isNaN();
    assertThat(mEdges.getRaw(YogaEdge.TOP)).isNaN();
    assertThat(mEdges.getRaw(YogaEdge.RIGHT)).isNaN();
    assertThat(mEdges.getRaw(YogaEdge.BOTTOM)).isNaN();
    assertThat(mEdges.getRaw(YogaEdge.ALL)).isEqualTo(10);

    assertThat(mEdges.get(YogaEdge.LEFT)).isEqualTo(10);
    assertThat(mEdges.get(YogaEdge.TOP)).isEqualTo(10);
    assertThat(mEdges.get(YogaEdge.RIGHT)).isEqualTo(10);
    assertThat(mEdges.get(YogaEdge.BOTTOM)).isEqualTo(10);
    assertThat(mEdges.get(YogaEdge.ALL)).isEqualTo(10);
  }

  private long getEdgesToValuesIndex() {
    return (long) Whitebox.getInternalState(mEdges, "mEdgesToValuesIndex");
  }

  private float[] getValuesArray() {
    return (float[]) Whitebox.getInternalState(mEdges, "mValues");
  }
}
