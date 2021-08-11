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

package com.facebook.rendercore.incrementalmount;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Node;
import com.facebook.rendercore.Node.LayoutResult;
import com.facebook.rendercore.RenderTreeHost;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.LayoutResultVisitor;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtension.IncrementalMountExtensionState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class IncrementalMountRenderCoreExtension
    extends RenderCoreExtension<IncrementalMountExtensionInput, IncrementalMountExtensionState> {

  public static final Comparator<IncrementalMountOutput> sTopsComparator =
      new Comparator<IncrementalMountOutput>() {
        @Override
        public int compare(IncrementalMountOutput l, IncrementalMountOutput r) {
          final int lhsTop = l.getBounds().top;
          final int rhsTop = r.getBounds().top;

          if (lhsTop == rhsTop) {
            if (l.getIndex() == r.getIndex()) {
              return 0;
            }

            // Lower indices should be higher for tops so that they are mounted first if possible.
            return l.getIndex() > r.getIndex() ? 1 : -1;
          } else {
            return lhsTop > rhsTop ? 1 : -1;
          }
        }
      };

  public static final Comparator<IncrementalMountOutput> sBottomsComparator =
      new Comparator<IncrementalMountOutput>() {
        @Override
        public int compare(IncrementalMountOutput l, IncrementalMountOutput r) {
          final int lhsBottom = l.getBounds().bottom;
          final int rhsBottom = r.getBounds().bottom;

          if (lhsBottom == rhsBottom) {
            if (l.getIndex() == r.getIndex()) {
              return 0;
            }

            // Lower indices should be lower for bottoms so that they are mounted first if possible.
            return l.getIndex() < r.getIndex() ? 1 : -1;
          } else {
            return lhsBottom > rhsBottom ? 1 : -1;
          }
        }
      };

  private final Visitor mLayoutResultVisitor;
  private static final IncrementalMountExtension mMountExtension =
      IncrementalMountExtension.getInstance(false);

  public IncrementalMountRenderCoreExtension(final InputProvider<?> provider) {
    mLayoutResultVisitor = new Visitor(provider);
  }

  @Override
  public LayoutResultVisitor<Results> getLayoutVisitor() {
    return mLayoutResultVisitor;
  }

  @Override
  public MountExtension<IncrementalMountExtensionInput, IncrementalMountExtensionState>
      getMountExtension() {
    return mMountExtension;
  }

  @Override
  public Results createInput() {
    return new Results();
  }

  public static class Results implements IncrementalMountExtensionInput {

    private final LinkedHashMap<Long, IncrementalMountOutput> outputs = new LinkedHashMap<>(8);
    private final SortedSet<IncrementalMountOutput> outputsOrderedByTopBounds =
        new TreeSet<>(sTopsComparator);
    private final SortedSet<IncrementalMountOutput> outputsOrderedByBottomBounds =
        new TreeSet<>(sBottomsComparator);
    private final Set<Long> renderUnitIdsWhichHostRenderTrees = new HashSet<>(4);

    private @Nullable List<IncrementalMountOutput> outputsOrderedByTopBoundsList;
    private @Nullable List<IncrementalMountOutput> outputsOrderedByBottomBoundsList;

    @Override
    public List<IncrementalMountOutput> getOutputsOrderedByTopBounds() {
      maybeInitializeList();
      return outputsOrderedByTopBoundsList;
    }

    @Override
    public List<IncrementalMountOutput> getOutputsOrderedByBottomBounds() {
      maybeInitializeList();
      return outputsOrderedByBottomBoundsList;
    }

    @Override
    public @Nullable IncrementalMountOutput getIncrementalMountOutputForId(long id) {
      return outputs.get(id);
    }

    @Override
    public Collection<IncrementalMountOutput> getIncrementalMountOutputs() {
      return outputs.values();
    }

    @Override
    public int getIncrementalMountOutputCount() {
      return outputs.size();
    }

    @Override
    public boolean renderUnitWithIdHostsRenderTrees(long id) {
      return renderUnitIdsWhichHostRenderTrees.contains(id);
    }

    void addOutput(IncrementalMountOutput output) {
      final IncrementalMountOutput existing = outputs.put(output.getId(), output);
      if (existing != null) {
        throw new IllegalArgumentException(
            "output with id="
                + output.getId()
                + " already exists."
                + "\nindex="
                + existing.getIndex()
                + (existing.getHostOutput() != null
                    ? "\nhostId=" + existing.getHostOutput().getId()
                    : "")
                + "\nbounds="
                + existing.getBounds());
      }
      outputsOrderedByTopBounds.add(output);
      outputsOrderedByBottomBounds.add(output);
    }

    void addRenderTreeHostId(long id) {
      renderUnitIdsWhichHostRenderTrees.add(id);
    }

    private void maybeInitializeList() {
      if (outputsOrderedByTopBoundsList == null || outputsOrderedByBottomBoundsList == null) {
        outputsOrderedByTopBoundsList = new ArrayList<>(outputsOrderedByTopBounds.size());
        outputsOrderedByBottomBoundsList = new ArrayList<>(outputsOrderedByBottomBounds.size());

        Iterator<IncrementalMountOutput> topIterator = outputsOrderedByTopBounds.iterator();
        Iterator<IncrementalMountOutput> bottomIterator = outputsOrderedByBottomBounds.iterator();

        while (topIterator.hasNext() && bottomIterator.hasNext()) {
          outputsOrderedByTopBoundsList.add(topIterator.next());
          outputsOrderedByBottomBoundsList.add(bottomIterator.next());
        }
      }
    }
  }

  public static class Visitor implements LayoutResultVisitor<Results> {

    private final InputProvider provider;

    public Visitor(InputProvider provider) {
      this.provider = provider;
    }

    @Override
    public void visit(
        final @Nullable RenderTreeNode parent,
        final Node.LayoutResult result,
        final Rect bounds,
        final int x,
        final int y,
        final int position,
        final Results results) {

      if (position == 0) {
        return;
      }

      final RenderUnit<?> unit = result.getRenderUnit();
      if (unit == null) {
        return;
      }

      final long id = unit.getId();

      final IncrementalMountOutput host;
      if (parent == null) {
        throw new IllegalArgumentException("Parent was null for position=" + position);
      }

      host = results.getIncrementalMountOutputForId(parent.getRenderUnit().getId());

      final Rect rect = new Rect(x, y, x + bounds.width(), y + bounds.height());
      results.addOutput(new IncrementalMountOutput(id, position, rect, host));
      if (provider.hasRenderTreeHosts(result)) {
        results.addRenderTreeHostId(id);
      }
    }
  }

  /**
   * The provider that client frameworks must set on {@link IncrementalMountRenderCoreExtension} to
   * enable the extension to collect the required data from the {@link LayoutResult} during the
   * layout pass.
   */
  public interface InputProvider<R extends LayoutResult> {

    /** Return {@code true} if the {@param result} will host any {@link RenderTreeHost}. */
    boolean hasRenderTreeHosts(R result);
  }
}
