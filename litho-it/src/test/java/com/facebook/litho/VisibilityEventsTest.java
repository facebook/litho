/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.testing.ComponentTestHelper.mountComponent;
import static com.facebook.litho.testing.ComponentTestHelper.unbindComponent;
import static com.facebook.litho.testing.TestViewComponent.create;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class VisibilityEventsTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testDetachWithReleasedTreeTriggersInvisibilityItems() {
    final TestComponent<?> content = create(mContext).build();
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Layout.create(c, content)
                            .invisibleHandler(invisibleEventHandler)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            });

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 10));
    lithoView.getComponentTree().release();

    assertThat(content.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(invisibleEventHandler);
    unbindComponent(lithoView);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).contains(invisibleEventHandler);
  }
}
