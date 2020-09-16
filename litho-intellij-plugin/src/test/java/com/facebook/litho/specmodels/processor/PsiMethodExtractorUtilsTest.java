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

package com.facebook.litho.specmodels.processor;

import static com.facebook.litho.specmodels.processor.PsiMethodExtractorUtils.getMethodParams;
import static com.facebook.litho.specmodels.processor.PsiMethodExtractorUtils.getTypeVariables;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class PsiMethodExtractorUtilsTest extends LithoPluginIntellijTest {
  PsiMethod[] methods;

  public PsiMethodExtractorUtilsTest() {
    super("testdata/processor");
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    PsiFile psiFile = testHelper.configure("PsiMethodExtractorUtilsTest.java");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              methods =
                  LithoPluginUtils.getFirstClass(psiFile, cls -> "TestClass".equals(cls.getName()))
                      .get()
                      .getMethods();
            });
  }

  @Test
  public void getMethodParams_noParams_emptyList() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertThat(
                      getMethodParams(
                          methods[1],
                          ImmutableList.<Class<? extends Annotation>>of(),
                          ImmutableList.<Class<? extends Annotation>>of(),
                          ImmutableList.<Class<? extends Annotation>>of()))
                  .hasSize(0);
            });
  }

  @Test
  public void getMethodParams_withParams_fillList() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              List<Class<? extends Annotation>> permittedAnnotations = new ArrayList<>();
              permittedAnnotations.add(Prop.class);
              List<MethodParamModel> methodParams =
                  getMethodParams(
                      methods[0],
                      permittedAnnotations,
                      ImmutableList.<Class<? extends Annotation>>of(),
                      ImmutableList.<Class<? extends Annotation>>of());
              assertThat(methodParams).hasSize(3);
              assertThat(methodParams.get(0).getName()).isEqualTo("c");
              assertThat(methodParams.get(0).getTypeName().toString())
                  .isEqualTo("com.facebook.litho.ComponentContext");
              assertThat(methodParams.get(0).getAnnotations()).hasSize(0);
              assertThat(methodParams.get(1).getName()).isEqualTo("num");
              assertThat(methodParams.get(1).getTypeName().toString()).isEqualTo("int");
              assertThat(methodParams.get(1).getAnnotations()).hasSize(0);
              assertThat(methodParams.get(2).getName()).isEqualTo("prop");
              assertThat(methodParams.get(2).getTypeName().toString()).isEqualTo("Object");
              assertThat(methodParams.get(2).getAnnotations()).hasSize(1);
              assertThat(methodParams.get(2).getAnnotations().get(0).annotationType().getTypeName())
                  .isEqualTo("com.facebook.litho.annotations.Prop");
            });
  }

  @Test
  public void getTypeVariables_noTypeVariables_emptyList() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertThat(getTypeVariables(methods[0])).hasSize(0);
            });
  }

  @Test
  public void getTypeVariables_withTypeVariables_fillList() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertThat(getTypeVariables(methods[1])).hasSize(2);
              assertThat(getTypeVariables(methods[1]).get(0).name).isEqualTo("T");
              assertThat(getTypeVariables(methods[1]).get(0).bounds).hasSize(0);
              assertThat(getTypeVariables(methods[1]).get(1).name).isEqualTo("U");
              assertThat(getTypeVariables(methods[1]).get(1).bounds).hasSize(1);
              assertThat(getTypeVariables(methods[1]).get(1).bounds.get(0).toString())
                  .isEqualTo("java.util.List");
            });
  }
}
