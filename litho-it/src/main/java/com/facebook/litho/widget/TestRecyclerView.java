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

package com.facebook.litho.widget;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/** A test {@link RecyclerView} class used for unit testing. */
public class TestRecyclerView extends RecyclerView {

  private final List<Runnable> postAnimationRunnableList = new ArrayList<>();

  public TestRecyclerView(Context context) {
    super(context);
  }

  @Override
  public void postOnAnimation(Runnable r) {
    postAnimationRunnableList.add(r);
  }

  public List<Runnable> getPostAnimationRunnableList() {
    return postAnimationRunnableList;
  }
}
