// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.generator;

import javax.lang.model.element.Modifier;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

import com.facebook.common.internal.ImmutableList;
import com.facebook.components.annotations.OnCreateTreeProp;
import com.facebook.components.annotations.Prop;
import com.facebook.components.annotations.State;
import com.facebook.components.specmodels.model.ClassNames;
import com.facebook.components.specmodels.model.DelegateMethodModel;
import com.facebook.components.specmodels.model.MethodParamModelFactory;
import com.facebook.components.specmodels.model.SpecModel;
import com.facebook.components.specmodels.model.TreePropModel;
import com.facebook.testing.robolectric.v3.WithTestDefaultsRunner;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link TreePropGenerator}
 */
@RunWith(WithTestDefaultsRunner.class)
public class TreePropGeneratorTest {
  private SpecModel mSpecModel = mock(SpecModel.class);
  private TreePropModel mTreeProp = mock(TreePropModel.class);
  private DelegateMethodModel mOnCreateTreePropMethodModel;

  @Before
  public void setUp() {
    mOnCreateTreePropMethodModel = new DelegateMethodModel(
        ImmutableList.<Annotation>of(new OnCreateTreeProp() {

          @Override
          public Class<? extends Annotation> annotationType() {
            return OnCreateTreeProp.class;
          }

          @Override
          public String name() {
            return "treeProp";
          }
        }),
        ImmutableList.of(Modifier.PROTECTED),
        "onCreateTreeProp",
        TypeName.BOOLEAN,
        ImmutableList.of(
            MethodParamModelFactory.create(
                ClassNames.COMPONENT_CONTEXT,
                "componentContext",
                new ArrayList<Annotation>(),
                new ArrayList<AnnotationSpec>(),
                null),
            MethodParamModelFactory.create(
                TypeName.BOOLEAN,
                "prop",
                ImmutableList.of(createAnnotation(Prop.class)),
                new ArrayList<AnnotationSpec>(),
                null),
            MethodParamModelFactory.create(
                TypeName.INT,
                "state",
                ImmutableList.of(createAnnotation(State.class)),
                new ArrayList<AnnotationSpec>(),
                null)),
        null);

    when(mTreeProp.getName()).thenReturn("treeProp");
    when(mTreeProp.getType()).thenReturn(TypeName.INT);

    when(mSpecModel.getContextClass()).thenReturn(ClassNames.COMPONENT_CONTEXT);
    when(mSpecModel.getComponentClass()).thenReturn(ClassNames.COMPONENT);
    when(mSpecModel.getComponentName()).thenReturn("Test");
    when(mSpecModel.getDelegateMethods())
        .thenReturn(ImmutableList.of(mOnCreateTreePropMethodModel));
    when(mSpecModel.getTreeProps()).thenReturn(ImmutableList.of(mTreeProp));
  }

  @Test
  public void testGenerate() {
    TypeSpecDataHolder typeSpecDataHolder =
        TreePropGenerator.generate(mSpecModel);

    assertThat(typeSpecDataHolder.getFieldSpecs()).isEmpty();
    assertThat(typeSpecDataHolder.getMethodSpecs()).hasSize(2);
    assertThat(typeSpecDataHolder.getTypeSpecs()).isEmpty();

    assertThat(typeSpecDataHolder.getMethodSpecs().get(0).toString()).isEqualTo(
        "@java.lang.Override\n" +
            "protected void populateTreeProps(com.facebook.components.Component _abstractImpl, com.facebook.components.TreeProps treeProps) {\n" +
            "  if (treeProps == null) {\n" +
            "    return;\n" +
            "  }\n" +
            "  final TestImpl _impl = (TestImpl) _abstractImpl;\n" +
            "  _impl.treeProp = treeProps.get(\"int~treeProp\");\n" +
            "}\n");

    assertThat(typeSpecDataHolder.getMethodSpecs().get(1).toString()).isEqualTo(
        "@java.lang.Override\n" +
        "protected com.facebook.components.TreeProps getTreePropsForChildren(com.facebook.components.ComponentContext c, com.facebook.components.Component _abstractImpl, com.facebook.components.TreeProps parentTreeProps) {\n" +
        "  final TestImpl _impl = (TestImpl) _abstractImpl;\n" +
        "  final com.facebook.components.TreeProps childTreeProps = com.facebook.components.TreeProps.copy(parentTreeProps);\n" +
        "  childTreeProps.put(\"boolean~treeProp\", mSpec.onCreateTreeProp(\n" +
        "      (com.facebook.components.ComponentContext) c,\n" +
        "      (boolean) _impl.prop,\n" +
        "      (int) _impl.state));\n" +
        "  return childTreeProps;\n" +
        "}\n");
  }

  private static Annotation createAnnotation(final Class<? extends Annotation> annotationClass) {
    return new Annotation() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return annotationClass;
      }
    };
  }
}
