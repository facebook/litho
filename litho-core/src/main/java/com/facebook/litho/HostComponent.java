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

import android.content.Context;
import android.os.Build;
import android.util.SparseArray;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
class HostComponent extends Component {

  /**
   * We duplicate mComponentDynamicProps here, in order to provide {@link
   * #setCommonDynamicProps(SparseArray)} to HostComponent only, which is used in LayoutState to
   * pass Common Dynamic Props from other Components that do not mount a view
   */
  @Nullable private SparseArray<DynamicValue<?>> mCommonDynamicProps;

  private boolean mImplementsVirtualViews = false;

  protected HostComponent() {}

  @Override
  protected MountContentPool onCreateMountContentPool() {
    return new DisabledMountContentPool();
  }

  @Override
  protected Object onCreateMountContent(Context c) {
    return new ComponentHost(c);
  }

  @Override
  protected void onMount(ComponentContext c, Object convertContent) {
    final ComponentHost host = (ComponentHost) convertContent;

    if (Build.VERSION.SDK_INT >= 11) {
      // We need to do this in case an external user of this ComponentHost has manually set alpha
      // to 0, which will mean that it won't draw anything.
      host.setAlpha(1.0f);
    }

    host.setImplementsVirtualViews(mImplementsVirtualViews);
  }

  @Override
  protected void onUnmount(ComponentContext c, Object mountedContent) {
    final ComponentHost host = (ComponentHost) mountedContent;

    // Some hosts might be duplicating parent state which could be 'pressed' and under certain
    // conditions that state might not be cleared from this host and carried to next reuse,
    // therefore applying wrong drawable state. Particular case where this might happen is when
    // host is unmounted as soon as click event is triggered, and host is unmounted before it has
    // chance to reset its internal pressed state.
    if (host.isPressed()) {
      host.setPressed(false);
    }

    host.setImplementsVirtualViews(false);
  }

  @Override
  protected void onBind(ComponentContext c, Object mountedContent) {
    final ComponentHost host = (ComponentHost) mountedContent;
    host.maybeInvalidateAccessibilityState();
  }

  @Override
  public MountType getMountType() {
    return MountType.VIEW;
  }

  static HostComponent create() {
    return new HostComponent();
  }

  @Override
  public boolean isEquivalentTo(@Nullable Component other) {
    return this == other;
  }

  @Override
  protected int poolSize() {
    return 45;
  }

  @Override
  protected boolean shouldUpdate(
      final @Nullable Component previous,
      final @Nullable StateContainer previousStateContainer,
      final @Nullable Component next,
      final @Nullable StateContainer nextStateContainer) {
    return true;
  }

  @Nullable
  @Override
  SparseArray<DynamicValue<?>> getCommonDynamicProps() {
    return mCommonDynamicProps;
  }

  @Override
  boolean hasCommonDynamicProps() {
    return mCommonDynamicProps != null && mCommonDynamicProps.size() > 0;
  }

  @Override
  public String getSimpleName() {
    return "HostComponent";
  }

  /**
   * Sets common dynamic Props. Used in {@link LayoutState} to pass dynamic props from a component,
   * to the host, that's wrapping it
   *
   * @param commonDynamicProps common dynamic props to set.
   * @see LayoutState#createHostLayoutOutput(LayoutState, InternalNode, boolean)
   */
  void setCommonDynamicProps(SparseArray<DynamicValue<?>> commonDynamicProps) {
    mCommonDynamicProps = commonDynamicProps;
  }

  void setImplementsVirtualViews() {
    mImplementsVirtualViews = true;
  }
}
