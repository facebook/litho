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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.content.Context;
import com.facebook.litho.ComponentLifecycle.StateUpdate;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentContextTest {

  private TestComponentContext mTestComponentContext;

  @Before
  public void setup() {
    mTestComponentContext = new TestComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testUpdateStateAsync() {
    mTestComponentContext.updateStateAsync(mock(StateUpdate.class));
    assertThat(mTestComponentContext.isUpdateStateAsync()).isTrue();

    ComponentsConfiguration.updateStateAsync = true;

    mTestComponentContext.updateStateSync(mock(StateUpdate.class));
    assertThat(mTestComponentContext.isUpdateStateAsync()).isTrue();
  }

  @Test
  public void testUpdateStateSync() {
    ComponentsConfiguration.updateStateAsync = false;

    mTestComponentContext.updateStateSync(mock(StateUpdate.class));
    assertThat(mTestComponentContext.isUpdateStateAsync()).isFalse();
  }

  private static class TestComponentContext extends ComponentContext {

    private boolean mIsUpdateStateAsync = false;

    public TestComponentContext(Context context) {
      super(context);
    }

    @Override
    public void updateStateAsync(StateUpdate stateUpdate) {
      super.updateStateAsync(stateUpdate);

      mIsUpdateStateAsync = true;
    }

    public boolean isUpdateStateAsync() {
      return mIsUpdateStateAsync;
    }
  }
}
