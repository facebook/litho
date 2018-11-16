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

package com.facebook.litho.sections;

import static com.facebook.litho.sections.Change.INSERT;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests {@link Change} */
@RunWith(ComponentsTestRunner.class)
public class ChangeTest {

  @Test
  public void testCreateChangeNull() {
    final Change change = new Change(INSERT, 0, -1, 5, null, null);
    assertThat(change.getRenderInfo()).isNotNull();
    assertThat(change.getRenderInfos()).isNotNull();
  }

  @Test
  public void testCreateChangeNullInList() {
    final List<RenderInfo> renderInfos = new ArrayList<>();
    renderInfos.add(ComponentRenderInfo.createEmpty());
    renderInfos.add(null);

    final Change change = new Change(INSERT, 0, -1, 5, null, renderInfos);

    assertThat(change.getRenderInfo()).isNotNull();

    final List<RenderInfo> changeRenderInfos = change.getRenderInfos();
    assertThat(changeRenderInfos).isNotNull();

    assertThat(changeRenderInfos.get(0)).isNotNull();
    assertThat(changeRenderInfos.get(1)).isNotNull();
  }
}
