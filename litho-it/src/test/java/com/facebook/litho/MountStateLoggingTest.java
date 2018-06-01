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

import static com.facebook.litho.FrameworkLogEvents.EVENT_MOUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MOUNTED_CONTENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MOUNTED_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_UNMOUNTED_CONTENT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_UNMOUNTED_COUNT;
import static com.facebook.litho.testing.TestViewComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests that Mount events are only logged when tracing is enabled. */
@RunWith(ComponentsTestRunner.class)
public class MountStateLoggingTest {
  private ComponentContext mContext;
  private TestComponentsLogger mComponentsLogger;

  @Before
  public void setup() {
    mComponentsLogger = new TestComponentsLogger();
    mContext = new ComponentContext(RuntimeEnvironment.application, "tag", mComponentsLogger);
  }

  @Test
  public void testLogWhenTracing() {
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

    List<String> mountedNames = new ArrayList<>();
    List<String> unmountedNames = new ArrayList<>();
    mountedNames.add("TestViewComponent");
    mountedNames.add("TestViewComponent");

    verifyLoggingAndResetLogger(2, 0, mountedNames, unmountedNames);
  }

  @Test
  public void testNoLogWhenTracingDisabled() {
    mContext =
        new ComponentContext(
            RuntimeEnvironment.application,
            "tag",
            new TestComponentsLogger() {
              @Override
              public boolean isTracing(PerfEvent logEvent) {
                return false;
              }
            });

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
    final LogEvent event = mComponentsLogger.newPerformanceEvent(EVENT_MOUNT);

    final List<PerfEvent> loggedPerfEvents = mComponentsLogger.getLoggedPerfEvents();

    assertThat(loggedPerfEvents).isEmpty();
  }

  private void verifyLoggingAndResetLogger(
      int mountedCount,
      int unmountedCount,
      List<String> mountedNames,
      List<String> unmountedNames) {
    final TestPerfEvent perfEvent = (TestPerfEvent) mComponentsLogger.getLoggedPerfEvents().get(1);
    final Map<String, Object> annotations = perfEvent.getAnnotations();
    assertThat(annotations).containsEntry(PARAM_MOUNTED_COUNT, mountedCount);
    assertThat(annotations).containsEntry(PARAM_UNMOUNTED_COUNT, unmountedCount);
    assertThat(annotations)
        .containsEntry(PARAM_MOUNTED_CONTENT, mountedNames.toArray(new String[0]));
    assertThat(annotations)
        .containsEntry(PARAM_UNMOUNTED_CONTENT, unmountedNames.toArray(new String[0]));
  }
}
