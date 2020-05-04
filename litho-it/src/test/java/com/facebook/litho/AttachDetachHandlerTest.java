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
import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.facebook.litho.LayoutState.LayoutStateContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.TestAttachDetachComponent;
import com.facebook.litho.testing.TestWrappedComponentProp;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.Text;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.verification.VerificationMode;

@RunWith(LithoTestRunner.class)
public class AttachDetachHandlerTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testOnAttached() {
    final Component component = spy(TestAttachDetachComponent.create(mContext).build());
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return component;
              }
            });
    final AttachDetachHandler attachDetachHandler =
        lithoView.getComponentTree().getAttachDetachHandler();

    verify(component).onAttached((ComponentContext) any());
    verify(component, never()).onDetached((ComponentContext) any());

    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(1);
  }

  @Test
  public void testOnDetached() {
    final Component component = spy(TestAttachDetachComponent.create(mContext).build());
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return component;
              }
            });
    final AttachDetachHandler attachDetachHandler =
        lithoView.getComponentTree().getAttachDetachHandler();

    lithoView.release();

    verify(component).onAttached((ComponentContext) any());
    verify(component).onDetached((ComponentContext) any());

    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(0);
  }

  @Test
  public void testReplaceRootWithSameComponent() {
    final Component c3 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component c4 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component c1 = spy(TestAttachDetachComponent.create(mContext, c3, c4).build());
    final Component c2 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component r1 = spy(TestAttachDetachComponent.create(mContext, c1, c2).build());
    final LithoView lithoView = mountComponent(mContext, Column.create(mContext).child(r1).build());
    final AttachDetachHandler attachDetachHandler =
        lithoView.getComponentTree().getAttachDetachHandler();

    verify(r1).onAttached((ComponentContext) any());
    verify(c1).onAttached((ComponentContext) any());
    verify(c2).onAttached((ComponentContext) any());
    verify(c3).onAttached((ComponentContext) any());
    verify(c4).onAttached((ComponentContext) any());

    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(5);

    /*
             r1                r2
           /    \            /    \
         c1      c2   =>   c5      c6
        /  \                         \
      c3    c4                        c7
    */
    final Component c5 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component c7 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component c6 = spy(TestAttachDetachComponent.create(mContext, c7).build());
    final Component r2 = spy(TestAttachDetachComponent.create(mContext, c5, c6).build());
    lithoView.setComponent(Column.create(mContext).child(r2).build());

    verify(c3).onDetached((ComponentContext) any());
    verify(c4).onDetached((ComponentContext) any());
    verify(c7).onAttached((ComponentContext) any());
    verify(r1, never()).onDetached((ComponentContext) any());
    verify(c1, never()).onDetached((ComponentContext) any());
    verify(c2, never()).onDetached((ComponentContext) any());
    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(4);

    /*
            r2                  r3
          /    \              /
        c5      c6     =>   c8
                  \
                   c7
    */
    final Component c8 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component r3 = spy(TestAttachDetachComponent.create(mContext, c8).build());
    lithoView.setComponent(Column.create(mContext).child(r3).build());

    verify(c6).onDetached((ComponentContext) any());
    verify(c7).onDetached((ComponentContext) any());
    verify(r2, never()).onDetached((ComponentContext) any());
    verify(c8, never()).onDetached((ComponentContext) any());
    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(2);
  }

  @Test
  public void testReplaceRootWithDifferentComponent() {
    final Component c3 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component c4 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component c1 = spy(TestAttachDetachComponent.create(mContext, c3, c4).build());
    final Component c2 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component r1 = spy(TestAttachDetachComponent.create(mContext, c1, c2).build());
    final LithoView lithoView = mountComponent(mContext, Column.create(mContext).child(r1).build());
    final AttachDetachHandler attachDetachHandler =
        lithoView.getComponentTree().getAttachDetachHandler();

    verify(r1).onAttached((ComponentContext) any());
    verify(c1).onAttached((ComponentContext) any());
    verify(c2).onAttached((ComponentContext) any());
    verify(c3).onAttached((ComponentContext) any());
    verify(c4).onAttached((ComponentContext) any());

    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(5);

    /*
             r1                r2 (TestWrappedComponentProp)
           /    \            /    \
         c1      c2   =>   c5      c6
        /  \                         \
      c3    c4                        c7
    */
    final Component c5 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component c7 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component c6 = spy(TestAttachDetachComponent.create(mContext, c7).build());
    final Component r2 =
        TestWrappedComponentProp.create(mContext).componentList(ImmutableList.of(c5, c6)).build();
    lithoView.setComponent(Column.create(mContext).child(r2).build());

    verify(r1).onDetached((ComponentContext) any());
    verify(c1).onDetached((ComponentContext) any());
    verify(c2).onDetached((ComponentContext) any());
    verify(c3).onDetached((ComponentContext) any());
    verify(c4).onDetached((ComponentContext) any());

    verify(c5).onAttached((ComponentContext) any());
    verify(c6).onAttached((ComponentContext) any());
    verify(c7).onAttached((ComponentContext) any());
    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(3);
  }

  @Test
  public void testReplaceNoAttachableComponent() {
    final Component r1 = spy(TestAttachDetachComponent.create(mContext).build());
    final LithoView lithoView = mountComponent(mContext, Column.create(mContext).child(r1).build());
    verify(r1).onAttached((ComponentContext) any());

    final Component newRoot =
        Column.create(mContext).child(Text.create(mContext).text("new root")).build();
    lithoView.setComponent(newRoot);
    verify(r1).onDetached((ComponentContext) any());
  }

  @Test
  public void testMultipleSetSizeSpec() {
    final Component component = spy(TestAttachDetachComponent.create(mContext).build());
    testSetSizeSpec(mContext, component, 10);
  }

  @Test
  public void testMultipleSetRootAndSizeSpec() {
    final Component component = spy(TestAttachDetachComponent.create(mContext).build());
    testSetRootAndSizeSpec(mContext, component, 10);
  }

  @Test
  public void testMultipleSetSizeSpecWithNestedTree() {
    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = true;

    final ComponentContext measureContext = new ComponentContext(mContext);
    measureContext.setLayoutStateContextForTesting();

    final Component c1 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component c2 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component component =
        spy(TestAttachDetachComponent.create(mContext, true, c1, c2).build());
    assertThat(Component.isNestedTree(measureContext, component)).isTrue();

    testSetSizeSpec(mContext, component, 20);

    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = false;
  }

  @Test
  public void testMultipleSetRootAndSizeSpecWithNestedTree() {
    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = true;

    final ComponentContext measureContext = new ComponentContext(mContext);
    measureContext.setLayoutStateContextForTesting();

    final Component c1 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component c2 = spy(TestAttachDetachComponent.create(mContext).build());
    final Component component =
        spy(TestAttachDetachComponent.create(mContext, true, c1, c2).build());
    assertThat(Component.isNestedTree(measureContext, component)).isTrue();

    testSetRootAndSizeSpec(mContext, component, 20);

    ComponentsConfiguration.enableShouldCreateLayoutWithNewSizeSpec = false;
  }

  @Test
  public void testAttachDetachWithComponentCachedLayout() {
    final ComponentContext measureContext = new ComponentContext(mContext);
    final LayoutState layoutState = new LayoutState(measureContext);
    measureContext.setLayoutStateContext(new LayoutStateContext(layoutState));

    final Component component = spy(TestAttachDetachComponent.create(mContext, true).build());

    final int widthSpec = SizeSpec.makeSizeSpec(100, EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(100, EXACTLY);
    final Size outSize = new Size();
    component.measure(measureContext, widthSpec, heightSpec, outSize);
    assertThat(layoutState.getCachedLayout(component)).isNotNull();

    final Component container = Column.create(mContext).child(component).build();
    final ComponentTree componentTree = ComponentTree.create(mContext, container).build();
    componentTree.setSizeSpec(
        SizeSpec.makeSizeSpec(1024, AT_MOST), SizeSpec.makeSizeSpec(1024, AT_MOST));

    for (int i = 0; i < 10; i++) {
      componentTree.setSizeSpec(
          SizeSpec.makeSizeSpec(1024 + 10 * i, AT_MOST),
          SizeSpec.makeSizeSpec(1024 + 10 * i, AT_MOST));
    }

    verifyOnAttached(componentTree, component);
    verifyOnDetached(component, never());

    componentTree.release();
    verifyOnDetached(component);
  }

  @Test
  public void testSetSizeSpecConcurrently() throws InterruptedException {
    final Component component = spy(TestAttachDetachComponent.create(mContext).build());
    final ComponentTree componentTree =
        ComponentTree.create(mContext, Column.create(mContext).child(component)).build();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final Thread thread1 =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                for (int i = 0; i < 10; i++) {
                  componentTree.setSizeSpec(
                      SizeSpec.makeSizeSpec(100 + 10 * i, EXACTLY),
                      SizeSpec.makeSizeSpec(100 + 10 * i, EXACTLY));
                }
                latch1.countDown();
              }
            });

    final CountDownLatch latch2 = new CountDownLatch(1);
    final Thread thread2 =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                for (int i = 0; i < 10; i++) {
                  componentTree.setSizeSpec(
                      SizeSpec.makeSizeSpec(200 + 10 * i, EXACTLY),
                      SizeSpec.makeSizeSpec(200 + 10 * i, EXACTLY));
                }
                latch2.countDown();
              }
            });
    thread1.start();
    thread2.start();

    assertThat(latch1.await(5000, TimeUnit.MILLISECONDS)).isTrue();
    assertThat(latch2.await(5000, TimeUnit.MILLISECONDS)).isTrue();

    verify(component).onAttached((ComponentContext) any());
    verify(component, never()).onDetached((ComponentContext) any());
  }

  @Test
  public void testAttachDetachWithDifferentRoots() {
    final Component root = spy(TestAttachDetachComponent.create(mContext).build());
    final Component container = Column.create(mContext).child(root).build();
    final ComponentTree componentTree = ComponentTree.create(mContext, container).build();
    componentTree.setRootAndSizeSpec(
        container, SizeSpec.makeSizeSpec(1024, AT_MOST), SizeSpec.makeSizeSpec(1024, AT_MOST));

    verifyOnAttached(componentTree, root);
    verifyOnDetached(root, never());

    final Component root2 = spy(TestAttachDetachComponent.create(mContext).build());
    componentTree.setRoot(root2);

    verifyOnAttached(componentTree, root2);
    verifyOnDetached(root2, never());
    verifyOnDetached(root);
  }

  @Test
  public void testSetRootWithMeasure() {
    final Component root = spy(TestAttachDetachComponent.create(mContext).build());
    final Component container = Column.create(mContext).child(root).build();
    final ComponentTree componentTree = ComponentTree.create(mContext, container).build();
    final int widthSpec = SizeSpec.makeSizeSpec(1024, AT_MOST);
    final int heightSpec = SizeSpec.makeSizeSpec(1024, AT_MOST);
    componentTree.setRootAndSizeSpec(container, widthSpec, heightSpec);

    verify(root, times(1)).onAttached((ComponentContext) any());
    verify(root, never()).onDetached((ComponentContext) any());

    LithoView lithoView = new LithoView(mContext);
    lithoView.setComponentTree(componentTree);
    lithoView.measure(widthSpec, heightSpec);

    verify(root, times(1)).onAttached((ComponentContext) any());
    verify(root, never()).onDetached((ComponentContext) any());
  }

  private static void testSetSizeSpec(ComponentContext c, Component component, int times) {
    final ComponentTree componentTree =
        ComponentTree.create(c, Column.create(c).child(component)).build();

    final int width = 100;
    final int height = 100;
    for (int i = 0; i < times; i++) {
      componentTree.setSizeSpec(
          SizeSpec.makeSizeSpec(width + 10 * i, EXACTLY),
          SizeSpec.makeSizeSpec(height + 10 * i, EXACTLY));
    }
    verifyOnAttached(componentTree, component);
    verifyOnDetached(component, never());

    componentTree.release();
    verifyOnDetached(component);
  }

  private static void testSetRootAndSizeSpec(ComponentContext c, Component component, int times) {
    final ComponentTree componentTree =
        ComponentTree.create(c, Wrapper.create(c).delegate(component)).build();

    final int width = 100;
    final int height = 100;

    for (int i = 0; i < times; i++) {
      componentTree.setRootAndSizeSpec(
          Wrapper.create(c).delegate(component).build(),
          SizeSpec.makeSizeSpec(width + 10 * i, EXACTLY),
          SizeSpec.makeSizeSpec(height + 10 * i, EXACTLY));
    }

    verifyOnAttached(componentTree, component);
    verifyOnDetached(component, never());

    componentTree.release();
    verifyOnDetached(component);
  }

  private static void verifyOnAttached(ComponentTree componentTree, Component component) {
    final int attachedCount = verifyOnAttached(component, 1);
    final AttachDetachHandler attachDetachHandler = componentTree.getAttachDetachHandler();
    assertThat(attachDetachHandler.getAttached().size()).isEqualTo(attachedCount);
  }

  private static int verifyOnAttached(Component component, int num) {
    verify(component, times(num)).onAttached((ComponentContext) any());
    int attachedCount = num;

    if (component instanceof TestAttachDetachComponent) {
      final Component[] children = ((TestAttachDetachComponent) component).getChildren();
      if (children != null) {
        for (Component child : children) {
          if (child instanceof TestAttachDetachComponent) {
            attachedCount += verifyOnAttached(child, num);
          }
        }
      }
    }
    return attachedCount;
  }

  private static void verifyOnDetached(Component component) {
    verifyOnDetached(component, times(1));
  }

  private static void verifyOnDetached(Component component, VerificationMode mode) {
    verify(component, mode).onDetached((ComponentContext) any());

    if (component instanceof TestAttachDetachComponent) {
      final Component[] children = ((TestAttachDetachComponent) component).getChildren();
      if (children != null) {
        for (Component child : children) {
          if (child instanceof TestAttachDetachComponent) {
            verifyOnDetached(child, mode);
          }
        }
      }
    }
  }
}
