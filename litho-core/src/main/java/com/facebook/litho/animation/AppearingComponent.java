// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * Class used to reference a component that is appearing in a transition.
 */
public class AppearingComponent extends AnimatedComponent {

  public AppearingComponent(String key) {
    super(key);
  }

  @Override
  public AppearingDimensionComponentProperty x() {
    return new AppearingDimensionComponentProperty(this, AnimatedProperties.X);
  }

  @Override
  public AppearingDimensionComponentProperty y() {
    return new AppearingDimensionComponentProperty(this, AnimatedProperties.Y);
  }

  @Override
  public AppearingDimensionComponentProperty width() {
    return new AppearingDimensionComponentProperty(this, AnimatedProperties.WIDTH);
  }

  @Override
  public AppearingDimensionComponentProperty height() {
    return new AppearingDimensionComponentProperty(this, AnimatedProperties.HEIGHT);
  }

  @Override
  public AppearingFloatComponentProperty scale() {
    return new AppearingFloatComponentProperty(this, AnimatedProperties.SCALE);
  }

  @Override
  public AppearingFloatComponentProperty alpha() {
    return new AppearingFloatComponentProperty(this, AnimatedProperties.ALPHA);
  }
}
