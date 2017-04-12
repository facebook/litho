// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * Class used to reference a component that is disappearing in a transition.
 */
public class DisappearingComponent extends AnimatedComponent {

  public DisappearingComponent(String key) {
    super(key);
  }

  @Override
  public DisappearingDimensionComponentProperty x() {
    return new DisappearingDimensionComponentProperty(this, AnimatedProperties.X);
  }

  @Override
  public DisappearingDimensionComponentProperty y() {
    return new DisappearingDimensionComponentProperty(this, AnimatedProperties.Y);
  }

  @Override
  public DisappearingDimensionComponentProperty width() {
    return new DisappearingDimensionComponentProperty(this, AnimatedProperties.WIDTH);
  }

  @Override
  public DisappearingDimensionComponentProperty height() {
    return new DisappearingDimensionComponentProperty(this, AnimatedProperties.HEIGHT);
  }

  @Override
  public DisappearingPositionComponentProperty xy() {
    return new DisappearingPositionComponentProperty(this, x(), y());
  }

  public DisappearingFloatComponentProperty scale() {
    return new DisappearingFloatComponentProperty(this, AnimatedProperties.SCALE);
  }

  @Override
  public DisappearingFloatComponentProperty alpha() {
    return new DisappearingFloatComponentProperty(this, AnimatedProperties.ALPHA);
  }
}
