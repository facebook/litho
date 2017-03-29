/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.javapoet;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import java.util.List;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.ParameterizedTypeName;

public class JPUtil {

  public static Class getClass(String type) throws ClassNotFoundException {
    try {
      return ClassLoader.getSystemClassLoader().loadClass(type);
    } catch (ClassNotFoundException e) {
      return ClassLoader.getSystemClassLoader().loadClass("java.lang." + type);
    }
  }

  public static ClassName getClassName(String type) {
    try {
      return ClassName.get(getClass(type));
    } catch (ClassNotFoundException e) {
      try {
        return ClassName.get(getClass("java.lang." + type));
      } catch (ClassNotFoundException x) {
        return ClassName.bestGuess(type);
      }
    }
  }

  public static TypeName getVariableType(String type) {
    switch (type) {
      case "byte":
        return ClassName.BYTE;
      case "short":
        return ClassName.SHORT;
      case "int":
        return ClassName.INT;
      case "long":
        return ClassName.LONG;
      case "char":
        return ClassName.CHAR;
      case "float":
        return ClassName.FLOAT;
      case "double":
        return ClassName.DOUBLE;
      case "boolean":
        return ClassName.BOOLEAN;
    }
    return getClassName(type);
  }

  public static Class getBoxedVariableClass(String type) throws ClassNotFoundException {
    switch (type) {
      case "byte":
        return Byte.class;
      case "short":
        return Short.class;
      case "int":
        return Integer.class;
      case "long":
        return Long.class;
      case "char":
        return Character.class;
      case "float":
        return Float.class;
      case "double":
        return Double.class;
      case "boolean":
        return Boolean.class;
    }
    return getClass(type);
  }

  public static TypeName getBoxedVariableType(String type) {
    switch (type) {
      case "byte":
        return ClassName.get(Byte.class);
      case "short":
        return ClassName.get(Short.class);
      case "int":
        return ClassName.get(Integer.class);
      case "long":
        return ClassName.get(Long.class);
      case "char":
        return ClassName.get(Character.class);
      case "float":
        return ClassName.get(Float.class);
      case "double":
        return ClassName.get(Double.class);
      case "boolean":
        return ClassName.get(Boolean.class);
    }
    return getClassName(type);
  }

  public static TypeName getTypeFromMirror(TypeMirror mirror) {
    if (mirror.getKind() == TypeKind.ERROR) {
      return ClassName.bestGuess(mirror.toString());
    } else if (mirror.getKind() == TypeKind.DECLARED) {
      DeclaredType declaredType = (DeclaredType) mirror;
      TypeElement typeElement = (TypeElement) declaredType.asElement();

      List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
      ClassName variableClassName = ClassName.bestGuess(typeElement.getQualifiedName().toString());

      if (typeArguments.isEmpty()) {
        return variableClassName;
      }

      TypeName[] parameters = new TypeName[typeArguments.size()];
      for (int i = 0; i < parameters.length; i++) {
        parameters[i] = getTypeFromMirror(typeArguments.get(i));
      }

      return ParameterizedTypeName.get(variableClassName, parameters);
    } else {
      return ClassName.get(mirror);
    }
  }

  public static CodeBlock wrapInitializer(TypeName type, String value) {
    String rep = "$L";
    if (type.equals(getClassName(String.class.getName()))) {
      rep = "$S";
    }
    return CodeBlock.builder().add(rep, value).build();
  }
