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

package com.facebook.litho.testing.viewtree;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests {@link ComponentQueries} */
@RunWith(ComponentsTestRunner.class)
public class ComponentQueriesTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testNoTextOnComponent() {
    final LithoView view =
        ComponentTestHelper.mountComponent(mContext, Text.create(mContext).text("goodbye").build());

    assertThat(ComponentQueries.hasTextMatchingPredicate(view, Predicates.equalTo("hello")))
        .isFalse();
  }

  @Test
  public void testTextOnComponent() {
    final LithoView view =
        ComponentTestHelper.mountComponent(mContext, Text.create(mContext).text("hello").build());

    assertThat(ComponentQueries.hasTextMatchingPredicate(view, Predicates.equalTo("hello")))
        .isTrue();
  }

  @Test
  public void testExtractTextFromTextComponent() {
    final LithoView view =
        ComponentTestHelper.mountComponent(mContext, Text.create(mContext).text("hello").build());

    assertThat(view.getTextContent().getTextItems())
        .isEqualTo(ImmutableList.<CharSequence>of("hello"));
  }
}
