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

package com.facebook.litho.testing;

import android.app.Activity;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.testing.assertj.LithoRepresentation;
import javax.annotation.Nullable;
import org.assertj.core.api.Assertions;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.robolectric.Robolectric;

public class ComponentsRule implements TestRule {

  private final @Nullable Integer mThemeResId;
  private ComponentContext mContext;

  public ComponentsRule() {
    this(null);
  }

  public ComponentsRule(@Nullable Integer themeResId) {
    mThemeResId = themeResId;
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        final Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        if (mThemeResId != null) {
          activity.setTheme(mThemeResId);
        }
        mContext = new ComponentContext(activity);

        Assertions.useRepresentation(new LithoRepresentation(mContext));

        try {
          base.evaluate();
        } finally {
          Assertions.useDefaultRepresentation();
        }
      }
    };
  }

  /** Get a Component Context for this test instance. */
  public ComponentContext getContext() {
    return mContext;
  }
}
