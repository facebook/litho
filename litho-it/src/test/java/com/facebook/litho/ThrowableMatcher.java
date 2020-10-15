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

package com.facebook.litho;

import androidx.annotation.Nullable;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class ThrowableMatcher extends BaseMatcher<Throwable> {

  private final Class<? extends Throwable> mClassMatcher;
  private final @Nullable String mMessageMatcher;

  public static ThrowableMatcher forClass(Class<? extends Throwable> classMatcher) {
    return new ThrowableMatcher(classMatcher, null);
  }

  public static ThrowableMatcher forClassWithMessage(
      Class<? extends Throwable> classMatcher, String messageSubstring) {
    return new ThrowableMatcher(classMatcher, messageSubstring);
  }

  private ThrowableMatcher(Class<? extends Throwable> classMatcher, String messageMatcher) {
    mClassMatcher = classMatcher;
    mMessageMatcher = messageMatcher;
  }

  @Override
  public boolean matches(Object item) {
    return mClassMatcher.isInstance(item)
        && (mMessageMatcher == null || ((Throwable) item).getMessage().contains(mMessageMatcher));
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("an instance of " + mClassMatcher);
    if (mMessageMatcher != null) {
      description.appendText(" with message containing \"" + mMessageMatcher + "\"");
    }
  }
}
