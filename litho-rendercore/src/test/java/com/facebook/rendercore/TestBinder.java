// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import android.content.Context;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestBinder<MODEL> implements RenderUnit.Binder<MODEL, View, Void> {

  public static class TestBinder1 extends TestBinder<RenderUnit> {
    public TestBinder1() {
      super();
    }

    public TestBinder1(List bindOrder, List unbindOrder) {
      super(bindOrder, unbindOrder);
    }
  }

  public static class TestBinder2 extends TestBinder<RenderUnit> {
    public TestBinder2() {
      super();
    }

    public TestBinder2(List bindOrder, List unbindOrder) {
      super(bindOrder, unbindOrder);
    }
  }

  public static class TestBinder3 extends TestBinder<RenderUnit> {
    public TestBinder3() {
      super();
    }

    public TestBinder3(List bindOrder, List unbindOrder) {
      super(bindOrder, unbindOrder);
    }
  }

  private final List bindOrder;
  private final List unbindOrder;
  boolean wasBound = false;
  boolean wasUnbound = false;

  public TestBinder() {
    this.bindOrder = new ArrayList<>();
    this.unbindOrder = new ArrayList<>();
  }

  public TestBinder(List bindOrder, List unbindOrder) {
    this.bindOrder = bindOrder;
    this.unbindOrder = unbindOrder;
  }

  @Override
  public boolean shouldUpdate(
      MODEL currentValue, MODEL newValue, Object currentLayoutData, Object nextLayoutData) {
    return !Objects.equals(currentLayoutData, nextLayoutData);
  }

  @Override
  public Void bind(Context context, View view, MODEL model, Object layoutData) {
    bindOrder.add(this);
    wasBound = true;
    return null;
  }

  @Override
  public void unbind(Context context, View view, MODEL model, Object layoutData, Void bindData) {
    unbindOrder.add(this);
    wasUnbound = true;
  }
}
