/*
 * Copyright 2004-present Facebook, Inc.
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
package com.facebook.litho.intellij;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class LithoPluginUtilsTest {

  @Test
  public void isComponentClass() {
    PsiClass component = createSubclassOf("com.facebook.litho.Component");
    Assert.assertTrue(LithoPluginUtils.isComponentClass(component));

    PsiClass notComponent = createSubclassOf("com.facebook.litho.Column");
    Assert.assertFalse(LithoPluginUtils.isComponentClass(notComponent));
  }

  private static PsiClass createSubclassOf(String superClass) {
    PsiClass componentSubclass = Mockito.mock(PsiClass.class);
    PsiClass componentClass = Mockito.mock(PsiClass.class);
    Mockito.when(componentClass.getQualifiedName()).thenReturn(superClass);
    Mockito.when(componentSubclass.getSuperClass()).thenReturn(componentClass);
    return componentSubclass;
  }

  @Test
  public void isSectionClass() {
    PsiClass section = createSubclassOf("com.facebook.litho.sections.Section");
    Assert.assertTrue(LithoPluginUtils.isSectionClass(section));

    PsiClass notSection = createSubclassOf("com.facebook.litho.sections.SectionTest");
    Assert.assertFalse(LithoPluginUtils.isSectionClass(notSection));
  }

  @Test
  public void hasAnnotationStartingWith() {
    String prefix = "abc";
    PsiClass withPrefix = createWithAnnotation(PsiClass.class, prefix + "AnyEnding");
    Assert.assertTrue(
        LithoPluginUtils.hasAnnotation(withPrefix, LithoPluginUtils.startsWith(prefix)));

    PsiClass withoutPrefix = createWithAnnotation(PsiClass.class, "AnyEnding");
    Assert.assertFalse(
        LithoPluginUtils.hasAnnotation(withoutPrefix, LithoPluginUtils.startsWith(prefix)));
  }

  private static <T extends PsiModifierListOwner> T createWithAnnotation(
      Class<T> cls, String annotationName) {
    T withAnnotation = Mockito.mock(cls);
    PsiAnnotation annotation = createPsiAnnotation(annotationName);
    Mockito.when(withAnnotation.getAnnotations()).thenReturn(new PsiAnnotation[] {annotation});
    return withAnnotation;
  }

  private static PsiAnnotation createPsiAnnotation(String annotationName) {
    PsiAnnotation annotation = Mockito.mock(PsiAnnotation.class);
    Mockito.when(annotation.getQualifiedName()).thenReturn(annotationName);
    return annotation;
  }

  @Test
  public void hasLithoComponentSpecAnnotation() {
    PsiClass withLitho = createSpecWithAnnotation("com.facebook.litho.annotations.MountSpec");
    Assert.assertTrue(LithoPluginUtils.hasLithoComponentSpecAnnotation(withLitho));

    PsiClass withoutLitho = createSpecWithAnnotation("com.facebook.litho.sections.annotations.Any");
    Assert.assertFalse(LithoPluginUtils.hasLithoComponentSpecAnnotation(withoutLitho));

    PsiClass notSpec = createWithAnnotation(PsiClass.class, "com.facebook.litho.annotations.Any");
    Assert.assertFalse(LithoPluginUtils.hasLithoComponentSpecAnnotation(notSpec));
  }

  @Test
  public void hasLithoSectionSpecAnnotation() {
    PsiClass withLithoSection =
        createSpecWithAnnotation("com.facebook.litho.sections.annotations.Any");
    Assert.assertTrue(LithoPluginUtils.hasLithoSectionSpecAnnotation(withLithoSection));

    PsiClass withoutLithoSection = createSpecWithAnnotation("com.facebook.litho.annotations.Any");
    Assert.assertFalse(LithoPluginUtils.hasLithoSectionSpecAnnotation(withoutLithoSection));

    PsiClass notSpec =
        createWithAnnotation(PsiClass.class, "com.facebook.litho.sections.annotations.Any");
    Assert.assertFalse(LithoPluginUtils.hasLithoSectionSpecAnnotation(notSpec));
  }

  @Test
  public void isSpecName() {
    Assert.assertTrue(LithoPluginUtils.isSpecName("AnySpec"));
    Assert.assertTrue(LithoPluginUtils.isSpecName("my.domain.TestSpec"));
    Assert.assertFalse(LithoPluginUtils.isSpecName("Any"));
    Assert.assertFalse(LithoPluginUtils.isSpecName(""));
  }

  private static PsiClass createSpecWithAnnotation(String annotationName) {
    PsiClass withAnnotation = createWithAnnotation(PsiClass.class, annotationName);
    Mockito.when(withAnnotation.getName()).thenReturn("AnySpec");
    return withAnnotation;
  }

  @Test
  public void isProp() {
    PsiParameter prop =
        createWithAnnotation(PsiParameter.class, "com.facebook.litho.annotations.Prop");
    Assert.assertTrue(LithoPluginUtils.isProp(prop));

    PsiParameter notProp =
        createWithAnnotation(PsiParameter.class, "com.facebook.litho.annotations.PropDefault");
    Assert.assertFalse(LithoPluginUtils.isProp(notProp));

    PsiParameter notProp2 = createWithAnnotation(PsiParameter.class, "any.Prop");
    Assert.assertFalse(LithoPluginUtils.isProp(notProp2));
  }

  @Test
  public void isState() {
    PsiParameter state =
        createWithAnnotation(PsiParameter.class, "com.facebook.litho.annotations.State");
    Assert.assertTrue(LithoPluginUtils.isState(state));

    PsiParameter notState =
        createWithAnnotation(PsiParameter.class, "com.facebook.litho.annotations.StateAny");
    Assert.assertFalse(LithoPluginUtils.isState(notState));

    PsiParameter notState2 = createWithAnnotation(PsiParameter.class, "any.State");
    Assert.assertFalse(LithoPluginUtils.isState(notState2));
  }

  @Test
  public void isEvent() {
    PsiClass event = createWithAnnotation(PsiClass.class, "com.facebook.litho.annotations.Event");
    Assert.assertTrue(LithoPluginUtils.isEvent(event));

    PsiClass notEvent =
        createWithAnnotation(PsiClass.class, "com.facebook.litho.annotations.EventAny");
    Assert.assertFalse(LithoPluginUtils.isEvent(notEvent));

    PsiClass notEvent2 = createWithAnnotation(PsiClass.class, "any.Event");
    Assert.assertFalse(LithoPluginUtils.isEvent(notEvent2));
  }

  @Test
  public void getLithoComponentNameFromSpec() {
    Assert.assertEquals("Test", LithoPluginUtils.getLithoComponentNameFromSpec("TestSpec"));
  }

  @Test
  public void getLithoComponentSpecNameFromComponent() {
    Assert.assertEquals(
        "NameSpec", LithoPluginUtils.getLithoComponentSpecNameFromComponent("Name"));
  }
}
