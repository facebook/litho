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

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests {@link TextInput} component. */
@RunWith(ComponentsTestRunner.class)
public class TextInputSpecTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testTextInputWithText() {
    String text = "Dummy text";
    int textSize = 10;
    Component.Builder component = TextInput.create(mContext).textSizePx(textSize).initialText(text);
    final android.widget.EditText editText = getEditText(component);

    assertThat(editText.getText().toString()).isEqualTo(text);
    assertThat(editText.getTextSize()).isEqualTo(textSize);
  }

  @Test
  public void testTextInputMultiline() {
    String multiline = "a\nb\nc";

    Component.Builder component = TextInput.create(mContext).initialText(multiline);
    android.widget.EditText editText = getEditText(component);
    assertThat(editText.getLineCount()).isEqualTo(1);

    component = TextInput.create(mContext).initialText(multiline).multiline(true);
    editText = getEditText(component);
    assertThat(editText.getLineCount()).isEqualTo(3);
  }

  private static android.widget.EditText getEditText(Component.Builder component) {
    final LithoView lithoView = ComponentTestHelper.mountComponent(component);
    return (android.widget.EditText) lithoView.getChildAt(0);
  }
}
