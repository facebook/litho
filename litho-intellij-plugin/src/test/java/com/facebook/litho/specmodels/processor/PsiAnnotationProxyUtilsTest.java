/*
 * Copyright 2019-present Facebook, Inc.
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
package com.facebook.litho.specmodels.processor;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.util.PsiTreeUtil;
import java.io.IOException;
import org.junit.Test;

public class PsiAnnotationProxyUtilsTest extends LithoPluginIntellijTest {

  public PsiAnnotationProxyUtilsTest() {
    super("testdata/processor");
  }

  @Test
  public void defaultValues() {
    testHelper.getPsiClass(
        psiClasses -> {
          assertNotNull(psiClasses);
          PsiClass psiClass = psiClasses.get(0);
          PsiParameter[] parameters =
              PsiTreeUtil.findChildOfType(psiClass, PsiParameterList.class).getParameters();

          Prop prop = PsiAnnotationProxyUtils.findAnnotationInHierarchy(parameters[0], Prop.class);
          assertNotNull(prop);
          assertFalse(prop.optional());
          assertFalse(prop.isCommonProp());
          assertFalse(prop.overrideCommonPropBehavior());
          assertFalse(prop.dynamic());
          assertEquals(ResType.NONE, prop.resType());

          return true;
        },
        "WithAnnotationClass.java");
  }

  @Test
  public void setValues() throws IOException {
    testHelper.getPsiClass(
        psiClasses -> {
          assertNotNull(psiClasses);
          PsiClass psiClass = psiClasses.get(0);
          PsiParameter[] parameters =
              PsiTreeUtil.findChildOfType(psiClass, PsiParameterList.class).getParameters();

          Prop prop = PsiAnnotationProxyUtils.findAnnotationInHierarchy(parameters[1], Prop.class);
          assertNotNull(prop);
          assertTrue(prop.optional());
          assertTrue(prop.isCommonProp());
          assertTrue(prop.overrideCommonPropBehavior());
          assertTrue(prop.dynamic());
          assertEquals(ResType.DRAWABLE, prop.resType());

          return true;
        },
        "WithAnnotationClass.java");
  }
}
