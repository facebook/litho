/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.rendercore;

import android.content.Context;
import android.view.View;
import androidx.core.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestBinderWithBindData<MODEL> implements RenderUnit.Binder<MODEL, View, Object> {

  public static class TestBinderWithBindData1 extends TestBinderWithBindData<RenderUnit> {
    public TestBinderWithBindData1() {
      super();
    }

    public TestBinderWithBindData1(List bindOrder, List unbindOrder) {
      super(bindOrder, unbindOrder);
    }

    public TestBinderWithBindData1(Object bindData) {
      super(bindData);
    }

    public TestBinderWithBindData1(List bindOrder, List unbindOrder, Object bindData) {
      super(bindOrder, unbindOrder, bindData);
    }
  }

  public static class TestBinderWithBindData2 extends TestBinderWithBindData<RenderUnit> {
    public TestBinderWithBindData2() {
      super();
    }

    public TestBinderWithBindData2(List bindOrder, List unbindOrder) {
      super(bindOrder, unbindOrder);
    }

    public TestBinderWithBindData2(Object bindData) {
      super(bindData);
    }

    public TestBinderWithBindData2(List bindOrder, List unbindOrder, Object bindData) {
      super(bindOrder, unbindOrder, bindData);
    }
  }

  public static class TestBinderWithBindData3 extends TestBinderWithBindData<RenderUnit> {
    public TestBinderWithBindData3() {
      super();
    }

    public TestBinderWithBindData3(List bindOrder, List unbindOrder) {
      super(bindOrder, unbindOrder);
    }

    public TestBinderWithBindData3(Object bindData) {
      super(bindData);
    }

    public TestBinderWithBindData3(List bindOrder, List unbindOrder, Object bindData) {
      super(bindOrder, unbindOrder, bindData);
    }
  }

  public static class TestBinderWithBindData4 extends TestBinderWithBindData<RenderUnit> {
    public TestBinderWithBindData4() {
      super();
    }

    public TestBinderWithBindData4(List bindOrder, List unbindOrder) {
      super(bindOrder, unbindOrder);
    }

    public TestBinderWithBindData4(Object bindData) {
      super(bindData);
    }

    public TestBinderWithBindData4(List bindOrder, List unbindOrder, Object bindData) {
      super(bindOrder, unbindOrder, bindData);
    }
  }

  private final List bindOrder;
  private final List unbindOrder;
  private final Object bindData;
  boolean wasBound = false;
  boolean wasUnbound = false;

  public TestBinderWithBindData() {
    this.bindOrder = new ArrayList<>();
    this.unbindOrder = new ArrayList<>();
    this.bindData = new Object();
  }

  public TestBinderWithBindData(Object bindData) {
    this.bindOrder = new ArrayList();
    this.unbindOrder = new ArrayList();
    this.bindData = bindData;
  }

  public TestBinderWithBindData(List bindOrder, List unbindOrder) {
    this.bindOrder = bindOrder;
    this.unbindOrder = unbindOrder;
    this.bindData = new Object();
  }

  public TestBinderWithBindData(List bindOrder, List unbindOrder, Object bindData) {
    this.bindOrder = bindOrder;
    this.unbindOrder = unbindOrder;
    this.bindData = bindData;
  }

  @Override
  public boolean shouldUpdate(
      MODEL currentValue, MODEL newValue, Object currentLayoutData, Object nextLayoutData) {
    return !Objects.equals(currentLayoutData, nextLayoutData);
  }

  @Override
  public Object bind(Context context, View view, MODEL model, Object layoutData) {
    bindOrder.add(new Pair(this, bindData));
    wasBound = true;
    return bindData;
  }

  @Override
  public void unbind(Context context, View view, MODEL model, Object layoutData, Object bindData) {
    unbindOrder.add(new Pair(this, bindData));
    wasUnbound = true;
  }
}
