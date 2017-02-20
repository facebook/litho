// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.viewcompatcreator;

import android.content.Context;
import android.view.View;

/**
 * Creates a View of the specified type. Used as the mount content for ViewCompatComponent.
 * Views created with the same {@link ViewCompatCreator} will be recycled across instances of
 * ViewCompatComponent.
 * @param <V> the type of View to create.
 */
public interface ViewCompatCreator<V extends View> {

  /**
   * @param c android Context.
   * @return a new view of type V.
   */
  V createView(Context c);
}
