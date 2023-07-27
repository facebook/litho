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

package com.facebook.rendercore;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.renderunits.HostRenderUnit;
import com.facebook.rendercore.testing.DrawableWrapperUnit;
import com.facebook.rendercore.testing.LayoutResultWrappingNode;
import com.facebook.rendercore.testing.RenderCoreTestRule;
import com.facebook.rendercore.testing.SimpleLayoutResult;
import com.facebook.rendercore.testing.TestLayoutResultVisitor.Result;
import com.facebook.rendercore.testing.TestMountExtension;
import com.facebook.rendercore.testing.TestRenderCoreExtension;
import com.facebook.rendercore.testing.ViewAssertions;
import com.facebook.rendercore.testing.ViewWrapperUnit;
import com.facebook.rendercore.testing.match.ViewMatchNode;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ViewHierarchyTest {

  public final @Rule RenderCoreTestRule mRenderCoreTestRule = new RenderCoreTestRule();

  @Test
  public void onRenderSimpleLayoutResult_shouldRenderTheView() {
    final LayoutResult root =
        SimpleLayoutResult.create()
            .renderUnit(new ViewWrapperUnit(new TextView(mRenderCoreTestRule.getContext()), 1))
            .width(100)
            .height(100)
            .build();

    mRenderCoreTestRule.useRootNode(new LayoutResultWrappingNode(root)).render();

    ViewAssertions.assertThat((View) mRenderCoreTestRule.getRootHost())
        .matches(
            ViewMatchNode.forType(Host.class)
                .child(ViewMatchNode.forType(TextView.class).bounds(0, 0, 100, 100)));
  }

  @Test
  public void onRenderSimpleLayoutResult_shouldRenderTheDrawable() {
    final LayoutResult root =
        SimpleLayoutResult.create()
            .renderUnit(new DrawableWrapperUnit(new ColorDrawable(Color.BLACK), 1))
            .width(100)
            .height(100)
            .build();

    mRenderCoreTestRule.useRootNode(new LayoutResultWrappingNode(root)).render();

    ViewAssertions.assertThat((View) mRenderCoreTestRule.getRootHost())
        .matches(ViewMatchNode.forType(Host.class).bounds(0, 0, 100, 100));

    final HostView host = (HostView) mRenderCoreTestRule.getRootHost();
    assertThat(host.getMountItemCount()).describedAs("Number mounted items").isEqualTo(1);
    final MountItem item = host.getMountItemAt(0);
    assertThat(item.getContent()).describedAs("Mounted item").isInstanceOf(ColorDrawable.class);
  }

  @Test
  public void onRenderNestedLayoutResultsWithoutHostRenderUnits_shouldRenderTheView() {
    final Context c = mRenderCoreTestRule.getContext();
    final LayoutResult root =
        SimpleLayoutResult.create()
            .width(200)
            .height(200)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 2))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 3))
                    .x(100)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .x(50)
                    .y(100)
                    .width(100)
                    .height(100)
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(new ViewWrapperUnit(new TextView(c), 4))
                            .width(100)
                            .height(100)))
            .build();

    mRenderCoreTestRule.useRootNode(new LayoutResultWrappingNode(root)).render();

    ViewAssertions.assertThat((View) mRenderCoreTestRule.getRootHost())
        .matches(
            ViewMatchNode.forType(Host.class)
                .child(
                    ViewMatchNode.forType(TextView.class)
                        .bounds(0, 0, 100, 100)
                        .absoluteBoundsForRootType(0, 0, 100, 100, RootHost.class))
                .child(
                    ViewMatchNode.forType(TextView.class)
                        .bounds(100, 0, 100, 100)
                        .absoluteBoundsForRootType(100, 0, 100, 100, RootHost.class))
                .child(
                    ViewMatchNode.forType(TextView.class)
                        .bounds(50, 100, 100, 100)
                        .absoluteBoundsForRootType(50, 100, 100, 100, RootHost.class)));
  }

  @Test
  public void onRenderNestedLayoutResultsWithoutHostRenderUnits_shouldRenderTheViewsAndDrawables() {
    final Context c = mRenderCoreTestRule.getContext();
    final LayoutResult root =
        SimpleLayoutResult.create()
            .width(240)
            .height(240)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new DrawableWrapperUnit(new ColorDrawable(Color.BLACK), 2))
                    .padding(5, 5, 5, 5)
                    .width(120)
                    .height(120))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new DrawableWrapperUnit(new ColorDrawable(Color.BLUE), 3))
                    .x(5)
                    .y(5)
                    .width(110)
                    .height(110))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 4))
                    .x(10)
                    .y(10)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new DrawableWrapperUnit(new ColorDrawable(Color.BLUE), 5))
                    .x(15)
                    .y(15)
                    .width(95)
                    .height(95))
            .build();

    mRenderCoreTestRule.useRootNode(new LayoutResultWrappingNode(root)).render();

    ViewAssertions.assertThat((View) mRenderCoreTestRule.getRootHost())
        .matches(
            ViewMatchNode.forType(Host.class)
                .child(
                    ViewMatchNode.forType(TextView.class)
                        .bounds(10, 10, 100, 100)
                        .absoluteBoundsForRootType(10, 10, 100, 100, RootHost.class)));

    final HostView host = (HostView) mRenderCoreTestRule.getRootHost();
    assertThat(host.getMountItemCount()).describedAs("Number mounted items").isEqualTo(4);
    assertThat(host.getMountItemAt(0).getContent())
        .describedAs("Mounted item")
        .isInstanceOf(ColorDrawable.class);
    assertThat(((Drawable) host.getMountItemAt(0).getContent()).getBounds())
        .describedAs("Drawable bounds are")
        .isEqualTo(new Rect(5, 5, 115, 115));
    assertThat(host.getMountItemAt(1).getContent())
        .describedAs("Mounted item")
        .isInstanceOf(ColorDrawable.class);
    assertThat(host.getMountItemAt(3).getContent())
        .describedAs("Mounted item")
        .isInstanceOf(ColorDrawable.class);
  }

  @Test
  public void onRenderNestedLayoutResultsWithHostRenderUnits_shouldRenderTheView() {
    final Context c = mRenderCoreTestRule.getContext();
    final LayoutResult root =
        SimpleLayoutResult.create()
            .width(200)
            .height(200)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 2))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 3))
                    .x(100)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new HostRenderUnit(4))
                    .x(50)
                    .y(100)
                    .width(100)
                    .height(100)
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(new ViewWrapperUnit(new TextView(c), 5))
                            .width(100)
                            .height(100)))
            .build();

    mRenderCoreTestRule.useRootNode(new LayoutResultWrappingNode(root)).render();

    ViewAssertions.assertThat((View) mRenderCoreTestRule.getRootHost())
        .matches(
            ViewMatchNode.forType(Host.class)
                .child(
                    ViewMatchNode.forType(TextView.class)
                        .bounds(0, 0, 100, 100)
                        .absoluteBoundsForRootType(0, 0, 100, 100, RootHost.class))
                .child(
                    ViewMatchNode.forType(TextView.class)
                        .bounds(100, 0, 100, 100)
                        .absoluteBoundsForRootType(100, 0, 100, 100, RootHost.class))
                .child(
                    ViewMatchNode.forType(Host.class)
                        .bounds(50, 100, 100, 100)
                        .child(
                            ViewMatchNode.forType(TextView.class)
                                .bounds(0, 0, 100, 100)
                                .absoluteBoundsForRootType(50, 100, 100, 100, RootHost.class))));
  }

  @Test
  public void onRenderDeeplyNestedMultiHostLayoutResults_shouldRenderTheView() {
    final Context c = mRenderCoreTestRule.getContext();
    final LayoutResult root =
        SimpleLayoutResult.create()
            .width(1000)
            .height(1000)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 1))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .y(100)
                    .width(400)
                    .height(400)
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(new ViewWrapperUnit(new TextView(c), 2))
                            .x(100)
                            .y(100)
                            .width(100)
                            .height(100))
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(new HostRenderUnit(3))
                            .x(200)
                            .y(200)
                            .width(200)
                            .height(200)
                            .child(
                                SimpleLayoutResult.create()
                                    .renderUnit(new ViewWrapperUnit(new TextView(c), 4))
                                    .x(100)
                                    .y(100)
                                    .width(100)
                                    .height(100))))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new HostRenderUnit(5))
                    .y(400)
                    .width(400)
                    .height(400)
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(new ViewWrapperUnit(new TextView(c), 6))
                            .x(100)
                            .y(100)
                            .width(100)
                            .height(100))
                    .child(
                        SimpleLayoutResult.create()
                            .renderUnit(new HostRenderUnit(7))
                            .x(200)
                            .y(200)
                            .width(200)
                            .height(200)
                            .child(
                                SimpleLayoutResult.create()
                                    .renderUnit(new ViewWrapperUnit(new TextView(c), 8))
                                    .x(100)
                                    .y(100)
                                    .width(100)
                                    .height(100))))
            .build();

    final TestRenderCoreExtension extension = new TestRenderCoreExtension();

    mRenderCoreTestRule
        .useExtensions(new RenderCoreExtension[] {extension})
        .useRootNode(new LayoutResultWrappingNode(root))
        .render();

    ViewAssertions.assertThat((View) mRenderCoreTestRule.getRootHost())
        .matches(
            ViewMatchNode.forType(Host.class)
                .child(ViewMatchNode.forType(TextView.class).bounds(0, 0, 100, 100))
                .child(
                    ViewMatchNode.forType(TextView.class)
                        .bounds(100, 200, 100, 100)
                        .absoluteBoundsForRootType(100, 200, 100, 100, RootHost.class))
                .child(
                    ViewMatchNode.forType(Host.class)
                        .bounds(200, 300, 200, 200)
                        .absoluteBoundsForRootType(200, 300, 200, 200, RootHost.class)
                        .child(
                            ViewMatchNode.forType(TextView.class)
                                .bounds(100, 100, 100, 100)
                                .absoluteBoundsForRootType(300, 400, 100, 100, RootHost.class)))
                .child(
                    ViewMatchNode.forType(Host.class)
                        .bounds(0, 400, 400, 400)
                        .absoluteBoundsForRootType(0, 400, 400, 400, RootHost.class)
                        .child(
                            ViewMatchNode.forType(TextView.class)
                                .bounds(100, 100, 100, 100)
                                .absoluteBoundsForRootType(100, 500, 100, 100, RootHost.class))
                        .child(
                            ViewMatchNode.forType(Host.class)
                                .bounds(200, 200, 200, 200)
                                .absoluteBoundsForRootType(200, 600, 200, 200, RootHost.class)
                                .child(
                                    ViewMatchNode.forType(TextView.class)
                                        .bounds(100, 100, 100, 100)
                                        .absoluteBoundsForRootType(
                                            300, 700, 100, 100, RootHost.class)))));

    final TestMountExtension e = (TestMountExtension) extension.getMountExtension();
    assertThat(e).isNotNull();
    assertThat(e.getInput()).isNotNull();
    assertThat((List) e.getInput()).hasSize(11);

    List<Result> results = (List) e.getInput();

    assertThat(results.get(0).x).isEqualTo(0);
    assertThat(results.get(0).y).isEqualTo(0);

    assertThat(results.get(1).x).isEqualTo(0);
    assertThat(results.get(1).y).isEqualTo(0);

    assertThat(results.get(2).x).isEqualTo(0);
    assertThat(results.get(2).y).isEqualTo(0);

    assertThat(results.get(3).x).isEqualTo(0);
    assertThat(results.get(3).y).isEqualTo(100);

    assertThat(results.get(4).x).isEqualTo(100);
    assertThat(results.get(4).y).isEqualTo(200);

    assertThat(results.get(5).x).isEqualTo(200);
    assertThat(results.get(5).y).isEqualTo(300);

    assertThat(results.get(6).x).isEqualTo(300);
    assertThat(results.get(6).y).isEqualTo(400);

    assertThat(results.get(7).x).isEqualTo(0);
    assertThat(results.get(7).y).isEqualTo(400);

    assertThat(results.get(8).x).isEqualTo(100);
    assertThat(results.get(8).y).isEqualTo(500);

    assertThat(results.get(9).x).isEqualTo(200);
    assertThat(results.get(9).y).isEqualTo(600);

    assertThat(results.get(10).x).isEqualTo(300);
    assertThat(results.get(10).y).isEqualTo(700);
  }
}
