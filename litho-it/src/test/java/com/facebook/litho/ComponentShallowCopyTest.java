/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import com.facebook.litho.widget.Text;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class ComponentShallowCopyTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testShallowCopyCachedLayoutSameLayoutState() {
    mContext = ComponentContext.withComponentTree(mContext, ComponentTree.create(mContext).build());

    final LayoutState layoutState = new LayoutState(mContext);

    final ComponentContext c = new ComponentContext(mContext);
    final LayoutStateContext layoutStateContext =
        new LayoutStateContext(layoutState, c.getComponentTree());
    final RenderStateContext renderStateContext = layoutStateContext.getRenderStateContext();
    c.setLayoutStateContext(layoutStateContext);
    Whitebox.setInternalState(layoutState, "mLayoutStateContext", layoutStateContext);

    final MeasuredResultCache resultCache = renderStateContext.getCache();

    Component component = SimpleMountSpecTester.create(mContext).build();
    component.measure(c, 100, 100, new Size());
    assertThat(resultCache.getCachedResult(component)).isNotNull();

    Component copyComponent = component.makeShallowCopy();
    assertThat(resultCache.getCachedResult(copyComponent)).isNotNull();

    assertThat(resultCache.getCachedResult(component))
        .isEqualTo(resultCache.getCachedResult(copyComponent));
  }

  @Test
  public void testShallowCopyCachedLayoutOtherLayoutStateCacheLayoutState() {
    mContext = ComponentContext.withComponentTree(mContext, ComponentTree.create(mContext).build());

    final LayoutState layoutState1 = new LayoutState(mContext);
    final LayoutState layoutState2 = new LayoutState(mContext);

    final ComponentContext c1 = new ComponentContext(mContext);
    final LayoutStateContext lsc1 = new LayoutStateContext(layoutState1, c1.getComponentTree());
    final RenderStateContext rsc1 = lsc1.getRenderStateContext();
    Whitebox.setInternalState(layoutState1, "mLayoutStateContext", lsc1);
    c1.setLayoutStateContext(lsc1);

    final ComponentContext c2 = new ComponentContext(mContext);
    final LayoutStateContext lsc2 = new LayoutStateContext(layoutState2, c2.getComponentTree());
    final RenderStateContext rsc2 = lsc2.getRenderStateContext();
    Whitebox.setInternalState(layoutState2, "mLayoutStateContext", lsc2);
    c2.setLayoutStateContext(lsc2);

    final MeasuredResultCache resultCache1 = rsc1.getCache();
    final MeasuredResultCache resultCache2 = rsc2.getCache();

    Component component = SimpleMountSpecTester.create(mContext).build();
    component.measure(c1, 100, 100, new Size());
    assertThat(resultCache1.getCachedResult(component)).isNotNull();

    Component copyComponent = component.makeShallowCopy();
    assertThat(resultCache2.getCachedResult(copyComponent)).isNull();
  }

  @Test
  public void shallowCopy_withManualKey_preservesManualKeyInformation() {
    Component component = Text.create(mContext).text("test").key("manual_key").build();
    assertThat(component.getKey()).isEqualTo("manual_key");
    assertThat(component.hasManualKey()).isTrue();

    Component shallowCopy = component.makeShallowCopy();
    assertThat(shallowCopy.getKey()).isEqualTo("manual_key");
    assertThat(shallowCopy.hasManualKey()).isTrue();
  }
}
