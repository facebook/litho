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
import androidx.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.MountDelegateInput;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension.IncrementalMountExtensionState;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtensionInput;
import com.facebook.rendercore.incrementalmount.IncrementalMountOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

@RunWith(LithoTestRunner.class)
public class IncrementalMountExtensionTest {

  @Test
  public void onIteratingOnIncrementMountOutputs_shouldIterateByInsertionOrder() {
    final TestInput input = new TestInput(10);
    int i = 0;
    for (IncrementalMountOutput output : input.getIncrementalMountOutputs()) {
      assertThat(output.getId()).isEqualTo(input.getMountableOutputAt(i).getRenderUnit().getId());
      i++;
    }
  }

  @Test
  public void testDirtyMountWithEmptyRect() {
    final LithoView lithoView = mock(LithoView.class);
    when(lithoView.getHeight()).thenReturn(50);
    final MountDelegate mountDelegate = mock(MountDelegate.class);
    final MountDelegateTarget mountDelegateTarget = mock(MountDelegateTarget.class);
    when(mountDelegate.getMountDelegateTarget()).thenReturn(mountDelegateTarget);
    final IncrementalMountExtension extension = IncrementalMountExtension.getInstance();

    final ExtensionState<IncrementalMountExtensionState> extensionState =
        extension.createExtensionState(mountDelegate);
    final IncrementalMountExtensionState state = extensionState.getState();

    mountDelegate.addExtension(extension);
    when(mountDelegate.getExtensionState(extension)).thenReturn(extensionState);

    final TestInput incrementalMountExtensionInput = new TestInput(10);

    extension.beforeMount(extensionState, incrementalMountExtensionInput, new Rect(0, 0, 10, 50));
    for (int i = 0, size = incrementalMountExtensionInput.getMountableOutputCount();
        i < size;
        i++) {
      final RenderTreeNode node = incrementalMountExtensionInput.getMountableOutputAt(i);
      extension.beforeMountItem(extensionState, node, i);
    }
    extension.afterMount(extensionState);

    assertThat(extension.getPreviousBottomsIndex(state)).isEqualTo(0);
    assertThat(extension.getPreviousTopsIndex(state)).isEqualTo(5);

    final TestInput incrementalMountExtensionInput2 = new TestInput(3);
    extension.beforeMount(extensionState, incrementalMountExtensionInput2, new Rect(0, 0, 0, 0));
    for (int i = 0, size = incrementalMountExtensionInput2.getMountableOutputCount();
        i < size;
        i++) {
      final RenderTreeNode node = incrementalMountExtensionInput2.getMountableOutputAt(i);
      extension.beforeMountItem(extensionState, node, i);
    }
    extension.afterMount(extensionState);

    extension.onVisibleBoundsChanged(extensionState, new Rect(0, 0, 10, 50));

    assertThat(extension.getPreviousBottomsIndex(state)).isEqualTo(0);
    assertThat(extension.getPreviousTopsIndex(state)).isEqualTo(3);
  }

  @Test
  public void testDirtyMountWithEmptyRect_leftRightMatch() {
    final LithoView lithoView = mock(LithoView.class);
    when(lithoView.getHeight()).thenReturn(50);
    final MountDelegate mountDelegate = mock(MountDelegate.class);
    final MountDelegateTarget mountDelegateTarget = mock(MountDelegateTarget.class);
    when(mountDelegate.getMountDelegateTarget()).thenReturn(mountDelegateTarget);
    final IncrementalMountExtension extension = IncrementalMountExtension.getInstance();
    mountDelegate.addExtension(extension);

    final ExtensionState<IncrementalMountExtensionState> extensionState =
        extension.createExtensionState(mountDelegate);
    final IncrementalMountExtensionState state = extensionState.getState();
    mountDelegate.addExtension(extension);
    when(mountDelegate.getExtensionState(extension)).thenReturn(extensionState);

    final TestInput incrementalMountExtensionInput = new TestInput(10);

    extension.beforeMount(extensionState, incrementalMountExtensionInput, new Rect(0, 0, 10, 50));
    for (int i = 0, size = incrementalMountExtensionInput.getMountableOutputCount();
        i < size;
        i++) {
      final RenderTreeNode node = incrementalMountExtensionInput.getMountableOutputAt(i);
      extension.beforeMountItem(extensionState, node, i);
    }
    extension.afterMount(extensionState);

    assertThat(extension.getPreviousBottomsIndex(state)).isEqualTo(0);
    assertThat(extension.getPreviousTopsIndex(state)).isEqualTo(5);

    final TestInput incrementalMountExtensionInput2 = new TestInput(3);
    extension.beforeMount(extensionState, incrementalMountExtensionInput2, new Rect(0, 0, 10, 0));
    for (int i = 0, size = incrementalMountExtensionInput2.getMountableOutputCount();
        i < size;
        i++) {
      final RenderTreeNode node = incrementalMountExtensionInput2.getMountableOutputAt(i);
      extension.beforeMountItem(extensionState, node, i);
    }
    extension.afterMount(extensionState);

    extension.onVisibleBoundsChanged(extensionState, new Rect(0, 0, 10, 50));

    assertThat(extension.getPreviousBottomsIndex(state)).isEqualTo(0);
    assertThat(extension.getPreviousTopsIndex(state)).isEqualTo(3);
  }

  final class TestInput implements IncrementalMountExtensionInput, MountDelegateInput {
    final List<RenderTreeNode> mountableOutputs = new ArrayList<>();
    final Map<Long, IncrementalMountOutput> mIncrementalMountOutputs = new LinkedHashMap<>();
    final List<IncrementalMountOutput> tops = new ArrayList<>();
    final List<IncrementalMountOutput> bottoms = new ArrayList<>();
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
        when(renderUnit.getId()).thenReturn((long) i);
        when(renderTreeNode.getRenderUnit()).thenReturn(renderUnit);

        mountableOutputs.add(renderTreeNode);
        final IncrementalMountOutput incrementalMountOutput =
            new IncrementalMountOutput(
                i, i, bounds, i != 0 ? mIncrementalMountOutputs.get((long) (i - 1)) : null);
        mIncrementalMountOutputs.put(incrementalMountOutput.getId(), incrementalMountOutput);
        tops.add(incrementalMountOutput);
        bottoms.add(incrementalMountOutput);
      }
    }

    @Override
    public int getMountableOutputCount() {
      return mCount;
    }

    @Override
    public List<IncrementalMountOutput> getOutputsOrderedByTopBounds() {
      return tops;
    }

    @Override
    public List<IncrementalMountOutput> getOutputsOrderedByBottomBounds() {
      return bottoms;
    }

    @Override
    public @Nullable IncrementalMountOutput getIncrementalMountOutputForId(long id) {
      return mIncrementalMountOutputs.get(id);
    }

    @Override
    public Collection<IncrementalMountOutput> getIncrementalMountOutputs() {
      return mIncrementalMountOutputs.values();
    }

    @Override
    public int getIncrementalMountOutputCount() {
      return mCount;
    }

    @Override
    public int getPositionForId(long id) {
      return 0;
    }

    @Override
    public boolean renderUnitWithIdHostsRenderTrees(long id) {
      return true;
    }

    @Override
    public RenderTreeNode getMountableOutputAt(int position) {
      return mountableOutputs.get(position);
    }
  }
}
