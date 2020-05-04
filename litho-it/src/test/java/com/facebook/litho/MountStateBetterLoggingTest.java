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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.FrameworkLogEvents.EVENT_MOUNT;
import static com.facebook.litho.testing.TestViewComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests that Mount events are only logged when tracing is enabled. */
@RunWith(LithoTestRunner.class)
public class MountStateBetterLoggingTest {
  private ComponentContext mContext;

  private TestComponentsLogger mLogger;

  @Before
  public void setup() {
    mLogger = new TestComponentsLogger();
    mContext = new ComponentContext(getApplicationContext(), "tag", mLogger);
  }

  @Test
  public void testLogMountEvent() {
    final TestComponent child1 = create(mContext).build();
    final TestComponent child2 = create(mContext).build();
    mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                .build();
          }
        });

    final List<PerfEvent> loggedPerfEvents = mLogger.getLoggedPerfEvents();
    final List<PerfEvent> perfEvents =
        loggedPerfEvents.stream()
            .filter(
                new Predicate<PerfEvent>() {
                  @Override
                  public boolean test(PerfEvent e) {
                    return e.getMarkerId() == EVENT_MOUNT;
                  }
                })
            .collect(Collectors.toList());
    assertThat(perfEvents).hasSize(1);

    final TestPerfEvent mountEvent = (TestPerfEvent) perfEvents.get(0);
    final Map<String, Object> annotations = mountEvent.getAnnotations();
    assertThat(annotations)
        .hasSize(18)
        .containsEntry("log_tag", "tag")
        .containsEntry("mounted_content", new String[] {"TestViewComponent", "TestViewComponent"})
        .containsEntry("mounted_count", 2)
        .containsEntry("moved_count", 0)
        .containsEntry("no_op_count", 0)
        .containsEntry("unmounted_content", new String[] {})
        .containsEntry("updated_content", new String[] {"HostComponent"})
        .containsEntry("updated_count", 1)
        .containsEntry("unmounted_time_ms", new Double[] {})
        .containsKey("visibility_handler_time_ms")
        .containsKey("visibility_handlers_total_time_ms")
        .containsKeys("mounted_extras", "mounted_time_ms", "updated_time_ms");
  }
}
