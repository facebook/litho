/*
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

package com.facebook.rendercore.renderunits;

import static com.facebook.rendercore.RenderUnit.Extension.extension;
import static com.facebook.rendercore.RenderUnit.RenderType.VIEW;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.facebook.rendercore.HostView;
import com.facebook.rendercore.InterceptTouchHandler;
import com.facebook.rendercore.RenderUnit;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class HostRenderUnit extends RenderUnit<HostView> {

  private static final int UNSET = -1;
  private static final int SET_FALSE = 0;
  private static final int SET_TRUE = 1;

  @IntDef({UNSET, SET_FALSE, SET_TRUE})
  @Retention(RetentionPolicy.SOURCE)
  @interface TriState {}

  private long mId;
  private @Nullable Drawable mBackground;
  private @Nullable Drawable mForeground;
  private int mLayerType = View.LAYER_TYPE_NONE;
  private @TriState int mClickable = UNSET;
  private boolean mEnabled = true;
  private boolean mFocusable;
  private boolean mFocusableInTouchMode;
  private OnFocusChangeListener mOnFocusChangeListener;
  private InterceptTouchHandler mInterceptTouchHandler;
  private OnLongClickListener mLongClickListener;
  private OnClickListener mClickListener;
  private OnTouchListener mOnTouchListener;

  public HostRenderUnit(long id) {
    super(VIEW);
    mId = id;
    addMountUnmountExtensions(
        extension(this, sBackgroundBindFunction),
        extension(this, sForegroundBindFunction),
        extension(this, sTouchHandlersBindFunction),
        extension(this, sLayerTypeBindFunction));
  }

  @Override
  public HostView createContent(Context c) {
    return new HostView(c);
  }

  @Override
  public long getId() {
    return mId;
  }

  @Nullable
  public Drawable getBackground() {
    return mBackground;
  }

  @Nullable
  public Drawable getForeground() {
    return mForeground;
  }

  public int getLayerType() {
    return mLayerType;
  }

  public @TriState int getClickable() {
    return mClickable;
  }

  public boolean isEnabled() {
    return mEnabled;
  }

  public OnTouchListener getOnTouchListener() {
    return mOnTouchListener;
  }

  public OnClickListener getOnClickListener() {
    return mClickListener;
  }

  public OnLongClickListener getOnLongClickListener() {
    return mLongClickListener;
  }

  public InterceptTouchHandler getOnInterceptTouchEvent() {
    return mInterceptTouchHandler;
  }

  public OnFocusChangeListener getOnFocusChangeListener() {
    return mOnFocusChangeListener;
  }

  public boolean isFocusable() {
    return mFocusable;
  }

  public boolean isFocusableInTouchMode() {
    return mFocusableInTouchMode;
  }

  public void setBackground(Drawable background) {
    mBackground = background;
  }

  public void setForeground(Drawable foreground) {
    mForeground = foreground;
  }

  public void setLayerType(int layerType) {
    mLayerType = layerType;
  }

  public void setClickable(boolean clickable) {
    mClickable = clickable ? SET_TRUE : SET_FALSE;
  }

  public void setEnabled(boolean enabled) {
    mEnabled = enabled;
  }

  public void setFocusable(boolean focusable) {
    mFocusable = focusable;
  }

  public void setFocusableInTouchMode(boolean focusableInTouchMode) {
    mFocusableInTouchMode = focusableInTouchMode;
  }

  public void setOnFocusChangeListener(OnFocusChangeListener onFocusChangeListener) {
    mOnFocusChangeListener = onFocusChangeListener;
  }

  public void setInterceptTouchHandler(InterceptTouchHandler interceptTouchHandler) {
    mInterceptTouchHandler = interceptTouchHandler;
  }

  public void setLongClickListener(OnLongClickListener longClickListener) {
    mLongClickListener = longClickListener;
  }

  public void setClickListener(OnClickListener clickListener) {
    mClickListener = clickListener;
  }

  public void setOnTouchListener(OnTouchListener onTouchListener) {
    mOnTouchListener = onTouchListener;
  }

  public static Binder<HostRenderUnit, HostView> sBackgroundBindFunction =
      new Binder<HostRenderUnit, HostView>() {
        @Override
        public boolean shouldUpdate(
            HostRenderUnit currentValue,
            HostRenderUnit newValue,
            Object currentLayoutData,
            Object nextLayoutData) {
          final Drawable currentBackground = currentValue.mBackground;
          final Drawable newBackground = newValue.mBackground;
          if (currentBackground == null) {
            return newBackground != null;
          } else {
            return newBackground != null && !currentBackground.equals(newBackground);
          }
        }

        @Override
        public void bind(
            Context context, HostView hostView, HostRenderUnit hostRenderUnit, Object layoutData) {
          hostView.setBackground(hostRenderUnit.getBackground());
        }

        @Override
        public void unbind(
            Context context, HostView hostView, HostRenderUnit hostRenderUnit, Object layoutData) {
          hostView.setBackground(null);
        }
      };

  public static Binder<HostRenderUnit, HostView> sForegroundBindFunction =
      new Binder<HostRenderUnit, HostView>() {
        @Override
        public boolean shouldUpdate(
            HostRenderUnit currentValue,
            HostRenderUnit newValue,
            Object currentLayoutData,
            Object nextLayoutData) {
          final Drawable currentForeground = currentValue.mForeground;
          final Drawable newForeground = newValue.mForeground;
          if (currentForeground == null) {
            return newForeground != null;
          } else {
            return newForeground != null && !currentForeground.equals(newForeground);
          }
        }

        @Override
        public void bind(
            Context context, HostView hostView, HostRenderUnit hostRenderUnit, Object layoutData) {
          hostView.setForegroundCompat(hostRenderUnit.getForeground());
        }

        @Override
        public void unbind(
            Context context, HostView hostView, HostRenderUnit hostRenderUnit, Object layoutData) {
          hostView.setForegroundCompat(null);
        }
      };

  public static Binder<HostRenderUnit, HostView> sLayerTypeBindFunction =
      new Binder<HostRenderUnit, HostView>() {

        @Override
        public boolean shouldUpdate(
            HostRenderUnit currentModel,
            HostRenderUnit newModel,
            @Nullable Object currentLayoutData,
            @Nullable Object nextLayoutData) {
          return currentModel.mLayerType != newModel.mLayerType;
        }

        @Override
        public void bind(
            Context context,
            HostView hostView,
            HostRenderUnit hostRenderUnit,
            @Nullable Object layoutData) {
          hostView.setLayerType(hostRenderUnit.getLayerType(), null);
        }

        @Override
        public void unbind(
            Context context,
            HostView hostView,
            HostRenderUnit hostRenderUnit,
            @Nullable Object layoutData) {
          hostView.setLayerType(View.LAYER_TYPE_NONE, null);
        }
      };

  public static Binder<HostRenderUnit, HostView> sTouchHandlersBindFunction =
      new Binder<HostRenderUnit, HostView>() {
        @Override
        public boolean shouldUpdate(
            HostRenderUnit currentValue,
            HostRenderUnit newValue,
            Object currentLayoutData,
            Object nextLayoutData) {
          // Updating touch and click listeners is not an expensive operation.
          return true;
        }

        @Override
        public void bind(
            Context context, HostView hostView, HostRenderUnit hostRenderUnit, Object layoutData) {
          hostView.setOnTouchListener(hostRenderUnit.getOnTouchListener());
          hostView.setInterceptTouchEventHandler(hostRenderUnit.getOnInterceptTouchEvent());
          final OnClickListener onClickListener = hostRenderUnit.getOnClickListener();
          if (onClickListener != null) {
            hostView.setOnClickListener(onClickListener);
          }

          final OnLongClickListener onLongClickListener = hostRenderUnit.getOnLongClickListener();
          if (onLongClickListener != null) {
            hostView.setOnLongClickListener(onLongClickListener);
          }

          OnFocusChangeListener onFocusChangeListener = hostRenderUnit.getOnFocusChangeListener();
          hostView.setOnFocusChangeListener(onFocusChangeListener);
          hostView.setFocusable(hostRenderUnit.isFocusable());
          hostView.setFocusableInTouchMode(hostRenderUnit.isFocusableInTouchMode());
          hostView.setEnabled(hostRenderUnit.isEnabled());

          final @TriState int clickable = hostRenderUnit.getClickable();
          if (clickable != UNSET) {
            hostView.setClickable(clickable == SET_TRUE);
          }
        }

        @Override
        public void unbind(
            Context context, HostView hostView, HostRenderUnit hostRenderUnit, Object layoutData) {
          hostView.setOnTouchListener(null);
          hostView.setInterceptTouchEventHandler(null);
          hostView.setOnClickListener(null);
          hostView.setClickable(false);
          hostView.setOnLongClickListener(null);
          hostView.setLongClickable(false);
          hostView.setOnFocusChangeListener(null);
          hostView.setFocusable(false);
          hostView.setFocusableInTouchMode(false);
        }
      };
}
