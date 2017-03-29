/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentView;
import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.ComponentsRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

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
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        EditText.create(c)
            .textChangedEventHandler(null)
            .textSizePx(10)
            .text(TEXT));

    final android.widget.EditText editText = (android.widget.EditText) componentView.getChildAt(0);
    assertThat(editText.getText().toString()).isEqualTo(TEXT);
