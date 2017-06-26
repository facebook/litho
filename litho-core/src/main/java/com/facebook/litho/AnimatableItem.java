package com.facebook.litho;

import android.graphics.Rect;

/**
 * Interface used to expose a limited API of {@link LayoutOutput} to the animations package.
 */
public interface AnimatableItem {

  Rect getBounds();
}
