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

package com.facebook.litho.testing;

import android.content.Context;
import android.view.View;
import com.facebook.litho.ComponentContext;

public class TestComponentContextWithView extends ComponentContext {

  private final View mTestView;

  public TestComponentContextWithView(Context c) {
    super(c);
    if (c instanceof TestComponentContextWithView) {
      mTestView = ((TestComponentContextWithView) c).getTestView();
    } else {
      mTestView = new View(c);
    }
  }

  public TestComponentContextWithView(Context context, View view) {
    super(context);
    mTestView = view;
  }

  public View getTestView() {
    return mTestView;
  }
}
