// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.reference;

import android.graphics.drawable.Drawable;

import com.facebook.components.ComponentContext;

/**
 * A very simple Reference for {@link Drawable} used in all the cases where it's not
 * possible/desirable to use a real Reference. This will simply keep a reference to the Drawable
 * in the Props and return it. Please take care when using this. It keeps the drawable in memory
 * all the time and should only be used when the other built in specs are not applicable and
 * it's not possible to write a custom ReferenceSpec
 */
public final class DrawableReference extends ReferenceLifecycle<Drawable> {

  private static DrawableReference sInstance;

  private DrawableReference() {

  }

  public static synchronized DrawableReference get() {
    if (sInstance == null) {
      sInstance = new DrawableReference();
    }
    return sInstance;
  }

  public static PropsBuilder create() {
    return new PropsBuilder(new State());
  }

  @Override
  protected Drawable onAcquire(
      ComponentContext context,
      Reference reference) {
    return ((State) reference).mDrawable;
  }

  private static class State extends Reference<Drawable> {

    Drawable mDrawable;

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

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      State state = (State) o;
      return DrawableUtils.areDrawablesEqual(mDrawable, state.mDrawable);
    }

    protected State() {
      super(get());
    }
  }

  public static class PropsBuilder extends Reference.Builder<Drawable> {

    private State mState;

    public PropsBuilder(State state) {
      mState = state;
    }

    public PropsBuilder drawable(Drawable drawable) {
      mState.mDrawable = drawable;
      return this;
    }

    @Override
    public Reference<Drawable> build() {
      return mState;
    }
  }
}
