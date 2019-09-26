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

package com.facebook.litho;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.YELLOW;
import static com.facebook.litho.testing.TestDrawableComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.drawable.ColorDrawable;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeMountTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testRemountsWithNewInputOnSameLayout() {
    final LithoView lithoView = mountComponent(mContext, create(mContext).color(BLACK).build());
    shadowOf(lithoView).callOnAttachedToWindow();

    assertThat(lithoView.getDrawables()).hasSize(1);
    assertThat(((ColorDrawable) lithoView.getDrawables().get(0)).getColor()).isEqualTo(BLACK);

    lithoView.getComponentTree().setRoot(create(mContext).color(YELLOW).build());
    assertThat(lithoView.getDrawables()).hasSize(1);
    assertThat(((ColorDrawable) lithoView.getDrawables().get(0)).getColor()).isEqualTo(YELLOW);
  }
}
