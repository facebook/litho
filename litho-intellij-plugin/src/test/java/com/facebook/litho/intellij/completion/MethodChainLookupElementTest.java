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

package com.facebook.litho.intellij.completion;

import static org.junit.Assert.assertEquals;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.impl.Variable;
import com.intellij.psi.PsiExpression;
import com.intellij.testFramework.fixtures.TestLookupElementPresentation;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

public class MethodChainLookupElementTest extends LithoPluginIntellijTest {

  public MethodChainLookupElementTest() {
    super("testdata/completion");
  }

  @Test
  public void createMethodChain() {
    testHelper.runInReadAction(
        project -> {
          List<String> names = new ArrayList<>(2);
          names.add("methodName1");
          names.add("otherName");

          PsiExpression methodChain =
              MethodChainLookupElement.createMethodChain(project, "methodNameBase", names);
          assertEquals(
              "methodNameBase(insert_placeholder_c)\n"
                  + ".methodName1(insert_placeholder)\n"
                  + ".otherName(insert_placeholder)",
              methodChain.getText());
        });
  }

  @Test
  public void renderElement() {
    LookupElement testDelegate = new TestLookupElement("");
    Template testTemplate = new TestTemplate("beforeDot.afterDot.afterDot");
    LookupElementPresentation testPresentation = new TestLookupElementPresentation();
    testPresentation.setTailText("oldTail");

    new MethodChainLookupElement(testDelegate, testTemplate).renderElement(testPresentation);
    assertEquals("oldTail.afterDot.afterDot", testPresentation.getTailText());
  }

  static class TestLookupElement extends LookupElement {
    private String lookupString;

    TestLookupElement(String lookupString) {
      this.lookupString = lookupString;
    }

    @Override
    public String getLookupString() {
      return lookupString;
    }
  }

  static class TestTemplate extends Template {
    private String templateText;

    TestTemplate(String templateText) {
      this.templateText = templateText;
    }

    @Override
    public String getTemplateText() {
      return templateText;
    }

    @Override
    public void addTextSegment(String text) {}

    @Override
    public void addVariableSegment(String name) {}

    @Override
    public Variable addVariable(Expression expression, boolean isAlwaysStopAt) {
      return null;
    }

    @Override
    public Variable addVariable(
        String name,
        Expression expression,
        Expression defaultValueExpression,
        boolean isAlwaysStopAt,
        boolean skipOnStart) {
      return null;
    }

    @Override
    public Variable addVariable(
        String name, String expression, String defaultValueExpression, boolean isAlwaysStopAt) {
      return null;
    }

    @Override
    public void addEndVariable() {}

    @Override
    public void addSelectionStartVariable() {}

    @Override
    public void addSelectionEndVariable() {}

    @Override
    public String getId() {
      return null;
    }

    @Override
    public String getKey() {
      return null;
    }

    @Nullable
    @Override
    public String getDescription() {
      return null;
    }

    @Override
    public boolean isToReformat() {
      return false;
    }

    @Override
    public void setToReformat(boolean toReformat) {}

    @Override
    public void setToIndent(boolean toIndent) {}

    @Override
    public void setInline(boolean isInline) {}

    @Override
    public int getSegmentsCount() {
      return 0;
    }

    @Override
    public String getSegmentName(int segmentIndex) {
      return null;
    }

    @Override
    public int getSegmentOffset(int segmentIndex) {
      return 0;
    }

    @Override
    public String getString() {
      return null;
    }

    @Override
    public boolean isToShortenLongNames() {
      return false;
    }

    @Override
    public void setToShortenLongNames(boolean toShortenLongNames) {}
  }
}
