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

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.ComponentWithTreeProp;
import com.facebook.litho.widget.TextDrawable;
import com.facebook.litho.widget.treeprops.SimpleTreeProp;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LithoViewRuleTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Test
  public void onLithoViewRuleWithTreeProp_shouldPropagateTreeProp() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Component component = ComponentWithTreeProp.create(c).build();

    mLithoViewRule
        .setTreeProp(SimpleTreeProp.class, new SimpleTreeProp("test"))
        .attachToWindow()
        .setRoot(component)
        .measure()
        .layout();

    Object item = mLithoViewRule.getLithoView().getMountItemAt(0).getContent();
    assertThat(item).isInstanceOf(TextDrawable.class);
    assertThat(((TextDrawable) item).getText()).isEqualTo("test");
  }

  @Test(expected = RuntimeException.class)
  public void onLithoViewRuleWithoutTreeProp_shouldThrowException() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Component component = ComponentWithTreeProp.create(c).build();
    mLithoViewRule.attachToWindow().setRoot(component).measure().layout();
  }
}
