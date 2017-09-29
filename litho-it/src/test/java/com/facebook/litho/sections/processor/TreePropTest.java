/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.app.Activity;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.treeprop.TreePropNumberType;
import com.facebook.litho.testing.treeprop.TreePropStringType;
import com.facebook.litho.testing.treeprop.TreePropTestParent;
import com.facebook.litho.testing.treeprop.TreePropTestResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

/** Tests passing {@link TreeProp}s down a Component tree. */
@RunWith(ComponentsTestRunner.class)
public class TreePropTest {

  private ComponentContext mContext;

  @Before
  public void setUp() {
    final Activity activity = Robolectric.buildActivity(Activity.class).create().get();
    mContext = new ComponentContext(activity);
  }

  /**
   * Tests that a TreeProp is propagated down a Component Tree,
   * is scoped correctly, and can be overwritten.
   */
  @Test
  public void testTreePropsPropagated() {
    final TreePropTestResult propALeaf1 = new TreePropTestResult();
    final TreePropTestResult propBLeaf1 = new TreePropTestResult();
    final TreePropTestResult probBLeaf2 = new TreePropTestResult();
    final TreePropTestResult propAMount = new TreePropTestResult();
    final TreePropNumberType treePropA = new TreePropNumberType(9);
    final TreePropStringType treePropB = new TreePropStringType("propB");
    final TreePropStringType treePropBChanged = new TreePropStringType("propB_changed");

    TreePropTestParent.create(mContext)
        .propA(treePropA)
        .propB(treePropB)
        .resultPropALeaf1(propALeaf1)
        .resultPropBLeaf1(propBLeaf1)
        .resultPropBLeaf2(probBLeaf2)
        .resultPropAMount(propAMount)
        .buildWithLayout();

    assertThat(propALeaf1.mProp).isEqualTo(treePropA);
    // TreePropTestMiddleSpec modifies "propB".
    assertThat(propBLeaf1.mProp).isEqualTo(treePropBChanged);

    // The second LeafSpec does not see the modification to "propB"
    // because its not a descendant of MiddleSpec.
    assertThat(probBLeaf2.mProp).isEqualTo(treePropB);

    assertThat(propAMount.mProp).isEqualTo(treePropA);
  }
}
