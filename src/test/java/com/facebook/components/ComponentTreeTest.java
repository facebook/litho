/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.os.Looper;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestLayoutComponent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import static com.facebook.components.SizeSpec.AT_MOST;
import static com.facebook.components.SizeSpec.EXACTLY;
import static com.facebook.components.SizeSpec.makeSizeSpec;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeTest {

  private int mWidthSpec;
  private int mWidthSpec2;
  private int mHeightSpec;
  private int mHeightSpec2;

  private Component mComponent;
  private ShadowLooper mLayoutThreadShadowLooper;
  private ComponentContext mContext;

  private static class TestComponent<L extends ComponentLifecycle> extends Component<L> {
    public TestComponent(L component) {
      super(component);
    }

    @Override
    public String getSimpleName() {
      return "TestComponent";
    }
  }

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mComponent = TestDrawableComponent.create(mContext)
        .build();

    mLayoutThreadShadowLooper = Shadows.shadowOf(
        (Looper) Whitebox.invokeMethod(
            ComponentTree.class,
            "getDefaultLayoutThreadLooper"));

    mWidthSpec = makeSizeSpec(39, EXACTLY);
    mWidthSpec2 = makeSizeSpec(40, EXACTLY);
    mHeightSpec = makeSizeSpec(41, EXACTLY);
    mHeightSpec2 = makeSizeSpec(42, EXACTLY);
  }

  private void creationCommonChecks(ComponentTree componentTree) {
    // Not view or attached yet
    Assert.assertNull(getComponentView(componentTree));
    Assert.assertFalse(isAttached(componentTree));

    // No measure spec from view yet.
    Assert.assertFalse(
        (Boolean) Whitebox.getInternalState(componentTree, "mHasViewMeasureSpec"));

    // The component input should be the one we passed in
    Assert.assertSame(
        mComponent,
        Whitebox.getInternalState(componentTree, "mRoot"));
  }

  private void postSizeSpecChecks(
      ComponentTree componentTree,
      String layoutStateVariableName) {
    postSizeSpecChecks(
        componentTree,
        layoutStateVariableName,
