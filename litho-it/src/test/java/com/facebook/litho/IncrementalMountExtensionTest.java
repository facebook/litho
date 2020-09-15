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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Rect;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class IncrementalMountExtensionTest {

  @Test
  public void testDirtyMountWithEmptyRect() {
    final LithoView lithoView = mock(LithoView.class);
    when(lithoView.getHeight()).thenReturn(50);
    final MountDelegate mountDelegate = mock(MountDelegate.class);
    final IncrementalMountExtension extension = new IncrementalMountExtension(lithoView);

    extension.registerToDelegate(mountDelegate);

    final IncrementalMountExtension.IncrementalMountExtensionInput incrementalMountExtensionInput =
        new TestInput(10);

    extension.beforeMount(incrementalMountExtensionInput, new Rect(0, 0, 10, 50));
    assertThat(extension.getPreviousBottomsIndex()).isEqualTo(0);
    assertThat(extension.getPreviousTopsIndex()).isEqualTo(5);

    final IncrementalMountExtension.IncrementalMountExtensionInput incrementalMountExtensionInput2 =
        new TestInput(3);
    extension.beforeMount(incrementalMountExtensionInput2, new Rect(0, 0, 0, 0));

    // extension.onViewOffset();

    extension.onVisibleBoundsChanged(new Rect(0, 0, 10, 50));

    assertThat(extension.getPreviousBottomsIndex()).isEqualTo(0);
    assertThat(extension.getPreviousTopsIndex()).isEqualTo(3);
  }

  @Test
  public void testDirtyMountWithEmptyRect_leftRightMatch() {
    final LithoView lithoView = mock(LithoView.class);
    when(lithoView.getHeight()).thenReturn(50);
    final MountDelegate mountDelegate = mock(MountDelegate.class);
    final IncrementalMountExtension extension = new IncrementalMountExtension(lithoView);

    extension.registerToDelegate(mountDelegate);

    final IncrementalMountExtension.IncrementalMountExtensionInput incrementalMountExtensionInput =
        new TestInput(10);

    extension.beforeMount(incrementalMountExtensionInput, new Rect(0, 0, 10, 50));
    assertThat(extension.getPreviousBottomsIndex()).isEqualTo(0);
    assertThat(extension.getPreviousTopsIndex()).isEqualTo(5);

    final IncrementalMountExtension.IncrementalMountExtensionInput incrementalMountExtensionInput2 =
        new TestInput(3);
    extension.beforeMount(incrementalMountExtensionInput2, new Rect(0, 0, 10, 0));

    extension.onVisibleBoundsChanged(new Rect(0, 0, 10, 50));

    assertThat(extension.getPreviousBottomsIndex()).isEqualTo(0);
    assertThat(extension.getPreviousTopsIndex()).isEqualTo(3);
  }

  final class TestInput implements IncrementalMountExtension.IncrementalMountExtensionInput {
    final List<RenderTreeNode> mountableOutputs = new ArrayList<>();
    final List<RenderTreeNode> tops = new ArrayList<>();
    final List<RenderTreeNode> bottoms = new ArrayList<>();
    private final int mCount;

    public TestInput(int count) {
      mCount = count;
      for (int i = 0; i < count; i++) {
        final Rect bounds = new Rect(0, (i * 10), 10, (i + 1) * 10);

        final LayoutOutput layoutOutput = mock(LayoutOutput.class);
        RenderTreeNode renderTreeNode = mock(RenderTreeNode.class);
        when(renderTreeNode.getLayoutData()).thenReturn(layoutOutput);
        when(renderTreeNode.getAbsoluteBounds(any(Rect.class))).thenReturn(bounds);
        when(layoutOutput.getComponent()).thenReturn(mock(Component.class));
        when(layoutOutput.getBounds()).thenReturn(bounds);

        RenderUnit renderUnit = new LithoRenderUnit(layoutOutput);
        when(renderUnit.getId()).thenReturn((i + 1) * 1L);
        when(renderTreeNode.getRenderUnit()).thenReturn(renderUnit);

        mountableOutputs.add(renderTreeNode);
        tops.add(renderTreeNode);
        bottoms.add(renderTreeNode);
      }
    }

    @Override
    public int getMountableOutputCount() {
      return mCount;
    }

    @Override
    public List<RenderTreeNode> getMountableOutputTops() {
      return tops;
    }

    @Override
    public List<RenderTreeNode> getMountableOutputBottoms() {
      return bottoms;
    }

    @Override
    public int getLayoutOutputPositionForId(long id) {
      return 0;
    }

    @Override
    public RenderTreeNode getMountableOutputAt(int position) {
      return mountableOutputs.get(position);
    }
  }
}
