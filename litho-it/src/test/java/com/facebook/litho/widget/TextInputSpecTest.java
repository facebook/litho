/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.LinkMovementMethod;
import android.widget.EditText;
import android.widget.TextView;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.lang.reflect.Field;
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

  @Test
  public void testCursorDrawableResNotSet()
      throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
    // Obtain default style value with the reflection
    // https://stackoverflow.com/questions/8683411/the-import-com-android-internal-r-cannot-be-resolved/8683466
    Class clsStyleable = Class.forName("com.android.internal.R$styleable");
    Field fldStyleable = clsStyleable.getDeclaredField("TextView");
    fldStyleable.setAccessible(true);
    int[] textStyleArr = (int[]) fldStyleable.get(null);
    int defEditTextStyle = Resources.getSystem().getIdentifier("editTextStyle", "attr", "android");
    final Resources.Theme theme = mContext.getAndroidContext().getTheme();
    TypedArray a = theme.obtainStyledAttributes(null, textStyleArr, defEditTextStyle, 0);

    fldStyleable = clsStyleable.getDeclaredField("TextView_textCursorDrawable");
    fldStyleable.setAccessible(true);
    int textCursorStyle = (Integer) fldStyleable.get(null);

    int defaultDrawableRes = a.getResourceId(textCursorStyle, 0);

    Component.Builder component = TextInput.create(mContext);
    final android.widget.EditText editText = getEditText(component);
    Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
    f.setAccessible(true);
    Object actualDrawableRes = f.get(editText);

    assertThat(actualDrawableRes).isEqualTo(defaultDrawableRes);
  }

  @Test
  public void testCursorDrawableResSet() throws IllegalAccessException, NoSuchFieldException {
    int drawableRes = 10;
    Component.Builder component = TextInput.create(mContext).cursorDrawableRes(drawableRes);
    final android.widget.EditText editText = getEditText(component);
    Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
    f.setAccessible(true);
    f.get(editText);

    assertThat(f.get(editText)).isEqualTo(drawableRes);
  }

  @Test
  public void testNullInputBackground() {
    Component.Builder component = TextInput.create(mContext).inputBackground(null);
    final android.widget.EditText editText = getEditText(component);
    Drawable editTextBackground = editText.getBackground();
    assertThat(editTextBackground).isEqualTo(null);
  }

  @Test
  public void testDefaultInputBackground() {
    Component.Builder component = TextInput.create(mContext);
    final android.widget.EditText editText = getEditText(component);
    Drawable editTextBackground = editText.getBackground();
    assertThat(editTextBackground).isNotNull();
  }

  @Test
  public void testSetMovementMethod() {
    Component.Builder component =
        TextInput.create(mContext).movementMethod(LinkMovementMethod.getInstance());
    final android.widget.EditText editText = getEditText(component);
    assertThat(editText.getMovementMethod()).isInstanceOf(LinkMovementMethod.class);
  }

  @Test
  public void testDefaultMovementMethod() {
    Component.Builder component = TextInput.create(mContext);
    final android.widget.EditText editText = getEditText(component);
    assertThat(editText.getMovementMethod()).isInstanceOf(ArrowKeyMovementMethod.class);
    assertThat(new EditText(RuntimeEnvironment.application).getMovementMethod())
        .isInstanceOf(ArrowKeyMovementMethod.class);
  }

  @Test
  public void testErrorState() {
    String errorMessage = "Error message";
    Component.Builder component = TextInput.create(mContext).error(errorMessage);
    final android.widget.EditText editText = getEditText(component);
    assertThat(editText.getError()).isEqualTo(errorMessage);
    component = TextInput.create(mContext).error(null);
    assertThat(getEditText(component).getError()).isNullOrEmpty();
  }

  private static android.widget.EditText getEditText(Component.Builder component) {
    final LithoView lithoView = ComponentTestHelper.mountComponent(component);
    return (android.widget.EditText) lithoView.getChildAt(0);
  }
}
