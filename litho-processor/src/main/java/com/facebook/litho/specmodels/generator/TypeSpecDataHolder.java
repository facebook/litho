/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.facebook.common.internal.ImmutableList;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * An object that holds data that can be used in the construction of a
 * {@link com.squareup.javapoet.TypeSpec}.
 *
 * This is a wrapper that is used to allow TypeSpecs to be composed. It is a dumb wrapper - any
 * checking of the semantics will be done in the TypeSpec itself.
 */
@Immutable
public class TypeSpecDataHolder {
  private final ImmutableList<JavadocSpec> javadocSpecs;
  private final ImmutableList<AnnotationSpec> annotationSpecs;
  private final ImmutableList<FieldSpec> fieldSpecs;
  private final ImmutableList<MethodSpec> methodSpecs;
  private final ImmutableList<TypeSpec> typeSpecs;

  private TypeSpecDataHolder(TypeSpecDataHolder.Builder builder) {
    this.javadocSpecs = ImmutableList.copyOf(builder.javadocSpecs);
    this.annotationSpecs = ImmutableList.copyOf(builder.annotationSpecs);
    this.fieldSpecs = ImmutableList.copyOf(builder.fieldSpecs);
    this.methodSpecs = ImmutableList.copyOf(builder.methodSpecs);
    this.typeSpecs = ImmutableList.copyOf(builder.typeSpecs);
  }

  public ImmutableList<JavadocSpec> getJavadocSpecs() {
    return javadocSpecs;
  }

  public ImmutableList<AnnotationSpec> getAnnotationSpecs() {
    return annotationSpecs;
  }

  public ImmutableList<FieldSpec> getFieldSpecs() {
    return fieldSpecs;
  }

  public ImmutableList<MethodSpec> getMethodSpecs() {
    return methodSpecs;
  }

  public ImmutableList<TypeSpec> getTypeSpecs() {
    return typeSpecs;
  }

  public static TypeSpecDataHolder.Builder newBuilder() {
    return new TypeSpecDataHolder.Builder();
  }

  public void addToTypeSpec(TypeSpec.Builder typeSpec) {
    for (JavadocSpec javadocSpec : javadocSpecs) {
      typeSpec.addJavadoc(javadocSpec.format, javadocSpec.args);
    }
    typeSpec.addAnnotations(annotationSpecs);
    typeSpec.addFields(fieldSpecs);
    typeSpec.addMethods(methodSpecs);
    typeSpec.addTypes(typeSpecs);
  }

  public static final class Builder {
    private final List<JavadocSpec> javadocSpecs = new ArrayList<>();
    private final List<AnnotationSpec> annotationSpecs = new ArrayList<>();
    private final List<FieldSpec> fieldSpecs = new ArrayList<>();
    private final List<MethodSpec> methodSpecs = new ArrayList<>();
    private final List<TypeSpec> typeSpecs = new ArrayList<>();

    private Builder() {
    }

    public TypeSpecDataHolder.Builder addJavadoc(JavadocSpec javadocSpec) {
      javadocSpecs.add(javadocSpec);
      return this;
    }

    public TypeSpecDataHolder.Builder addJavadocs(Iterable<JavadocSpec> javadocSpecs) {
      for (JavadocSpec javadocSpec : javadocSpecs) {
        addJavadoc(javadocSpec);
      }
      return this;
    }

    public TypeSpecDataHolder.Builder addAnnotation(AnnotationSpec annotationSpec) {
      annotationSpecs.add(annotationSpec);
      return this;
    }

    public TypeSpecDataHolder.Builder addAnnotations(Iterable<AnnotationSpec> annotationSpecs) {
      for (AnnotationSpec annotationSpec : annotationSpecs) {
        addAnnotation(annotationSpec);
      }
      return this;
    }

    public TypeSpecDataHolder.Builder addFields(Iterable<FieldSpec> fieldSpecs) {
      for (FieldSpec fieldSpec : fieldSpecs) {
        addField(fieldSpec);
      }
      return this;
    }

    public TypeSpecDataHolder.Builder addField(FieldSpec fieldSpec) {
      fieldSpecs.add(fieldSpec);
      return this;
    }

    public TypeSpecDataHolder.Builder addField(TypeName type, String name, Modifier... modifiers) {
      return addField(FieldSpec.builder(type, name, modifiers).build());
    }

    public TypeSpecDataHolder.Builder addField(Type type, String name, Modifier... modifiers) {
      return addField(TypeName.get(type), name, modifiers);
    }

    public TypeSpecDataHolder.Builder addMethods(Iterable<MethodSpec> methodSpecs) {
      for (MethodSpec methodSpec : methodSpecs) {
        addMethod(methodSpec);
      }
      return this;
    }

    public TypeSpecDataHolder.Builder addMethod(MethodSpec methodSpec) {
      methodSpecs.add(methodSpec);
      return this;
    }

    public TypeSpecDataHolder.Builder addTypes(Iterable<TypeSpec> typeSpecs) {
      for (TypeSpec typeSpec : typeSpecs) {
        addType(typeSpec);
      }
      return this;
    }

    public TypeSpecDataHolder.Builder addType(TypeSpec typeSpec) {
      typeSpecs.add(typeSpec);
      return this;
    }

    public TypeSpecDataHolder.Builder addTypeSpecDataHolder(TypeSpecDataHolder typeSpecDataHolder) {
      addJavadocs(typeSpecDataHolder.javadocSpecs);
      addAnnotations(typeSpecDataHolder.annotationSpecs);
      addFields(typeSpecDataHolder.fieldSpecs);
      addMethods(typeSpecDataHolder.methodSpecs);
      addTypes(typeSpecDataHolder.typeSpecs);
      return this;
    }

    public TypeSpecDataHolder build() {
      return new TypeSpecDataHolder(this);
    }
  }

  public static class JavadocSpec {
    public final String format;
    public final Object[] args;

    public JavadocSpec(String format, Object... args) {
      this.format = format;
      this.args = args;
    }

    @Override
    public String toString() {
      return CodeBlock.builder()
          .add(format, args)
          .build()
          .toString();
    }
  }
}
