/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.FrameworkLogEvents.EVENT_PREPARE_MOUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MOVED_COUNT;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.testing.TestDrawableComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Color;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.litho.widget.SolidColor;
import com.facebook.litho.widget.Text;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class MountStateRemountInPlaceTest {
  private ComponentContext mContext;
  private ComponentsLogger mComponentsLogger;

  @Before
  public void setup() {
    mComponentsLogger = spy(new TestComponentsLogger());
    when(mComponentsLogger.newEvent(any(int.class))).thenCallRealMethod();
    when(mComponentsLogger.newPerformanceEvent(any(int.class))).thenCallRealMethod();

    mContext = new ComponentContext(RuntimeEnvironment.application, "tag", mComponentsLogger);
  }

  @Test
  public void testMountUnmountWithShouldUpdate() {
    final TestComponent firstComponent =
        create(mContext)
            .unique()
            .build();

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(firstComponent).build();
              }
            });

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent =
        create(mContext)
            .unique()
            .build();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(secondComponent).build();
              }
            });

    assertThat(secondComponent.wasOnMountCalled()).isTrue();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue();
  }

  @Test
  public void testMountUnmountWithNoShouldUpdate() {
    final TestComponent firstComponent =
        create(mContext)
            .build();

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(firstComponent).build();
              }
            });

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent =
        create(mContext)
            .build();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(secondComponent).build();
              }
            });

    assertThat(secondComponent.wasOnMountCalled()).isFalse();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();
  }

  @Test
  public void testMountUnmountWithNoShouldUpdateAndDifferentSize() {
    final TestComponent firstComponent =
        create(
            mContext,
            0,
            0,
            true,
            true,
            true,
            false,
            false,
            true /*isMountSizeDependent*/)
            .measuredHeight(10)
            .build();

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(firstComponent).build();
              }
            });

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent =
        create(
            mContext,
            0,
            0,
            true,
            true,
            true,
            false,
            false,
            true /*isMountSizeDependent*/)
            .measuredHeight(11)
            .build();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(secondComponent).build();
              }
            });

    assertThat(secondComponent.wasOnMountCalled()).isTrue();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue();
  }

  @Test
  public void testMountUnmountWithNoShouldUpdateAndSameSize() {
    final TestComponent firstComponent =
        create(
            mContext,
            0,
            0,
            true,
            true,
            true,
            false,
            false,
            true /*isMountSizeDependent*/)
            .measuredHeight(10)
            .build();

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(firstComponent).build();
              }
            });

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent =
        create(
            mContext,
            0,
            0,
            true,
            true,
            true,
            false,
            false,
            true /*isMountSizeDependent*/)
            .measuredHeight(10)
            .build();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(secondComponent).build();
              }
            });

    assertThat(secondComponent.wasOnMountCalled()).isFalse();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();
  }

  @Test
  public void testMountUnmountWithNoShouldUpdateAndDifferentMeasures() {
    final TestComponent firstComponent =
        create(mContext)
            .build();

    final LithoView lithoView =
        mountComponent(
            new LithoView(mContext),
            ComponentTree.create(
                    mContext,
                    new InlineLayoutSpec() {
                      @Override
                      protected Component onCreateLayout(ComponentContext c) {
                        return Column.create(c).child(firstComponent).build();
                      }
                    })
                .incrementalMount(false)
                .layoutDiffing(false)
                .build(),
            makeSizeSpec(100, AT_MOST),
            makeSizeSpec(100, AT_MOST));

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent =
        create(mContext)
            .build();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(secondComponent).widthPx(10).heightPx(10).build();
              }
            });

    assertThat(lithoView.isLayoutRequested()).isTrue();
    assertThat(secondComponent.wasOnMountCalled()).isFalse();
    assertThat(secondComponent.wasOnBindCalled()).isFalse();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();
  }

  @Test
  public void testMountUnmountWithNoShouldUpdateAndSameMeasures() {
    final TestComponent firstComponent =
        create(mContext, 0, 0, true, true, true, false, false, true)
            .color(Color.GRAY)
            .build();

    final LithoView lithoView =
        mountComponent(
            new LithoView(mContext),
            ComponentTree.create(
                    mContext,
                    new InlineLayoutSpec() {
                      @Override
                      protected Component onCreateLayout(ComponentContext c) {
                        return Column.create(c).child(firstComponent).build();
                      }
                    })
                .incrementalMount(false)
                .layoutDiffing(false)
                .build(),
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY));

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent =
        create(mContext)
            .color(Color.RED)
            .build();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(secondComponent).widthPx(10).heightPx(10).build();
              }
            });

    assertThat(lithoView.isLayoutRequested()).isFalse();
    assertThat(secondComponent.wasOnMountCalled()).isTrue();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue();
  }

  @Test
  public void testRebindWithNoShouldUpdateAndSameMeasures() {
    final TestComponent firstComponent =
        create(mContext)
            .build();

    final LithoView lithoView =
        mountComponent(
            new LithoView(mContext),
            ComponentTree.create(
                    mContext,
                    new InlineLayoutSpec() {
                      @Override
                      protected Component onCreateLayout(ComponentContext c) {
                        return Column.create(c).child(firstComponent).build();
                      }
                    })
                .incrementalMount(false)
                .layoutDiffing(false)
                .build(),
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY));

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent =
        create(mContext)
            .build();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(secondComponent).widthPx(10).heightPx(10).build();
              }
            });

    assertThat(lithoView.isLayoutRequested()).isFalse();
    assertThat(secondComponent.wasOnMountCalled()).isFalse();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();
  }

  @Test
  public void testMountUnmountWithSkipShouldUpdate() {
    final TestComponent firstComponent =
        create(mContext)
            .color(BLACK)
            .build();

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(firstComponent).build();
              }
            });

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent =
        create(mContext)
            .color(BLACK)
            .build();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(secondComponent).build();
              }
            });

    assertThat(secondComponent.wasOnMountCalled()).isFalse();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();
  }

  @Test
  public void testMountUnmountWithSkipShouldUpdateAndRemount() {
    final TestComponent firstComponent =
        create(mContext)
            .color(BLACK)
            .build();

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(firstComponent).build();
              }
            });

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent =
        create(mContext)
            .color(WHITE)
            .build();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(secondComponent).build();
              }
            });

    assertThat(secondComponent.wasOnMountCalled()).isTrue();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue();
  }

  @Test
  public void testMountUnmountDoesNotSkipShouldUpdateAndRemount() {
    final TestComponent firstComponent =
        create(mContext)
            .unique()
            .build();

    final LithoView firstLithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(firstComponent).build();
              }
            });

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent =
        create(mContext)
            .unique()
            .build();

    final ComponentTree secondTree =
        ComponentTree.create(
                mContext,
                new InlineLayoutSpec() {
                  @Override
                  protected Component onCreateLayout(ComponentContext c) {
                    return Column.create(c).child(secondComponent).build();
                  }
                })
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();
    secondTree.setSizeSpec(100, 100);

    final TestComponent thirdComponent =
        create(mContext)
            .build();

    secondTree.setRoot(
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c).child(thirdComponent).build();
          }
        });

    mountComponent(firstLithoView, secondTree);

    assertThat(thirdComponent.wasOnMountCalled()).isTrue();
    assertThat(thirdComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue();
  }

  @Test
  public void testSkipShouldUpdateAndRemountForUnsupportedComponent() {
    final TestComponent firstComponent =
        create(
            mContext,
            false,
            true,
            true,
            false,
            false)
            .build();

    final LithoView firstLithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c).child(firstComponent).build();
              }
            });

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent =
        create(
            mContext,
            false,
            true,
            true,
            false,
            false)
            .build();

    final ComponentTree secondTree =
        ComponentTree.create(
                mContext,
                new InlineLayoutSpec() {
                  @Override
                  protected Component onCreateLayout(ComponentContext c) {
                    return Column.create(c).child(secondComponent).build();
                  }
                })
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();
    secondTree.setSizeSpec(100, 100);

    mountComponent(firstLithoView, secondTree);

    assertThat(secondComponent.wasOnMountCalled()).isTrue();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue();
  }

  @Test
  public void testRemountSameSubTreeWithDifferentParentHost() {
    final TestComponent firstComponent =
        TestDrawableComponent.create(
            mContext,
            false,
            true,
            true,
            false,
            false)
            .build();

    InlineLayoutSpec firstLayout =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(
                    Column.create(c)
                        .clickHandler(c.newEventHandler(3))
                        .child(Text.create(c).text("test")))
                .child(
                    Column.create(c)
                        .clickHandler(c.newEventHandler(2))
                        .child(Text.create(c).text("test2"))
                        .child(
                            Column.create(c)
                                .clickHandler(c.newEventHandler(1))
                                .child(firstComponent)
                                .child(SolidColor.create(c).color(Color.GREEN))))
                .build();
          }
        };

    final InlineLayoutSpec secondLayout =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(
                    Column.create(c)
                        .clickHandler(c.newEventHandler(3))
                        .child(Text.create(c).text("test"))
                        .child(
                            Column.create(c)
                                .clickHandler(c.newEventHandler(1))
                                .child(firstComponent)
                                .child(SolidColor.create(c).color(Color.GREEN))))
                .child(
                    Column.create(c)
                        .clickHandler(c.newEventHandler(2))
                        .child(Text.create(c).text("test2")))
                .build();
          }
        };

    ComponentTree tree = ComponentTree.create(mContext, firstLayout)
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();
    LithoView cv = new LithoView(mContext);
    ComponentTestHelper.mountComponent(cv, tree);
    tree.setRoot(secondLayout);

    final LogEvent event = mComponentsLogger.newPerformanceEvent(EVENT_PREPARE_MOUNT);
    event.addParam(PARAM_MOVED_COUNT, "2");
    verify(mComponentsLogger).log(eq(event));
  }
}
