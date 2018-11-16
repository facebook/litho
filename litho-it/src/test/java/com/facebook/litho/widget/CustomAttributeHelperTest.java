/*
 * Copyright 2018-present Facebook, Inc.
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
package com.facebook.litho.widget;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class CustomAttributeHelperTest {
  private CustomAttributeHelper<String> mAttributeHelper;
  private TestRenderInfoBuilder mRenderInfoBuilder;
  private RenderInfo mRenderInfo;

  @Before
  public void setup() {
    mAttributeHelper = new TestAttributeHelper();
    mRenderInfoBuilder = new TestRenderInfoBuilder();

    mRenderInfo = mock(RenderInfo.class);
  }

  @Test
  public void testAttrSet() {
    String object = "aoie";
    mAttributeHelper.addAttribute(object, mRenderInfoBuilder);
    assertEquals(object, mRenderInfoBuilder.attrs.get(TestAttributeHelper.TAG));
  }

  @Test
  public void testAttrGet() {
    String object = "aabc";
    when(mRenderInfo.getCustomAttribute(TestAttributeHelper.TAG)).thenReturn(object);
    String attribute = mAttributeHelper.getAttribute(mRenderInfo);
    assertEquals(object, attribute);
  }

  static class TestAttributeHelper extends CustomAttributeHelper<String> {
    static final String TAG = "tag";

    @Override
    protected String getTag() {
      return TAG;
    }
  }

  static class TestRenderInfoBuilder extends ComponentRenderInfo.Builder {
    final Map<String, Object> attrs = new HashMap<>();

    @Override
    public ComponentRenderInfo.Builder customAttribute(String key, Object value) {
      attrs.put(key, value);
      return this;
    }
  }
}
