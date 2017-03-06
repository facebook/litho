// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.generator;

import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.facebook.common.internal.ImmutableList;

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
  private final ImmutableList<FieldSpec> fieldSpecs;
  private final ImmutableList<MethodSpec> methodSpecs;
  private final ImmutableList<TypeSpec> typeSpecs;

  private TypeSpecDataHolder(TypeSpecDataHolder.Builder builder) {
    this.fieldSpecs = ImmutableList.copyOf(builder.fieldSpecs);
    this.methodSpecs = ImmutableList.copyOf(builder.methodSpecs);
    this.typeSpecs = ImmutableList.copyOf(builder.typeSpecs);
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
    typeSpec.addFields(fieldSpecs);
    typeSpec.addMethods(methodSpecs);
    typeSpec.addTypes(typeSpecs);
  }

  public static final class Builder {
    private final List<FieldSpec> fieldSpecs = new ArrayList<>();
    private final List<MethodSpec> methodSpecs = new ArrayList<>();
    private final List<TypeSpec> typeSpecs = new ArrayList<>();

    private Builder() {
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
      addFields(typeSpecDataHolder.fieldSpecs);
      addMethods(typeSpecDataHolder.methodSpecs);
      addTypes(typeSpecDataHolder.typeSpecs);
      return this;
    }

    public TypeSpecDataHolder build() {
      return new TypeSpecDataHolder(this);
    }
  }
}
