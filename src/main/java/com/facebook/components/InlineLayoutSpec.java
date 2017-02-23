// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

public abstract class InlineLayoutSpec extends Component implements Cloneable {

  private static class Lifecycle extends ComponentLifecycle {

    @Override
    protected ComponentLayout onCreateLayout(ComponentContext c, Component<?> component) {
      return ((InlineLayoutSpec) component).onCreateLayout(c);
    }

    @Override
    public Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
      // no-op
      return null;
    }
  }

  public InlineLayoutSpec() {
    super(new Lifecycle());
  }

  @Override
  public String getSimpleName() {
    // You may want to override this in your inline spec, but it's not required.
    return "InlineLayout";
  }

  protected abstract ComponentLayout onCreateLayout(ComponentContext c);
}
