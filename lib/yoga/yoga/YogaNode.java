/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.yoga;

import javax.annotation.Nullable;

import java.util.List;
import java.util.ArrayList;

import com.facebook.proguard.annotations.DoNotStrip;
import com.facebook.soloader.SoLoader;

@DoNotStrip
public class YogaNode implements YogaNodeAPI<YogaNode> {

  static {
    SoLoader.loadLibrary("yoga");
  }

  /**
   * Get native instance count. Useful for testing only.
   */
  static native int jni_YGNodeGetInstanceCount();
  static native void jni_YGLog(int level, String message);

  private static native void jni_YGSetLogger(Object logger);
  public static void setLogger(YogaLogger logger) {
    jni_YGSetLogger(logger);
  }

  private YogaNode mParent;
  private List<YogaNode> mChildren;
  private YogaMeasureFunction mMeasureFunction;
  private YogaBaselineFunction mBaselineFunction;
  private long mNativePointer;
  private Object mData;

  private boolean mHasSetPadding = false;
  private boolean mHasSetMargin = false;
  private boolean mHasSetBorder = false;
  private boolean mHasSetPosition = false;

  @DoNotStrip
  private float mWidth = YogaConstants.UNDEFINED;
  @DoNotStrip
  private float mHeight = YogaConstants.UNDEFINED;
  @DoNotStrip
  private float mTop = YogaConstants.UNDEFINED;
  @DoNotStrip
  private float mLeft = YogaConstants.UNDEFINED;
  @DoNotStrip
  private float mMarginLeft = 0;
  @DoNotStrip
  private float mMarginTop = 0;
  @DoNotStrip
  private float mMarginRight = 0;
  @DoNotStrip
  private float mMarginBottom = 0;
  @DoNotStrip
  private float mPaddingLeft = 0;
  @DoNotStrip
  private float mPaddingTop = 0;
  @DoNotStrip
  private float mPaddingRight = 0;
  @DoNotStrip
  private float mPaddingBottom = 0;
  @DoNotStrip
  private float mBorderLeft = 0;
  @DoNotStrip
  private float mBorderTop = 0;
  @DoNotStrip
  private float mBorderRight = 0;
  @DoNotStrip
  private float mBorderBottom = 0;
  @DoNotStrip
  private int mLayoutDirection = 0;

  private native long jni_YGNodeNew();
  public YogaNode() {
    mNativePointer = jni_YGNodeNew();
    if (mNativePointer == 0) {
      throw new IllegalStateException("Failed to allocate native memory");
    }
  }

  private native long jni_YGNodeNewWithConfig(long configPointer);
  public YogaNode(YogaConfig config) {
    mNativePointer = jni_YGNodeNewWithConfig(config.mNativePointer);
    if (mNativePointer == 0) {
      throw new IllegalStateException("Failed to allocate native memory");
    }
  }

  private native void jni_YGNodeFree(long nativePointer);
  @Override
  protected void finalize() throws Throwable {
    try {
      jni_YGNodeFree(mNativePointer);
    } finally {
      super.finalize();
    }
  }

  private native void jni_YGNodeReset(long nativePointer);
  @Override
  public void reset() {
    mHasSetPadding = false;
    mHasSetMargin = false;
    mHasSetBorder = false;
    mHasSetPosition = false;

    mWidth = YogaConstants.UNDEFINED;
    mHeight = YogaConstants.UNDEFINED;
    mTop = YogaConstants.UNDEFINED;
    mLeft = YogaConstants.UNDEFINED;
    mLayoutDirection = 0;

    mMeasureFunction = null;
    mData = null;

    jni_YGNodeReset(mNativePointer);
  }

  @Override
  public int getChildCount() {
    return mChildren == null ? 0 : mChildren.size();
  }

  @Override
  public YogaNode getChildAt(int i) {
    return mChildren.get(i);
  }

  private native void jni_YGNodeInsertChild(long nativePointer, long childPointer, int index);
  @Override
  public void addChildAt(YogaNode child, int i) {
    if (child.mParent != null) {
      throw new IllegalStateException("Child already has a parent, it must be removed first.");
    }

    if (mChildren == null) {
      mChildren = new ArrayList<>(4);
    }
    mChildren.add(i, child);
    child.mParent = this;
    jni_YGNodeInsertChild(mNativePointer, child.mNativePointer, i);
  }

  private native void jni_YGNodeRemoveChild(long nativePointer, long childPointer);
  @Override
  public YogaNode removeChildAt(int i) {

    final YogaNode child = mChildren.remove(i);
    child.mParent = null;
    jni_YGNodeRemoveChild(mNativePointer, child.mNativePointer);
    return child;
  }

  @Override
  public @Nullable
  YogaNode getParent() {
    return mParent;
  }

  @Override
  public int indexOf(YogaNode child) {
    return mChildren == null ? -1 : mChildren.indexOf(child);
