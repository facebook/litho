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

package com.facebook.litho.reference;

import static com.facebook.litho.reference.Reference.acquire;
import static com.facebook.litho.reference.Reference.release;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ReferenceTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testAcquireAndRelease() {
    TestReferenceLifecycle referenceLifecycle = new TestReferenceLifecycle();
    TestReference reference = new TestReference(referenceLifecycle);

    Drawable acquiredDrawable = acquire(mContext, reference);
    assertThat(referenceLifecycle.mAcquired).isTrue();

    release(mContext, acquiredDrawable, reference);
    assertThat(referenceLifecycle.mReleased).isTrue();
  }

  private static class TestReference extends Reference<Drawable> {
    private TestReference(ReferenceLifecycle<Drawable> lifecycle) {
      super(lifecycle);
    }

    @Override
    public String getSimpleName() {
      return "TestReference";
    }
  }

  private static class TestReferenceLifecycle extends ReferenceLifecycle<Drawable> {
    private boolean mAcquired;
    private boolean mReleased;

    @Override
    protected Drawable onAcquire(Context c, Reference<Drawable> reference) {
      mAcquired = true;
      return new ColorDrawable(0);
    }

    @Override
    protected void onRelease(Context c, Drawable value, Reference<Drawable> reference) {
      mReleased = true;
    }
  }
}
