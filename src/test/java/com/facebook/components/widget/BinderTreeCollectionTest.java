/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.v4.util.SparseArrayCompat;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestLayoutComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class BinderTreeCollectionTest {

  private BinderTreeCollection mBinderTreeCollection;

  @Before
  public void setup() {
    mBinderTreeCollection = new BinderTreeCollection();

    for (int i = 0; i < 10; i++) {
      mBinderTreeCollection.put(i + 1, createNewComponentTree());
    }
  }

  @Test
  public void testPositionOf() {
    ComponentTree treeAtPos3 = mBinderTreeCollection.get(3);

