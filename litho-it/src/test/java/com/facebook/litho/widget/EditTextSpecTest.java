/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests {@link EditText} component.
 */

@RunWith(ComponentsTestRunner.class)
public class EditTextSpecTest {
  @Rule
  public ComponentsRule mComponentsRule = new ComponentsRule();

  private static final String TEXT = "Hello Components";

  @Test
  public void testEditTextWithText() {
    final ComponentContext c = mComponentsRule.getContext();
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        EditText.create(c)
            .textChangedEventHandler(null)
            .textSizePx(10)
            .text(TEXT));

    final android.widget.EditText editText = (android.widget.EditText) lithoView.getChildAt(0);
    assertThat(editText.getText().toString()).isEqualTo(TEXT);
    assertThat(editText.getTextSize()).isEqualTo(10);
  }
}
