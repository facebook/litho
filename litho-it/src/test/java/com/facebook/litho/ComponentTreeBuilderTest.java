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

import static com.facebook.litho.testing.Whitebox.getInternalState;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.os.Handler;
import android.os.Looper;
import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/**
 * Tests for {@link ComponentTree.Builder}
 */

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeBuilderTest {
  private static final String mLogTag = "logTag";
  private final Object mLayoutLock = new Object();

  private ComponentContext mContext;
  private Component mRoot;
  private ComponentTree.Builder mComponentTreeBuilder;
  private Looper mLooper;
  private ComponentsLogger mComponentsLogger;

  @Before
  public void setup() throws Exception {
    mLooper = mock(Looper.class);
    mComponentsLogger = mock(ComponentsLogger.class);
    mContext = new ComponentContext(RuntimeEnvironment.application, mLogTag, mComponentsLogger);
    mRoot = TestLayoutComponent.create(mContext)
        .build();

    mComponentTreeBuilder = ComponentTree.create(mContext, mRoot);
  }

  @Test
  public void testDefaultCreation() {
    ComponentTree componentTree = mComponentTreeBuilder.build();

    assertSameAsInternalState(componentTree, mRoot, "mRoot");
    assertDefaults(componentTree);
  }

  @Test
  public void testCreationWithInputs() {
    ComponentTree componentTree =
        mComponentTreeBuilder
            .layoutThreadLooper(mLooper)
            .build();

    assertSameAsInternalState(componentTree, mRoot, "mRoot");
    assertEqualToInternalState(componentTree, true, "mIsLayoutDiffingEnabled");

    assertThat(componentTree.isIncrementalMountEnabled()).isTrue();
    assertThat(mContext.getLogger()).isEqualTo(mComponentsLogger);
    assertThat(mContext.getLogTag()).isEqualTo(mLogTag);

    Handler handler = getInternalState(componentTree, "mLayoutThreadHandler");
    assertThat(mLooper).isSameAs(handler.getLooper());
  }

  private static void assertSameAsInternalState(
      ComponentTree componentTree,
      Object object,
      String internalName) {
    assertThat(object).isSameAs(getInternalState(componentTree, internalName));
  }

  private static void assertEqualToInternalState(
      ComponentTree componentTree,
      Object object,
      String internalName) {
    assertThat((Object) getInternalState(componentTree, internalName)).isEqualTo(object);
  }

  private static void assertDefaults(ComponentTree componentTree) {
    assertEqualToInternalState(componentTree, true, "mIsLayoutDiffingEnabled");

    assertThat(componentTree.isIncrementalMountEnabled()).isTrue();
  }
}
