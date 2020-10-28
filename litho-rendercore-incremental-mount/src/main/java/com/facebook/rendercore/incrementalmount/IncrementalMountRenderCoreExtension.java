// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore.incrementalmount;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.rendercore.MountState;
import com.facebook.rendercore.Node;
import com.facebook.rendercore.Node.LayoutResult;
import com.facebook.rendercore.RenderTreeHost;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.LayoutResultVisitor;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class IncrementalMountRenderCoreExtension
    extends RenderCoreExtension<IncrementalMountExtensionInput, Void> {

  public static final Comparator<IncrementalMountOutput> sTopsComparator =
      new Comparator<IncrementalMountOutput>() {
        @Override
        public int compare(IncrementalMountOutput l, IncrementalMountOutput r) {
          final int lhsTop = l.getBounds().top;
          final int rhsTop = r.getBounds().top;

          // Lower indices should be higher for tops so that they are mounted first if possible.
          if (lhsTop == rhsTop) {
            if (l.getIndex() == r.getIndex()) {
              return 0;
            }

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

          // Lower indices should be lower for bottoms so that they are mounted first if possible.
          if (lhsBottom == rhsBottom) {
            if (r.getIndex() == l.getIndex()) {
              return 0;
            }

            return r.getIndex() > l.getIndex() ? 1 : -1;
          } else {
            return lhsBottom > rhsBottom ? 1 : -1;
          }
        }
      };

  private final Visitor mLayoutResultVisitor;
  private final IncrementalMountExtension mMountExtension;

  public IncrementalMountRenderCoreExtension(final InputProvider<?> provider) {
    mLayoutResultVisitor = new Visitor(provider);
    mMountExtension = new IncrementalMountExtension(true);
  }

  @Override
  public LayoutResultVisitor<Results> getLayoutVisitor() {
    return mLayoutResultVisitor;
  }

  @Override
  public MountExtension<IncrementalMountExtensionInput, Void> getMountExtension() {
    return mMountExtension;
  }

  @Override
  public Results createInput() {
    return new Results();
  }

  public static class Results implements IncrementalMountExtensionInput {

    private final List<IncrementalMountOutput> outputs = new ArrayList<>(8);
    private final SortedSet<IncrementalMountOutput> outputsOrderedByTopBounds =
        new TreeSet<>(sTopsComparator);
    private final SortedSet<IncrementalMountOutput> outputsOrderedByBottomBounds =
        new TreeSet<>(sBottomsComparator);
    private final Set<Long> renderUnitWithIdHostsRenderTrees = new HashSet<>(4);
    private final HashMap<Long, Integer> positionForIdMap = new HashMap<>(8);

    private @Nullable List<IncrementalMountOutput> outputsOrderedByTopBoundsList;
    private @Nullable List<IncrementalMountOutput> outputsOrderedByBottomBoundsList;

    @Override
    @Nullable
    public List<IncrementalMountOutput> getOutputsOrderedByTopBounds() {
      maybeInitializeList();
      return outputsOrderedByTopBoundsList;
    }

    @Override
    @Nullable
    public List<IncrementalMountOutput> getOutputsOrderedByBottomBounds() {
      maybeInitializeList();
      return outputsOrderedByBottomBoundsList;
    }

    @Override
    public IncrementalMountOutput getIncrementalMountOutputAt(int position) {
      return outputs.get(position);
    }

    @Override
    public int getIncrementalMountOutputCount() {
      return outputs.size();
    }

    @Override
    public boolean renderUnitWithIdHostsRenderTrees(long id) {
      return renderUnitWithIdHostsRenderTrees.contains(id);
    }

    @Override
    public int getPositionForId(long id) {
      Integer position = positionForIdMap.get(id);
      if (position == null) {
        throw new IllegalArgumentException("No position found for item with id: " + id);
      }
      return position;
    }

    void addOutput(IncrementalMountOutput output) {
      outputs.add(output);
      outputsOrderedByTopBounds.add(output);
      outputsOrderedByBottomBounds.add(output);
    }

    void addRenderTreeHostId(long id) {
      renderUnitWithIdHostsRenderTrees.add(id);
    }

    void addPositionForId(final long id, final int position) {
      positionForIdMap.put(id, position);
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
        final Node.LayoutResult<?> result,
        final Rect bounds,
        final int x,
        final int y,
        final int position,
        final Results results) {

      final long id;
      if (position == 0) {
        id = MountState.ROOT_HOST_ID;
      } else {
        final RenderUnit<?> unit = result.getRenderUnit();
        if (unit == null) {
          return;
        }
        id = unit.getId();
      }

      final long hostId;
      if (parent != null) {
        if (parent.getRenderUnit() == null) {
          throw new IllegalArgumentException("Parent Node must have a RenderUnit.");
        }
        hostId = parent.getRenderUnit().getId();
      } else {
        hostId = -1;
      }

      final Rect rect = new Rect(x, y, x + bounds.width(), y + bounds.height());
      results.addOutput(new IncrementalMountOutput(id, position, rect, hostId));
      results.addPositionForId(id, position);
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
  public interface InputProvider<R extends LayoutResult<?>> {

    /** Return {@code true} if the {@param result} will host any {@link RenderTreeHost}. */
    boolean hasRenderTreeHosts(R result);
  }
}
