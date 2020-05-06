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

package com.facebook.rendercore.testing;

import android.view.View;
import android.view.ViewGroup;
import com.facebook.rendercore.testing.match.MatchNode;
import com.facebook.rendercore.testing.match.ViewMatchNode;
import java.util.List;
import org.junit.ComparisonFailure;

/**
 * Utility to match a rendered View hierarchy against an expected View hierarchy. See {@link
 * ViewMatchNode}.
 */
public class ViewAssertions {

  public static MatchAssertionBuilder assertThat(View view) {
    return new MatchAssertionBuilder(view);
  }

  public static class MatchAssertionBuilder {

    private final View mView;

    private MatchAssertionBuilder(View view) {
      mView = view;
    }

    public void matches(ViewMatchNode root) {
      final MatchNode.DebugTraceContext debugTraceContext = new MatchNode.DebugTraceContext();
      try {
        root.assertMatches(mView, debugTraceContext);
      } catch (AssertionError e) {
        String viewHierarchy = getHierarchyAsString(mView);
        String matchNodeList = getDebugMatchNodeString(debugTraceContext);
        String context =
            "View hierarchy: " + viewHierarchy + "\nMatchNode hierarchy:\n" + matchNodeList;
        if (e instanceof ComparisonFailure) {
          ComparisonFailure withContext =
              new ComparisonFailure(
                  "\n" + context + "\n" + e.getMessage(),
                  ((ComparisonFailure) e).getExpected(),
                  ((ComparisonFailure) e).getActual());
          withContext.setStackTrace(e.getStackTrace());
          throw withContext;
        } else {
          System.err.println("ViewAssertion Context:\n" + context);
          throw e;
        }
      }
    }

    private String getDebugMatchNodeString(MatchNode.DebugTraceContext debugTraceContext) {
      List<MatchNode> matchNodes = debugTraceContext.getDebugMatchNodeList();
      if (matchNodes == null || matchNodes.isEmpty()) {
        return "No match nodes";
      }

      String res = "";
      int i = 1;
      for (MatchNode matchNode : matchNodes) {
        res += "" + i + ". " + matchNode.toString() + "\n";
        i++;
      }
      return res;
    }

    private String getHierarchyAsString(View view) {
      return "\n" + getHierarchyAsString(view, 0);
    }

    private String getHierarchyAsString(View view, int depth) {
      String res = "-";
      for (int i = 0; i < depth; i++) {
        res += "--";
      }
      res += " " + viewToString(view) + "\n";
      if (view instanceof ViewGroup) {
        ViewGroup asViewGroup = (ViewGroup) view;
        for (int i = 0; i < asViewGroup.getChildCount(); i++) {
          res += getHierarchyAsString(asViewGroup.getChildAt(i), depth + 1);
        }
      }
      return res;
    }

    public static String viewToString(View view) {
      return view.getClass().getSimpleName() + "@" + Integer.toHexString(view.hashCode());
    }
  }
}
