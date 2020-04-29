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
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import androidx.annotation.Nullable;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class LogTreePopulatorTest {
  private ComponentContext mContext;

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(getApplicationContext(), "test", new TestComponentsLogger());
  }

  @Test
  public void testCustomTreePropLogger() {
    final ComponentsLogger logger =
        new TestComponentsLogger() {
          @Nullable
          @Override
          public Map<String, String> getExtraAnnotations(TreeProps treeProps) {
            final Object o = treeProps.get(MyKey.class);

            final Map<String, String> map = new HashMap<>(1);
            map.put("my_key", String.valueOf((int) o));

            return map;
          }
        };

    final PerfEvent event = mock(PerfEvent.class);
    final TreeProps treeProps = new TreeProps();
    treeProps.put(MyKey.class, 1337);
    mContext.setTreeProps(treeProps);

    LogTreePopulator.populatePerfEventFromLogger(mContext, logger, event);

    verify(event).markerAnnotate("my_key", "1337");
  }

  @Test
  public void testSkipOnEmptyTag() {
    final TestComponentsLogger logger =
        new TestComponentsLogger() {
          @Nullable
          @Override
          public Map<String, String> getExtraAnnotations(TreeProps treeProps) {
            final Object o = treeProps.get(MyKey.class);

            final Map<String, String> map = new HashMap<>(1);
            map.put("my_key", String.valueOf((int) o));

            return map;
          }
        };

    final PerfEvent event = mock(PerfEvent.class);
    final TreeProps treeProps = new TreeProps();
    treeProps.put(MyKey.class, 1337);
    mContext.setTreeProps(treeProps);

    final ComponentContext noLogTagContext = new ComponentContext(getApplicationContext());
    final PerfEvent perfEvent =
        LogTreePopulator.populatePerfEventFromLogger(noLogTagContext, logger, event);

    assertThat(perfEvent).isNull();
    assertThat(logger.getCanceledPerfEvents()).containsExactly(event);

    verifyNoMoreInteractions(event);
  }

  @Test
  public void testNullTreePropLogger() {
    final ComponentsLogger logger =
        new TestComponentsLogger() {
          @Nullable
          @Override
          public Map<String, String> getExtraAnnotations(TreeProps treeProps) {
            return null;
          }
        };

    final PerfEvent event = mock(PerfEvent.class);
    final TreeProps treeProps = new TreeProps();
    treeProps.put(MyKey.class, 1337);
    mContext.setTreeProps(treeProps);

    LogTreePopulator.populatePerfEventFromLogger(mContext, logger, event);

    verify(event).markerAnnotate("log_tag", "test");
    verifyNoMoreInteractions(event);
  }

  @Test
  public void testGetAnnotationBundleFromLogger() {
    final ComponentsLogger logger =
        new TestComponentsLogger() {
          @Nullable
          @Override
          public Map<String, String> getExtraAnnotations(TreeProps treeProps) {
            final Object o = treeProps.get(MyKey.class);

            final Map<String, String> map = new LinkedHashMap<>(2);
            map.put("my_key", String.valueOf((int) o));
            map.put("other_key", "value");

            return map;
          }
        };

    final TreeProps treeProps = new TreeProps();
    final Component component = mock(Component.class);
    when(component.getScopedContext()).thenReturn(mContext);

    treeProps.put(MyKey.class, 1337);
    mContext.setTreeProps(treeProps);

    final String res = LogTreePopulator.getAnnotationBundleFromLogger(component, logger);
    assertThat(res).isEqualTo("my_key:1337;other_key:value;");
  }

  @Test
  public void testSkipNullPerfEvent() {
    final ComponentsLogger logger =
        new TestComponentsLogger() {
          @Nullable
          @Override
          public Map<String, String> getExtraAnnotations(TreeProps treeProps) {
            return null;
          }
        };

    assertThat(LogTreePopulator.populatePerfEventFromLogger(mContext, logger, null)).isNull();
  }

  private static class MyKey {}
}
