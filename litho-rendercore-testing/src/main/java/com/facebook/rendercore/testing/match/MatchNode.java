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

package com.facebook.rendercore.testing.match;

import android.annotation.SuppressLint;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Java6Assertions;

/**
 * Base class for matching against a generic Object. When {@link #assertMatches(Object,
 * DebugTraceContext)} is called, this class will assert the type and props are as expected.
 *
 * <pre>
 *   For example:
 *
 *   MatchNode node = MatchNode.forType(ColorDrawable.class).prop("color", Color.WHITE);
 *
 *   node.assertMatches(new ColorDrawable(Color.WHITE)); // ok!
 *   node.assertMatches(new ColorDrawable(Color.BLACK)); // fails
 *   node.assertMatches(new GradientDrawable()); // fails
 * </pre>
 */
public class MatchNode {

  public static MatchNode forType(Class type) {
    return new MatchNode(type);
  }

  public static class DebugTraceContext {
    private final ArrayList<MatchNode> mNodes = new ArrayList<>();

    public List<MatchNode> getDebugMatchNodeList() {
      return Collections.unmodifiableList(mNodes);
    }
  }

  private final Class mType;
  private final HashMap<String, Object> mExpectedProps = new HashMap<>();

  protected MatchNode(Class type) {
    mType = type;
  }

  /**
   * Provide an expected prop to match against. The value can be the expected value or another
   * MatchNode which will be matched against the actual value.
   *
   * <p>The prop name will be turned into a getter by pre-pending "get": e.g. "color" ->
   * "getColor()".
   */
  public <T> MatchNode prop(String name, T value) {
    mExpectedProps.put(name, value);
    return this;
  }

  /**
   * Asserts the actual object matches the assertions specified by this MatchNode. Subclasses should
   * override this class and call super to provide more assertions.
   */
  public final void assertMatches(Object o, DebugTraceContext debugContext) {
    debugContext.mNodes.add(this);

    Java6Assertions.assertThat(o)
        .describedAs(getDescription("Expecting type " + mType))
        .isInstanceOf(mType);

    for (Map.Entry<String, Object> prop : mExpectedProps.entrySet()) {
      Object expected = prop.getValue();
      Object actual = getProp(o, mType, prop.getKey());
      if (expected instanceof MatchNode) {
        ((MatchNode) expected).assertMatches(actual, debugContext);
      } else {
        Java6Assertions.assertThat(actual)
            .describedAs(getDescription("Comparing field '" + prop.getKey() + "' on " + o))
            .isEqualTo(expected);
      }
    }

    assertMatchesImpl(o, debugContext);

    debugContext.mNodes.remove(debugContext.mNodes.lastIndexOf(this));
  }

  /** For subclasses to override. */
  public void assertMatchesImpl(Object o, DebugTraceContext debugContext) {}

  /**
   * Returns a string that can be used in the `.describedAs()` of a JUnit assertion to provide extra
   * context.
   */
  public String getDescription(String assertionDescription) {
    return assertionDescription + " matching against " + toString();
  }

  @SuppressLint("BadMethodUse-java.lang.String.charAt")
  private static Object getProp(Object o, Class type, String propName) {
    String methodName = "get" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
    try {
      final Method method = type.getMethod(methodName);
      method.setAccessible(true);
      return method.invoke(o);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "mType="
        + mType.getSimpleName()
        + ", mExpectedProps="
        + mExpectedProps
        + '}';
  }
}
