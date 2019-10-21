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

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.res.Configuration;
import android.os.Build;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ResourceCacheTest {

  @Test
  public void testSameConfigurationDoesNotUpdateResourceCache() {
    Configuration configuration = RuntimeEnvironment.application.getResources().getConfiguration();
    ResourceCache cache = ResourceCache.getLatest(configuration);
    assertThat(cache).isEqualTo(ResourceCache.getLatest(configuration));
  }

  @Test
  public void testSameConfigurationNewInstanceDoesNotUpdateResourceCache() {
    Configuration configuration = RuntimeEnvironment.application.getResources().getConfiguration();
    ResourceCache cache = ResourceCache.getLatest(configuration);
    assertThat(cache).isEqualTo(ResourceCache.getLatest(new Configuration(configuration)));
  }

  @Test
  public void testDifferentLocaleUpdatesResourceCache() {
    Configuration configuration =
        new Configuration(RuntimeEnvironment.application.getResources().getConfiguration());
    setLocale(configuration, new Locale("en"));

    ResourceCache cache = ResourceCache.getLatest(configuration);

    setLocale(configuration, new Locale("it"));
    assertThat(cache).isNotEqualTo(ResourceCache.getLatest(configuration));
  }

  private static void setLocale(Configuration configuration, Locale locale) {
    if (Build.VERSION.SDK_INT >= 17) {
      configuration.setLocale(locale);
    } else {
      configuration.locale = locale;
    }
  }
}
