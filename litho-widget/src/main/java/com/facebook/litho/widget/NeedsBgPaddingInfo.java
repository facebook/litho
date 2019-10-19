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

import android.graphics.Rect;

/**
 * Because a LayoutInfo/LayoutManager may rely on padding to generate a child measure specs, and
 * because padding isn't accessible in a thread-safe way, this interface gives a LayoutManager the
 * chance to record the RecyclerView's padding when it's attached.
 *
 * <p>This is a bit of a hack and is best-effort only -- the primary goal is to prevent accessing
 * View padding off the main thread since it can crash. If RV padding changes after this padding
 * info is set, the bg padding info will NOT be up-to-date. For most usecases, this shouldn't
 * matter.
 */
public interface NeedsBgPaddingInfo {
  void setBgPaddingInfo(Rect paddingInfo);
}
