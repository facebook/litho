// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

/**
 * Reference to a component that has one or more properties changing in a transition.
 */
public class ChangingComponent extends AnimatedComponent {

  public ChangingComponent(String key) {
    super(key);
  }

  @Override
  public DimensionComponentProperty x() {
    return new DimensionComponentProperty(this, AnimatedProperties.X);
  }

  @Override
  public DimensionComponentProperty y() {
    return new DimensionComponentProperty(this, AnimatedProperties.Y);
  }

  @Override
  public DimensionComponentProperty width() {
    return new DimensionComponentProperty(this, AnimatedProperties.WIDTH);
  }

  @Override
  public DimensionComponentProperty height() {
    return new DimensionComponentProperty(this, AnimatedProperties.HEIGHT);
  }

  public PositionComponentProperty xy() {
    return new PositionComponentProperty(this, x(), y());
  }

  @Override
  public FloatComponentProperty scale() {
    return new FloatComponentProperty(this, AnimatedProperties.SCALE);
  }

  @Override
  public FloatComponentProperty alpha() {
    return new FloatComponentProperty(this, AnimatedProperties.ALPHA);
  }
}
