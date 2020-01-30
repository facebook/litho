// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore.utils;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.View;
import androidx.annotation.RequiresApi;

public class LayoutUtils {

  /**
   * @return whether the current Activity has RTL layout direction -- returns false in API <= 16.
   */
  public static boolean isLayoutDirectionRTL(Context context) {
    ApplicationInfo applicationInfo = context.getApplicationInfo();

    if ((SDK_INT >= JELLY_BEAN_MR1)
        && (applicationInfo.flags & ApplicationInfo.FLAG_SUPPORTS_RTL) != 0) {

      int layoutDirection = API17LayoutUtils.getLayoutDirection(context);
      return layoutDirection == View.LAYOUT_DIRECTION_RTL;
    }

    return false;
  }

  /** Separate class to limit scope of pre-verification failures on older devices. */
  private static class API17LayoutUtils {

    @RequiresApi(JELLY_BEAN_MR1)
    public static int getLayoutDirection(Context context) {
      return context.getResources().getConfiguration().getLayoutDirection();
    }
  }
}
