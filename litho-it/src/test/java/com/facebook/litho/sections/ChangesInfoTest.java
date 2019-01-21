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

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests {@link ChangesInfo} */
@RunWith(ComponentsTestRunner.class)
public class ChangesInfoTest {

  @Test
  public void testGetVisibleChanges() {
    final List<Change> changes = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      changes.add(Change.insert(i, null, new Object()));
    }

    final ChangesInfo changesInfo = new ChangesInfo(changes);
    assertThat(changesInfo.getVisibleChanges(0, 5)).isEqualTo(changes.subList(0, 6));
  }
}
