// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import android.view.View;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentView;
import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ProgressSpec}
 */

@RunWith(ComponentsTestRunner.class)
public class ProgressSpecTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testDefault() {
    ComponentView view = getMountedView();
    assertThat(view.getMeasuredWidth()).isGreaterThan(0);
    assertThat(view.getMeasuredHeight()).isGreaterThan(0);
  }

  /**
   * Ignored because the first view.measure() fails to trigger onMeasure in ProgressSpec.
   * CSSLayout bug?
   */
  @Test
  @Ignore
  public void testUnsetSize() {
    ComponentView view = getMountedView();

    view.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

    assertThat(view.getMeasuredWidth()).isEqualTo(ProgressSpec.DEFAULT_SIZE);
    assertThat(view.getMeasuredHeight()).isEqualTo(ProgressSpec.DEFAULT_SIZE);
  }

  private ComponentView getMountedView() {
    Progress.Builder progress = Progress.create(mContext);

    return ComponentTestHelper.mountComponent(
        progress);
  }
}
