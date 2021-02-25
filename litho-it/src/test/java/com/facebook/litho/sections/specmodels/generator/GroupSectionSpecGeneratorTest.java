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

package com.facebook.litho.sections.specmodels.generator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.ChangesInfo;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.annotations.OnDataRendered;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.sections.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.sections.specmodels.model.GroupSectionSpecModel;
import com.facebook.litho.sections.specmodels.processor.GroupSectionSpecModelFactory;
import com.facebook.litho.specmodels.generator.DelegateMethodGenerator;
import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.widget.Text;
import com.google.testing.compile.CompilationRule;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class GroupSectionSpecGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();
  @Mock private Messager mMessager;

  private final GroupSectionSpecModelFactory mGroupSectionSpecModelFactory =
      new GroupSectionSpecModelFactory();

  @GroupSectionSpec
  static class TestGroupSectionSpec {

    @OnCreateChildren
    public Children onCreateChildren(SectionContext c) {
      return Children.create()
          .child(
              SingleComponentSection.create(c)
                  .component(Text.create(c).text("Single Component").build()))
          .build();
    }

    @OnDataRendered
    public void onDataRendered(
        SectionContext c,
        boolean isDataChanged,
        boolean isMounted,
        long uptimeMillis,
        int firstVisibleIndex,
        int lastVisibleIndex,
        ChangesInfo changesInfo,
        int globalOffset,
        @Prop boolean arg0,
        @State int arg1) {}
  }

  private SpecModel mSpecModel;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement =
        elements.getTypeElement(TestGroupSectionSpec.class.getCanonicalName());
    mSpecModel =
        mGroupSectionSpecModelFactory.create(
            elements, types, typeElement, mMessager, RunMode.normal(), null, null);
  }

  @Test
  public void testGenerateDelegates() {
    final GroupSectionSpecModel groupSectionSpecModel = (GroupSectionSpecModel) mSpecModel;
    final TypeSpecDataHolder dataHolder =
        DelegateMethodGenerator.generateDelegates(
            groupSectionSpecModel,
            DelegateMethodDescriptions.getGroupSectionSpecDelegatesMap(groupSectionSpecModel),
            RunMode.normal());

    assertThat(dataHolder.getMethodSpecs()).hasSize(2);
    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected com.facebook.litho.sections.Children createChildren(com.facebook.litho.sections.SectionContext c) {\n"
                + "  com.facebook.litho.sections.Children _result;\n"
                + "  _result = (com.facebook.litho.sections.Children) TestGroupSectionSpec.onCreateChildren(\n"
                + "    (com.facebook.litho.sections.SectionContext) c);\n"
                + "  return _result;\n"
                + "}\n");
    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected void dataRendered(com.facebook.litho.sections.SectionContext c, boolean isDataChanged,\n"
                + "    boolean isMounted, long uptimeMillis, int firstVisibleIndex, int lastVisibleIndex,\n"
                + "    com.facebook.litho.sections.ChangesInfo changesInfo, int globalOffset) {\n"
                + "  TestGroupSectionSpec.onDataRendered(\n"
                + "    (com.facebook.litho.sections.SectionContext) c,\n"
                + "    (boolean) isDataChanged,\n"
                + "    (boolean) isMounted,\n"
                + "    (long) uptimeMillis,\n"
                + "    (int) firstVisibleIndex,\n"
                + "    (int) lastVisibleIndex,\n"
                + "    (com.facebook.litho.sections.ChangesInfo) changesInfo,\n"
                + "    (int) globalOffset,\n"
                + "    (boolean) arg0,\n"
                + "    (int) getStateContainerImpl(c).arg1);\n"
                + "}\n");
  }
}
