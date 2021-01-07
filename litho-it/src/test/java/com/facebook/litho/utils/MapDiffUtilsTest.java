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

package com.facebook.litho.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.testing.testrunner.LithoTestRunner;
import java.util.HashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class MapDiffUtilsTest {

  @Test
  public void testSameInstance() {
    HashMap<String, Object> map = new HashMap<>();
    map.put("test", new Object());

    assertThat(MapDiffUtils.areMapsEqual(map, map)).isTrue();
  }

  @Test
  public void testEmpty() {
    HashMap<String, Object> map = new HashMap<>();
    HashMap<String, Object> map2 = new HashMap<>();

    assertThat(MapDiffUtils.areMapsEqual(map, map2)).isTrue();
  }

  @Test
  public void testNotEmpty() {
    HashMap<String, Object> map = new HashMap<>();
    map.put("test", "hello");
    map.put("test2", "foo");
    map.put("test3", "bar");

    HashMap<String, Object> map2 = new HashMap<>();
    map2.put("test3", "bar");
    map2.put("test2", "foo");
    map2.put("test", "hello");

    assertThat(MapDiffUtils.areMapsEqual(map, map2)).isTrue();
  }

  @Test
  public void testNotSameSize() {
    HashMap<String, Object> map = new HashMap<>();
    map.put("test", "hello");
    map.put("test2", "foo");
    map.put("test3", "bar");

    HashMap<String, Object> map2 = new HashMap<>();
    map2.put("test3", "bar");
    map2.put("test2", "foo");

    assertThat(MapDiffUtils.areMapsEqual(map, map2)).isFalse();
  }

  @Test
  public void testNotSameContent() {
    HashMap<String, Object> map = new HashMap<>();
    map.put("test", "hello");

    HashMap<String, Object> map2 = new HashMap<>();
    map2.put("test", "hello2");

    assertThat(MapDiffUtils.areMapsEqual(map, map2)).isFalse();
  }

  @Test
  public void testOneNull() {
    HashMap<String, Object> map = new HashMap<>();
    map.put("test", "hello");

    assertThat(MapDiffUtils.areMapsEqual(map, null)).isFalse();
    assertThat(MapDiffUtils.areMapsEqual(null, map)).isFalse();
  }

  @Test
  public void testBothNull() {
    assertThat(MapDiffUtils.areMapsEqual(null, null)).isTrue();
  }

  @Test
  public void testNullValues() {
    HashMap<String, Object> map = new HashMap<>();
    map.put("test", "hello");
    map.put("test2", null);
    HashMap<String, Object> map2 = new HashMap<>();
    map2.put("test", "hello");
    map2.put("test2", null);

    assertThat(MapDiffUtils.areMapsEqual(map, map2)).isTrue();
  }
}
