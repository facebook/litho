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

/**
 * A callback invoked when the data for a section has been dispatched to the underlying
 * RecyclerView. This can happen after setRoot is called because the setRoot may be async, or we may
 * wait for layouts to complete before inserting into the adapter.
 */
public interface ChangeSetCompleteCallback {

  /** A callback invoked when the data for a section has changed to the underlying RecyclerView. */
  void onDataBound();

  /** A callback invoked when the data for a section has rendered to the underlying RecyclerView. */
  void onDataRendered(boolean isMounted, long uptimeMillis);
}
