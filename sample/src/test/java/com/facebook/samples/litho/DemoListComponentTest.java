/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.samples.litho;

import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;

import android.support.v7.widget.OrientationHelper;
import com.facebook.litho.Component;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.SubComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class DemoListComponentTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();
  private Component<DemoListComponent> mComponent;

  @Before
  public void setUp() {
    final RecyclerBinder recyclerBinder = new RecyclerBinder.Builder()
        .layoutInfo(new LinearLayoutInfo(mComponentsRule.getContext(), OrientationHelper.VERTICAL, false))
        .build(mComponentsRule.getContext());

    mComponent = DemoListComponent.create(mComponentsRule.getContext())
        .recyclerBinder(recyclerBinder)
        .build();
  }

  @Test
  public void testSubComponents() {
    assertThat(mComponentsRule.getContext(), mComponent)
        .containsOnlySubComponents(SubComponent.of(Recycler.class));
  }
}
