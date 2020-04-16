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

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;

import com.facebook.litho.SizeSpec.MeasureSpecMode;
import org.junit.Before;
import org.robolectric.RuntimeEnvironment;

public abstract class BaseLithoComponentTest {

  protected static final int sDefaultWidth = 1080;
  protected static final int sDefaultHeight = 1920;
  protected static final @MeasureSpecMode int sDefaultWidthMode = EXACTLY;
  protected static final @MeasureSpecMode int sDefaultHeightMode = EXACTLY;
  protected static final boolean sDefaultShouldAttachBeforeTest = true;
  protected static final boolean sDefaultShouldMeasureBeforeTest = true;
  protected static final boolean sDefaultShouldLayoutBeforeTest = true;

  protected ComponentContext mContext;
  protected ComponentTree mComponentTree;
  protected LithoView mLithoView;

  protected boolean mShouldAttachBeforeTest = sDefaultShouldAttachBeforeTest;
  protected boolean mShouldMeasureBeforeTest = sDefaultShouldMeasureBeforeTest;
  protected boolean mShouldLayoutBeforeTest = sDefaultShouldLayoutBeforeTest;

  protected int mWidth = sDefaultWidth;
  protected int mHeight = sDefaultHeight;
  protected @MeasureSpecMode int mWidthMode = sDefaultWidthMode;
  protected @MeasureSpecMode int mHeightMode = sDefaultHeightMode;

  @Before
  public void before() {
    overrideDefaults();

    // TODO: (T65557912) Use a Context from an Activity in BaseLithoComponentTest.
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mLithoView = new LithoView(mContext);
    mComponentTree = ComponentTree.create(mContext).build();
    mLithoView.setComponentTree(mComponentTree);
    if (mShouldAttachBeforeTest) {
      attachLithoView();
    }
    if (mShouldMeasureBeforeTest) {
      measureLithoView();
    }
    if (mShouldLayoutBeforeTest) {
      layoutLithoView();
    }
  }

  /**
   * Override the default values for:
   *
   * <ul>
   *   <li>{@link #mWidth}
   *   <li>{@link #mHeight}
   *   <li>{@link #mWidthMode}
   *   <li>{@link #mHeightMode}
   *   <li>{@link #mShouldAttachBeforeTest}
   *   <li>{@link #mShouldMeasureBeforeTest}
   *   <li>{@link #mShouldLayoutBeforeTest}
   * </ul>
   */
  protected abstract void overrideDefaults();

  protected void setRoot(Component component) {
    mLithoView.setComponent(component);
  }

  protected void setRootAsync(Component component) {
    mComponentTree.setRootAsync(component);
  }

  protected void attachLithoView() {
    mLithoView.onAttachedToWindow();
  }

  protected void detachLithoView() {
    mLithoView.onDetachedFromWindow();
  }

  protected void measureLithoView() {
    measureLithoViewWithSize(getWidthSpec(), getHeightSpec());
  }

  protected void measureLithoViewWithSize(int width, int height) {
    measureLithoViewWithSizeSpec(makeSizeSpec(width, EXACTLY), makeSizeSpec(height, EXACTLY));
  }

  protected void measureLithoViewWithSizeSpec(int widthSpec, int heightSpec) {
    mLithoView.measure(widthSpec, heightSpec);
  }

  protected void layoutLithoView() {
    mLithoView.layout(0, 0, mLithoView.getMeasuredWidth(), mLithoView.getMeasuredHeight());
  }

  protected int getWidthSpec() {
    return makeSizeSpec(mWidth, mWidthMode);
  }

  protected int getHeightSpec() {
    return makeSizeSpec(mHeight, mHeightMode);
  }
}
