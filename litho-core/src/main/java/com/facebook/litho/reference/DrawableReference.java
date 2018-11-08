/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.reference;

import android.content.Context;
import android.graphics.drawable.Drawable;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.litho.drawable.DefaultComparableDrawable;

/**
 * A very simple Reference for {@link Drawable} used in all the cases where it's not
 * possible/desirable to use a real Reference. This will simply keep a reference to the Drawable in
 * the Props and return it. Please take care when using this. It keeps the drawable in memory all
 * the time and should only be used when the other built in specs are not applicable and it's not
 * possible to write a custom ReferenceSpec
 *
 * <p>TODO: rename everything!
 */
public final class DrawableReference extends ReferenceLifecycle<ComparableDrawable> {

  private static DrawableReference sInstance;

  private DrawableReference() {

  }

  public static synchronized DrawableReference get() {
    if (sInstance == null) {
      sInstance = new DrawableReference();
    }
    return sInstance;
  }

  /**
   * @deprecated Use {@link #create(ComparableDrawable)} for efficient diffing
   * @see ComparableDrawable
   */
  @Deprecated
  public static PropsBuilder create() {
    return new PropsBuilder(new State());
  }

  @Override
  protected ComparableDrawable onAcquire(Context context, Reference reference) {
    return ((State) reference).mDrawable;
  }

  /**
   * Utility method to create Comparable Drawable Reference
   *
   * @param drawable the drawable to wrap
   * @return the no-op drawable reference
   */
  public static Reference<ComparableDrawable> create(ComparableDrawable drawable) {
    return new PropsBuilder(new State()).drawable(drawable).build();
  }

  @Override
  protected boolean shouldUpdate(
      Reference<ComparableDrawable> previous, Reference<ComparableDrawable> next) {
    ComparableDrawable previousDrawable = ((State) previous).mDrawable;
    ComparableDrawable nextDrawable = ((State) next).mDrawable;
    return !previousDrawable.isEquivalentTo(nextDrawable);
  }

  private static class State extends Reference<ComparableDrawable> {

    ComparableDrawable mDrawable;

    @Override
    public String getSimpleName() {
      return "DrawableReference";
    }

    @Override
    public int hashCode() {
      return mDrawable != null ? mDrawable.hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof State)) {
        return false;
      }

      State state = (State) o;
      return mDrawable.isEquivalentTo(state.mDrawable);
    }

    protected State() {
      super(get());
    }
  }

  public static class PropsBuilder extends Reference.Builder<ComparableDrawable> {

    private final State mState;

    public PropsBuilder(State state) {
      mState = state;
    }

    public PropsBuilder drawable(ComparableDrawable drawable) {
      mState.mDrawable = drawable;
      return this;
    }

    public PropsBuilder drawable(Drawable drawable) {
      mState.mDrawable = DefaultComparableDrawable.create(drawable);
      return this;
    }

    @Override
    public Reference<ComparableDrawable> build() {
      return mState;
    }
  }
}
