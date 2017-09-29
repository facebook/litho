/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor;

import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;

import android.app.Activity;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.testing.sections.TestTarget;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.treeprop.TreePropNumberType;
import com.facebook.litho.testing.treeprop.TreePropSectionTestLeafGroupSpec.Result;
import com.facebook.litho.testing.treeprop.TreePropSectionTestParentGroup;
import com.facebook.litho.testing.treeprop.TreePropStringType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

/** Tests passing {@link TreeProp}s down a Section tree. */
@RunWith(ComponentsTestRunner.class)
public class TreePropSectionTest {

  private SectionContext mContext;

  @Before
  public void setUp() {
    final Activity activity = Robolectric.buildActivity(Activity.class).create().get();
    mContext = new SectionContext(activity);
  }

  /**
   * Tests that a TreeProp is propagated down a Section Tree,
   * is scoped correctly, and can be overwritten.
   */
  @Test
  public void testTreePropsPropagated() {
    final Result propALeaf1 = new Result();
    final Result propBLeaf1 = new Result();
    final Result propBLeaf2 = new Result();
    final TreePropNumberType treePropA = new TreePropNumberType(9);
    final TreePropStringType treePropB = new TreePropStringType("propB");
    final TreePropStringType treePropBChanged = new TreePropStringType("propB_changed");

    final SectionTree tree = SectionTree.create(mContext, new TestTarget()).build();
    tree.setRoot(TreePropSectionTestParentGroup.create(mContext)
        .propA(treePropA)
        .propB(treePropB)
        .resultPropALeaf1(propALeaf1)
        .resultPropBLeaf1(propBLeaf1)
        .resultPropBLeaf2(propBLeaf2)
        .build());

    assertThat(propALeaf1.mProp).isEqualTo(treePropA);
    // TreePropSectionTestMiddleGroupSpec modifies "propB".
    assertThat(propBLeaf1.mProp).isEqualTo(treePropBChanged);

    // The second LeafGroupSpec does not see the modification to "propB"
    // because its not a descendant of MiddleGroupSpec.
    assertThat(propBLeaf2.mProp).isEqualTo(treePropB);
  }
}
