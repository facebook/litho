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

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;

/**
 * Litho's implementation of the {@link AnimatableItem} required by the {@link TransitionsExtension}
 * to power animations. This object should NOT be used to inform the should update during mounting,
 * therefore it should NOT be used to host any such information.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LithoAnimtableItem implements AnimatableItem {

  private final long mId;
  private final Rect mAbsoluteBounds;
  private final @OutputUnitType int mOutputType;
  private final @Nullable NodeInfo mNodeInfo;
  private final @Nullable TransitionId mTransitionId;

  public LithoAnimtableItem(
      final long id,
      final Rect absoluteBounds,
      final @OutputUnitType int type,
      final @Nullable NodeInfo nodeInfo,
      final @Nullable TransitionId transitionId) {
    mId = id;
    mAbsoluteBounds = absoluteBounds;
    mOutputType = type;
    mNodeInfo = nodeInfo;
    mTransitionId = transitionId;
  }

  @Override
  public long getId() {
    return mId;
  }

  @Override
  public Rect getAbsoluteBounds() {
    return mAbsoluteBounds;
  }

  @Override
  public int getOutputType() {
    return mOutputType;
  }

  @Override
  public @Nullable TransitionId getTransitionId() {
    return mTransitionId;
  }

  @Override
  public float getScale() {
    return mNodeInfo != null ? mNodeInfo.getScale() : 1;
  }

  @Override
  public float getAlpha() {
    return mNodeInfo != null ? mNodeInfo.getAlpha() : 1;
  }

  @Override
  public float getRotation() {
    return mNodeInfo != null ? mNodeInfo.getRotation() : 0;
  }

  @Override
  public float getRotationX() {
    return mNodeInfo != null ? mNodeInfo.getRotationX() : 0;
  }

  @Override
  public float getRotationY() {
    return mNodeInfo != null ? mNodeInfo.getRotationY() : 0;
  }

  @Override
  public boolean isScaleSet() {
    return mNodeInfo != null && mNodeInfo.isScaleSet();
  }

  @Override
  public boolean isAlphaSet() {
    return mNodeInfo != null && mNodeInfo.isAlphaSet();
  }

  @Override
  public boolean isRotationSet() {
    return mNodeInfo != null && mNodeInfo.isRotationSet();
  }

  @Override
  public boolean isRotationXSet() {
    return mNodeInfo != null && mNodeInfo.isRotationXSet();
  }

  @Override
  public boolean isRotationYSet() {
    return mNodeInfo != null && mNodeInfo.isRotationYSet();
  }
}
