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

package com.facebook.litho;

import com.facebook.litho.annotations.Event;

/**
 * An event used internally to propagate exceptions up the hierarchy. Don't use this directly. Use
 * {@link com.facebook.litho.annotations.OnError} instead.
 */
@Event
public class ErrorEvent {
  /** The {@link ComponentTree} the the error happened in. */
  public ComponentTree componentTree;

  /** The exception that caused the error event to be raised. */
  public Exception exception;
}
