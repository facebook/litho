/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.squareup.javapoet.ClassName;

public class Utils {

  private static final String SPEC_SUFFIX = "Spec";

  public static String getGenClassName(TypeElement specElement, String name) {
    if (name.isEmpty()) {
      final String className = specElement.getSimpleName().toString();
      if (!className.endsWith(SPEC_SUFFIX)) {
        return null;
      }

      name = className.substring(0, className.length() - SPEC_SUFFIX.length());
    }

    return Utils.getPackageName(specElement.getQualifiedName().toString()) + "." + name;
  }

  /**
   * Get the correct String for representing the given TypeMirror in source code.
   *
   * Right now this always delegates to toString. If this is truly sufficient we should remove this
   * method.
   */
  public static String getTypeName(TypeMirror typeMirror) {
    // TypeMirror.toString() is documented to return a form suitable for representing this type in
    // source code - if possible.
    return typeMirror.toString();
  }

  /**
   * Gather a list of parameters from the given element that have a particular annotation.
   *
   * @param element The executable element.
   * @param annotation The required annotation.
   * @return List of parameters with the annotation.
   */
  public static List<VariableElement> getParametersWithAnnotation(
      ExecutableElement element,
      Class<? extends Annotation> annotation) {
    final List<? extends VariableElement> params = element.getParameters();
    final ArrayList<VariableElement> props = new ArrayList<>();
    for (final VariableElement v : params) {
      if (v.getAnnotation(annotation) != null) {
        props.add(v);
      }
    }

    return props;
  }

  /**
   * Find ExecutableElement (aka methods) children with the given annotation.
   */
  public static <A extends Annotation> List<ExecutableElement> getAnnotatedMethods(
      TypeElement element,
      Class<A> annotation) {
    final List<ExecutableElement> annotatedMethods = new ArrayList<>();
    for (final Element enclosedElement : element.getEnclosedElements()) {
      if (enclosedElement.getKind() == ElementKind.METHOD) {
        final ExecutableElement executableElement = (ExecutableElement) enclosedElement;
        if (executableElement.getAnnotation(annotation) != null) {
          annotatedMethods.add(executableElement);
        }
      }
    }

    return annotatedMethods;
  }

  /**
   * Find inner classes with the given annotation.
   */
  public static <A extends Annotation> List<TypeElement> getAnnotatedClasses(
      TypeElement element, Class<A> annotation) {
    final List<TypeElement> annotatedMethods = new ArrayList<>();
    for (final Element enclosedElement : element.getEnclosedElements()) {
      if (enclosedElement.getKind() == ElementKind.CLASS) {
        final TypeElement typeElement = (TypeElement) enclosedElement;
        if (typeElement.getAnnotation(annotation) != null) {
          annotatedMethods.add(typeElement);
        }
      }
    }

    return annotatedMethods;
  }

  /**
   * Find ExecutableElement (aka methods) children with the given annotation.
   */
  public static <A extends Annotation> ExecutableElement getAnnotatedMethod(
      TypeElement element,
      Class<A> annotation) {
    ExecutableElement annotatedMethod = null;
    for (final Element enclosedElement : element.getEnclosedElements()) {
      if (enclosedElement.getKind() == ElementKind.METHOD) {
        final ExecutableElement executableElement = (ExecutableElement) enclosedElement;
        if (executableElement.getAnnotation(annotation) != null) {
          if (annotatedMethod != null) {
            throw new ComponentsProcessingException(
                element,
                "Duplicate " + annotation + " methods detected in " + element);
          }
          annotatedMethod = executableElement;
        }
      }
    }

    return annotatedMethod;
  }

  /**
   * If the type is outerClass, return the type argument.
   * Otherwise return null.
   */
  public static TypeMirror getGenericTypeArgument(
      TypeMirror diffType,
      ClassName outerClass) {
    if (diffType.getKind() == TypeKind.DECLARED) {
      final DeclaredType parameterDeclaredType = (DeclaredType) diffType;
      final TypeElement typeElement = (TypeElement) parameterDeclaredType.asElement();
      if (typeElement.getQualifiedName().toString().equals(outerClass.toString())) {
        final List<? extends TypeMirror> typeArguments = parameterDeclaredType.getTypeArguments();
        return typeArguments.get(0);
      }
    }

    return null;
  }

  /**
   * If the type is declared, return the class name without any generics.
   * Otherwise return null.
   */
  public static Name getDeclaredClassNameWithoutGenerics(VariableElement variableElement) {
    TypeMirror type = variableElement.asType();
    if (type.getKind() == TypeKind.DECLARED) {
      final DeclaredType parameterDeclaredType = (DeclaredType) type;
      final TypeElement typeElement = (TypeElement) parameterDeclaredType.asElement();

      return typeElement.getQualifiedName();
    }

    return null;
  }

  public static String getSimpleClassName(String qualifiedName) {
    return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
  }

  public static String getPackageName(String qualifiedName) {
    return qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
  }

  public static List<VariableElement> getEnclosedFields(TypeElement typeElement) {
    final List<VariableElement> enclosedFields = new ArrayList<>();
    for (final Element enclosed : typeElement.getEnclosedElements()) {
      if (enclosed.getKind() != ElementKind.FIELD) {
        continue;
      }
      enclosedFields.add((VariableElement) enclosed);
    }

    return enclosedFields;
  }

  public static String capitalize(String s) {
    if (s == null) {
      return null;
    }
    if (s.length() < 2) {
      return s.toUpperCase();
    }
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  public static <T> T getAnnotationParameter(
      ProcessingEnvironment processingEnv,
      Element element,
      Class<?> annotationType,
      String parameterName) {
    List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
    AnnotationMirror mirror = null;
    for (AnnotationMirror m : annotationMirrors) {
      if (m.getAnnotationType().toString().equals(annotationType.getCanonicalName())) {
        mirror = m;
        break;
      }
    }

    if (mirror == null) {
      return null;
    }

    final Elements elements = processingEnv.getElementUtils();
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        elements.getElementValuesWithDefaults(mirror).entrySet()) {
