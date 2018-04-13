/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.litho.widget;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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
