// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import android.content.Context;
import android.view.View;
import java.util.Collections;
import java.util.List;

class TestRenderUnit extends RenderUnit<View> implements ContentAllocator<View> {

  public TestRenderUnit() {
    super(RenderType.VIEW);
  }

  public TestRenderUnit(List<DelegateBinder<?, ? super View, ?>> fixedMountBinders) {
    super(RenderType.VIEW, fixedMountBinders, Collections.emptyList(), Collections.emptyList());
  }

  public TestRenderUnit(
      List<DelegateBinder<?, ? super View, ?>> fixedMountBinders,
      List<DelegateBinder<?, ? super View, ?>> optionalMountBinders,
      List<DelegateBinder<?, ? super View, ?>> attachBinder) {
    super(RenderType.VIEW, fixedMountBinders, optionalMountBinders, attachBinder);
  }

  @Override
  public View createContent(Context c) {
    return new View(c);
  }

  @Override
  public ContentAllocator<View> getContentAllocator() {
    return this;
  }

  @Override
  public long getId() {
    return 0;
  }
}
