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

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Node.LayoutResult;
import com.facebook.rendercore.extensions.LayoutResultVisitor;
import com.facebook.rendercore.testing.TestLayoutResultVisitor.Result;
import java.util.List;

public class TestLayoutResultVisitor implements LayoutResultVisitor<List<Result>> {

  @Override
  public void visit(
      final LayoutResult<?> result,
      final Rect bounds,
      final int x,
      final int y,
      final @Nullable List<Result> results) {
    if (results != null) {
      results.add(new Result(result, bounds, x, y));
    }
  }

  public static class Result {
    public final LayoutResult result;
    public final Rect bounds;
    public final int x;
    public final int y;

    public Result(LayoutResult<?> result, Rect bounds, int x, int y) {
      this.result = result;
      this.bounds = bounds;
      this.x = x;
      this.y = y;
    }
  }
}
