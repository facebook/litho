/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import android.os.Build;
import android.os.Trace;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.RenderCoreSystrace;
import com.facebook.rendercore.Systracer;

public class DefaultComponentsSystrace implements Systracer {

  @Override
  public void beginSection(String name) {
    if (ComponentsConfiguration.IS_INTERNAL_BUILD
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

      String normalizedName =
          (name.length() > MAX_CHARACTERS_SECTION_NAME)
              ? name.substring(0, MAX_CHARACTERS_SECTION_NAME - 1).concat("…")
              : name;

      Trace.beginSection(normalizedName);
    }
  }

  @Override
  public void beginAsyncSection(String name) {
    // no-op
  }

  @Override
  public void beginAsyncSection(String name, int cookie) {
    // no-op
  }

  @Override
  public ArgsBuilder beginSectionWithArgs(String name) {
    beginSection(name);
    return RenderCoreSystrace.NO_OP_ARGS_BUILDER;
  }

  @Override
  public void endSection() {
    if (ComponentsConfiguration.IS_INTERNAL_BUILD
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      Trace.endSection();
    }
  }

  @Override
  public void endAsyncSection(String name) {
    // no-op
  }

  @Override
  public void endAsyncSection(String name, int cookie) {
    // no-op
  }

  @Override
  public boolean isTracing() {
    return ComponentsConfiguration.IS_INTERNAL_BUILD
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
        && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Trace.isEnabled());
  }

  /**
   * In android.os.Trace there is a limit to the section name (127 characters). If {@link
   * Trace#beginSection(String)} is called with a String bigger than 127, it will throw an
   * exception.
   *
   * <p>We handle this case in our scenario and will trim it in case it is bigger than the valid
   * limit.
   *
   * @see <a href="https://fburl.com/7bngmv5x">android.os.Trace</a>
   */
  private static final int MAX_CHARACTERS_SECTION_NAME = 127;
}
