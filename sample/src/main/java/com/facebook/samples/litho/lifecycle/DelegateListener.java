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

package com.facebook.samples.litho.lifecycle;

interface DelegateListener {
  int ON_CREATE_INITIAL_STATE = 0;
  int ON_CREATE_TREE_PROP = 1;
  int ON_CREATE_LAYOUT = 2;
  int ON_CREATE_TRANSITION = 3;
  int ON_ATTACHED = 4;
  int ON_DETACHED = 5;
  int ON_VISIBLE = 6;
  int ON_INVISIBLE = 7;

  void onDelegateMethodCalled(int type, Thread thread, long timestamp, int id);

  void setRootComponent(boolean isSync);
}
