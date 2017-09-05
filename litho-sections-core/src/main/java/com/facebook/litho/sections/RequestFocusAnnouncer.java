/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import android.support.annotation.Nullable;

/**
 * Pass this down as a {@link com.facebook.litho.annotations.Prop} and use it in your implementation
 * of {@link com.facebook.litho.sections.annotations.OnDataBound} to bind the focuser when you want to
 * trigger a requestFocus() from outside the Sections hierarchy.
 *
 * Example usage:
 *
 * class ExampleSectionSpec {
 *
 *  @OnDataBound
 *  void onDataBound(SectionContext c, @Prop RequestFocusAnnouncer<T> focusAnnouncer) {
 *    focusAnnouncer.setFocuser(new Focuser<T>() {
 *
 *      void requestFocus(T object) {
 *        int indexOfObject = findObjectInData(object);
 *        ExampleSection.requestFocus(c, indexOfObject);
 *      }
 *    });
 *  }
 * }
 */
public class RequestFocusAnnouncer<T> {

  private @Nullable T mFocusObject;
  private @Nullable Focuser mFocuser;

  public interface Focuser<T> {
    void requestFocus(T focusObject);
  }

  public void requestFocus(T focusObject) {
    mFocusObject = focusObject;

    if (mFocuser != null && focusObject != null) {
      mFocuser.requestFocus(focusObject);
      mFocusObject = null;
    }
  }

  public void setFocuser(Focuser focuser) {
    if (focuser == null) {
      return;
    }

    mFocuser = focuser;
    if (mFocusObject != null) {
      requestFocus(mFocusObject);
    }
  }
}
