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

package com.facebook.samples.litho.animations.expandableelement;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;

public class Message {

  private final boolean mIsMe;
  private final String mMessage;
  private final boolean mSeen;
  private final String mTimestamp;
  private final boolean mForceAnimateOnAppear;

  public Message(boolean isMe, String message, boolean seen, String timestamp) {
    mIsMe = isMe;
    mMessage = message;
    mSeen = seen;
    mTimestamp = timestamp;
    mForceAnimateOnAppear = false;
  }

  public Message(
      boolean isMe, String message, boolean seen, String timestamp, boolean forceAnimateOnInsert) {
    mIsMe = isMe;
    mMessage = message;
    mSeen = seen;
    mTimestamp = timestamp;
    mForceAnimateOnAppear = forceAnimateOnInsert;
  }

  public RenderInfo createComponent(ComponentContext c) {
    final Component component =
        mIsMe
            ? ExpandableElementMe.create(c)
                .messageText(mMessage)
                .timestamp(mTimestamp)
                .seen(mSeen)
                .forceAnimateOnAppear(mForceAnimateOnAppear)
                .build()
            : ExpandableElementOther.create(c)
                .messageText(mMessage)
                .timestamp(mTimestamp)
                .seen(mSeen)
                .build();
    return ComponentRenderInfo.create().component(component).build();
  }
}
