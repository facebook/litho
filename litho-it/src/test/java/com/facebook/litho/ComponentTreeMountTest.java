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

import static android.graphics.Color.BLACK;
import static android.graphics.Color.YELLOW;
import static com.facebook.litho.testing.TestDrawableComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import com.facebook.litho.testing.TestTransitionComponent;
import com.facebook.litho.testing.eventhandler.EventHandlerTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.EmptyComponent;
import com.facebook.litho.widget.SolidColor;
import com.facebook.litho.widget.Text;
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

  @Test
  public void testReentrantMounts() {
    final LithoView lithoView =
        mountComponent(mContext, EmptyComponent.create(mContext).build(), true, true);
    final EventHandler<VisibleEvent> visibleEventHandler =
        EventHandlerTestHelper.createMockEventHandler(
            VisibleEvent.class,
            new EventHandlerTestHelper.MockEventHandler<VisibleEvent, Void>() {
              @Override
              public Void handleEvent(VisibleEvent event) {
                lithoView.setComponent(
                    TestTransitionComponent.create(
                            mContext,
                            Row.create(mContext)
                                .child(Text.create(mContext).text("text").textSizeDip(20))
                                .child(
                                    SolidColor.create(mContext)
                                        .color(Color.BLUE)
                                        .widthDip(20)
                                        .heightDip(20))
                                .build())
                        .build());
                return null;
              }
            });
    lithoView.setComponent(
        TestTransitionComponent.create(
                mContext,
                Column.create(mContext)
                    .child(
                        SolidColor.create(mContext).color(Color.YELLOW).widthDip(10).heightDip(10))
                    .child(
                        SolidColor.create(mContext).color(Color.GREEN).widthDip(10).heightDip(10))
                    .child(SolidColor.create(mContext).color(Color.GRAY).widthDip(10).heightDip(10))
                    .build())
            .visibleHandler(visibleEventHandler)
            .build());
    lithoView.notifyVisibleBoundsChanged(new Rect(0, 0, 100, 100), true);
  }
}
