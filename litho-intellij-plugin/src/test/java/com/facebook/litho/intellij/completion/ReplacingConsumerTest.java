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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionSorter;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.mockito.Mockito;

public class ReplacingConsumerTest extends LithoPluginIntellijTest {

  public ReplacingConsumerTest() {
    super("testdata/completion");
  }

  @Test
  public void consume() {
    testHelper.getPsiClass(
        classes -> {
          PsiClass OneCls = classes.get(0);
          PsiClass LayoutSpecCls = classes.get(1);

          TestCompletionResultSet mutate = new TestCompletionResultSet();
          List<String> namesToReplace = new ArrayList<>();
          namesToReplace.add("One");
          namesToReplace.add("Other");

          ReplacingConsumer replacingConsumer =
              new ReplacingConsumer(namesToReplace, mutate, "Test");

          // Consumes CompletionResult and passes SpecLookupElement replacement to the provided
          // CompletionResultSet
          replacingConsumer.consume(createCompletionResultFor(OneCls));
          assertThat(mutate.elements).hasSize(1);
          assertThat(mutate.elements.get(0).getLookupString()).isEqualTo("One");
          assertThat(mutate.elements.get(0)).isInstanceOf(SpecLookupElement.class);

          replacingConsumer.consume(createCompletionResultFor(LayoutSpecCls));
          assertThat(mutate.elements).hasSize(2);
          assertThat(mutate.elements.get(1).getLookupString()).isEqualTo("LayoutSpec");
          assertThat(mutate.elements.get(1)).isNotInstanceOf(SpecLookupElement.class);

          return true;
        },
        "One.java",
        "LayoutSpec.java");
  }

  private static CompletionResult createCompletionResultFor(PsiClass cls) {
    TestLookupElement lookupElement = new TestLookupElement(cls);
    PrefixMatcher matcher = Mockito.mock(PrefixMatcher.class);
    Mockito.when(matcher.prefixMatches(any(LookupElement.class))).thenReturn(true);
    return CompletionResult.wrap(lookupElement, matcher, Mockito.mock(CompletionSorter.class));
  }

  @Test
  public void insertHandler() {
    testHelper.getPsiClass(
        classes -> {
          PsiClass OneCls = classes.get(0);

          List<String> inserts = new ArrayList<>(1);
          TestCompletionResultSet mutate = new TestCompletionResultSet();
          List<String> namesToReplace = new ArrayList<>();
          namesToReplace.add("One");

          ReplacingConsumer replacingConsumer =
              new ReplacingConsumer(
                  namesToReplace, mutate, (context, item) -> inserts.add(item.getLookupString()));

          replacingConsumer.consume(createCompletionResultFor(OneCls));
          mutate.elements.get(0).handleInsert(Mockito.mock(InsertionContext.class));

          assertThat(inserts).hasSize(1);
          assertThat(inserts.get(0)).isEqualTo("One");

          return true;
        },
        "One.java");
  }

  @Test
  public void addRemainingCompletions() {
    testHelper.runInReadAction(
        project -> {
          TestCompletionResultSet mutate = new TestCompletionResultSet();
          List<String> namesToReplace = new ArrayList<>();
          namesToReplace.add("one");
          namesToReplace.add("other");

          new ReplacingConsumer(namesToReplace, mutate, "Test").addRemainingCompletions(project);

          // Creates Completions without consumption
          assertThat(mutate.elements).hasSize(2);
          assertThat(mutate.elements.get(0).getLookupString()).isEqualTo("other");
          assertThat(mutate.elements.get(1).getLookupString()).isEqualTo("one");
        });
  }

  static class TestLookupElement extends LookupElement {
    final String name;
    final PsiClass mock;

    TestLookupElement(PsiClass cls) {
      name = cls.getQualifiedName();
      mock = cls;
    }

    @Nullable
    @Override
    public PsiElement getPsiElement() {
      return mock;
    }

    @Override
    public String getLookupString() {
      return name;
    }
  }

  static class TestCompletionResultSet extends CompletionResultSet {
    private final List<LookupElement> elements = new ArrayList<>();

    TestCompletionResultSet() {
      super(
          Mockito.mock(PrefixMatcher.class),
          Mockito.mock(Consumer.class),
          Mockito.mock(CompletionContributor.class));
    }

    @Override
    public void addElement(LookupElement element) {
      elements.add(element);
    }

    @Override
    public void passResult(CompletionResult result) {
      elements.add(result.getLookupElement());
    }

    @Override
    public CompletionResultSet withPrefixMatcher(PrefixMatcher matcher) {
      return null;
    }

    @Override
    public CompletionResultSet withPrefixMatcher(String prefix) {
      return null;
    }

    @Override
    public CompletionResultSet withRelevanceSorter(CompletionSorter sorter) {
      return null;
    }

    @Override
    public void addLookupAdvertisement(String text) {}

    @Override
    public CompletionResultSet caseInsensitive() {
      return null;
    }

    @Override
    public void restartCompletionOnPrefixChange(ElementPattern<String> prefixCondition) {}

    @Override
    public void restartCompletionWhenNothingMatches() {}
  }
}
