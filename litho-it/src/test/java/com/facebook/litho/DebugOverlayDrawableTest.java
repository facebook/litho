/*
 * Copyright 2018-present Facebook, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class DebugOverlayDrawableTest {

  @Test
  public void testEqualList() {
    List<Boolean> testList = new ArrayList<>();
    testList.add(false);
    testList.add(false);
    testList.add(false);
    DebugOverlayDrawable testD = new DebugOverlayDrawable(testList);

    List<Boolean> equalList = new ArrayList<>(testList);
    DebugOverlayDrawable equalD = new DebugOverlayDrawable(equalList);

    assertEquals(testD.hashCode(), equalD.hashCode());
    assertTrue(testD.isEquivalentTo(equalD));
  }

  @Test
  public void testNotEqualList() {
    List<Boolean> testList = new ArrayList<>();
    testList.add(true);
    testList.add(false);
    testList.add(true);
    DebugOverlayDrawable testD = new DebugOverlayDrawable(testList);

    List<Boolean> notEqualList = new ArrayList<>();
    notEqualList.add(true);
    notEqualList.add(true);
    notEqualList.add(false);
    DebugOverlayDrawable notEqualD = new DebugOverlayDrawable(notEqualList);

    assertNotEquals(testD.hashCode(), notEqualD.hashCode());
    assertFalse(testD.isEquivalentTo(notEqualD));
  }

  @Test
  public void testEmptyList() {
    List<Boolean> testList = new ArrayList<>();
    testList.add(true);
    testList.add(true);
    testList.add(true);
    DebugOverlayDrawable testD = new DebugOverlayDrawable(testList);

    DebugOverlayDrawable emptyD = new DebugOverlayDrawable(new ArrayList<>());

    assertNotEquals(testD.hashCode(), emptyD.hashCode());
    assertFalse(testD.isEquivalentTo(emptyD));
  }

  @Test
  public void testBigList() {
    List<Boolean> bigList = new ArrayList<>();
    for (int i = 0; i < 100_000; i++) {
      bigList.add(true);
    }
    DebugOverlayDrawable testD = new DebugOverlayDrawable(bigList);

    DebugOverlayDrawable bigD = new DebugOverlayDrawable(new ArrayList<>(bigList));

    assertEquals(testD.hashCode(), bigD.hashCode());
    assertTrue(testD.isEquivalentTo(bigD));
  }

  @Test
  public void testSameParams() {
    List<Boolean> testList = new ArrayList<>();
    testList.add(true);
    testList.add(false);
    testList.add(true);
    testList.add(true);
    DebugOverlayDrawable testD = new DebugOverlayDrawable(testList);

    DebugOverlayDrawable sameD = new DebugOverlayDrawable(testList);

    assertEquals(testD.text, sameD.text);
    assertEquals(testD.overlayColor, sameD.overlayColor);
  }
}
