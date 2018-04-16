/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.sections.widget;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.widget.Binder;
import javax.annotation.Nullable;

/**
 * A configuration object the {@link RecyclerCollectionComponent} will use to determine which layout
 * manager should be used for the {@link RecyclerView}
 */
public interface RecyclerConfiguration {

  <E extends Binder<RecyclerView> & SectionTree.Target> E buildTarget(ComponentContext c);

  @Nullable
  SnapHelper getSnapHelper();

  int getOrientation();

  boolean isWrapContent();
}
