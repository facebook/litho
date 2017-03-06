// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

/**
 * An interface for {@link SpecModel}s that can be pure render.
 */
public interface HasPureRender {

  /**
   * Whether this spec is pure render or not. If a spec is pure render then when the
   * ComponentTree for this component is updated, if nothing changes then the measurements for this
   * component can be re-used.
   */
  boolean isPureRender();
}
