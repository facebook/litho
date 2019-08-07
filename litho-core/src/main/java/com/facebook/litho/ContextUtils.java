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

package com.facebook.litho;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import androidx.annotation.Nullable;

public class ContextUtils {

  /**
   * @return the Activity representing this Context if the Context is backed by an Activity and the
   * Activity has not been finished/destroyed yet. Returns null otherwise.
   */
  @Nullable
  static Activity getValidActivityForContext(Context context) {
    final Activity activity = findActivityInContext(context);

    if (activity == null || activity.isFinishing() || isActivityDestroyed(activity)) {
      return null;
    }

    return activity;
  }

  /**
   * @return the "most base" Context of this Context, i.e. the Activity, Application, or Service
   *     backing this Context and all its ContextWrappers. In some cases, e.g. instrumentation
   *     tests or other places we don't wrap a standard Context, this root Context may instead be a
   *     raw ContextImpl.
   */
  static Context getRootContext(Context context) {
    Context currentContext = context;

    while (currentContext instanceof ContextWrapper
        && !(currentContext instanceof Activity)
        && !(currentContext instanceof Application)
        && !(currentContext instanceof Service)) {
      currentContext = ((ContextWrapper) currentContext).getBaseContext();
    }

    return currentContext;
  }

  @Nullable
  public static Activity findActivityInContext(Context context) {
    if (context instanceof Activity) {
      return (Activity) context;
    } else if (context instanceof ContextWrapper) {
      return findActivityInContext(((ContextWrapper) context).getBaseContext());
    }

    return null;
  }

  private static boolean isActivityDestroyed(Activity activity) {
    if (SDK_INT >= JELLY_BEAN_MR1) {
      return activity.isDestroyed();
    }

    // If we cannot guarantee that the activity is not destroyed we prefer to assume that it is.
    // This might only happen on ICS.
    return true;
  }

  public static int getTargetSdkVersion(Context context) {
    return context.getApplicationContext().getApplicationInfo().targetSdkVersion;
  }
}
