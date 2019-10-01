/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
