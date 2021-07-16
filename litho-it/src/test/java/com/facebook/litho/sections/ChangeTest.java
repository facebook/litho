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

package com.facebook.litho.sections;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests {@link Change} */
@RunWith(LithoTestRunner.class)
public class ChangeTest {

  @Test
  public void testCreateChangeNull() {
    final Change change = Change.insertRange(0, 5, null);
    assertThat(change.getRenderInfo()).isNotNull();
    assertThat(change.getRenderInfos()).isNotNull();
  }

  @Test
  public void testCreateChangeNullInList() {
    final List<RenderInfo> renderInfos = new ArrayList<>();
    renderInfos.add(ComponentRenderInfo.createEmpty());
    renderInfos.add(null);

    final Change change = Change.insertRange(0, 5, renderInfos);

    assertThat(change.getRenderInfo()).isNotNull();

    final List<RenderInfo> changeRenderInfos = change.getRenderInfos();
    assertThat(changeRenderInfos).isNotNull();

    assertThat(changeRenderInfos.get(0)).isNotNull();
    assertThat(changeRenderInfos.get(1)).isNotNull();
  }

  @Test
  public void testInsert() {
    final Object data = new Object();
    final Change change = Change.insert(0, ComponentRenderInfo.createEmpty(), data);

    assertThat(change.getRenderInfo()).isNotNull();
    assertThat(change.getRenderInfos()).isEmpty();
    assertThat(change.getPrevData()).isNull();
    assertThat(change.getNextData()).isEqualTo(ImmutableList.of(data));
  }

  @Test
  public void testInsertRange() {
    final List<RenderInfo> renderInfos = new ArrayList<>();
    final List<Object> data = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      renderInfos.add(ComponentRenderInfo.createEmpty());
      data.add(new Object());
    }
    final Change change = Change.insertRange(0, 5, renderInfos, data);

    assertThat(change.getRenderInfo()).isNotNull();
    assertThat(change.getRenderInfos()).isNotEmpty();
    assertThat(change.getPrevData()).isNull();
    assertThat(change.getNextData()).isEqualTo(data);
  }

  @Test
  public void testUpdate() {
    final Object oldData = new Object();
    final Object newData = new Object();
    final Change change = Change.update(0, ComponentRenderInfo.createEmpty(), oldData, newData);

    assertThat(change.getRenderInfo()).isNotNull();
    assertThat(change.getRenderInfos()).isEmpty();
    assertThat(change.getPrevData()).isEqualTo(ImmutableList.of(oldData));
    assertThat(change.getNextData()).isEqualTo(ImmutableList.of(newData));
  }

  @Test
  public void testUpdateRange() {
    final List<RenderInfo> renderInfos = new ArrayList<>();
    final List<Object> oldData = new ArrayList<>();
    final List<Object> newData = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      renderInfos.add(ComponentRenderInfo.createEmpty());
      oldData.add(new Object());
      newData.add(new Object());
    }
    final Change change = Change.updateRange(0, 5, renderInfos, oldData, newData);

    assertThat(change.getRenderInfo()).isNotNull();
    assertThat(change.getRenderInfos()).isNotEmpty();
    assertThat(change.getPrevData()).isEqualTo(oldData);
    assertThat(change.getNextData()).isEqualTo(newData);
  }

  @Test
  public void testRemove() {
    final Object data = new Object();
    final Change change = Change.remove(0, data);

    assertThat(change.getRenderInfo()).isNotNull();
    assertThat(change.getRenderInfos()).isEmpty();
    assertThat(change.getPrevData()).isEqualTo(ImmutableList.of(data));
    assertThat(change.getNextData()).isNull();
  }

  @Test
  public void testRemoveRange() {
    final List<RenderInfo> renderInfos = new ArrayList<>();
    final List<Object> data = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      renderInfos.add(ComponentRenderInfo.createEmpty());
      data.add(new Object());
    }
    final Change change = Change.removeRange(0, 5, data);

    assertThat(change.getRenderInfo()).isNotNull();
    assertThat(change.getRenderInfos()).isEmpty();
    assertThat(change.getPrevData()).isEqualTo(data);
    assertThat(change.getNextData()).isNull();
  }

  @Test
  public void testMove() {
    final Object data = new Object();
    final Change change = Change.move(0, 1, data);

    assertThat(change.getRenderInfo()).isNotNull();
    assertThat(change.getRenderInfos()).isEmpty();
    assertThat(change.getPrevData()).isEqualTo(ImmutableList.of(data));
    assertThat(change.getNextData()).isEqualTo(ImmutableList.of(data));
  }
}
