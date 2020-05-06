/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.rendercore.testing;

import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.rendercore.RenderUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class TestRenderUnit extends RenderUnit {

  private static AtomicLong sIdGenerator = new AtomicLong();

  private long mId;

  public TestRenderUnit(
      final @Nullable List<? extends RenderUnit.Binder> mountBinders,
      final @Nullable List<? extends RenderUnit.Binder> attachBinders) {
    super(RenderType.VIEW, mountBinders, attachBinders);
    mId = sIdGenerator.incrementAndGet();
  }

  public TestRenderUnit() {
    super(RenderType.VIEW);
    mId = sIdGenerator.incrementAndGet();
  }

  public TestRenderUnit(RenderUnit.Binder... attachDetachFunctions) {
    super(RenderType.VIEW, new ArrayList<>(), Arrays.asList(attachDetachFunctions));
    mId = sIdGenerator.incrementAndGet();
  }

  @Override
  public Object createContent(Context c) {
    return new View(c);
  }

  @Override
  public long getId() {
    return mId;
  }

  public void setId(long id) {
    mId = id;
  }
}
