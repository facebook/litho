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

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiTreeUtil;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import org.jetbrains.annotations.Nullable;

/**
 * Makes a more robustly-featured Proxy for wrapping Annotations in Psi-land. This makes Enums
 * usable and implements methods like {@code hashCode}, which go unimplemented by Psi's default
 * Annotation implementation.
 */
public class PsiAnnotationProxyUtils {

  @Nullable
  public static <T extends Annotation> T findAnnotationInHierarchy(
      PsiModifierListOwner listOwner, Class<T> annotationClass) {
    T basicProxy = AnnotationUtil.findAnnotationInHierarchy(listOwner, annotationClass);
    if (basicProxy == null) {
      return null;
    }

    T biggerProxy =
        (T)
            Proxy.newProxyInstance(
                annotationClass.getClassLoader(),
                new Class[] {annotationClass},
                new AnnotationProxyInvocationHandler(basicProxy, listOwner, annotationClass));

    return biggerProxy;
  }

  private static class AnnotationProxyInvocationHandler<T extends Annotation>
      implements InvocationHandler {
    private final T mStubbed;
    private final PsiModifierListOwner mListOwner;
    private final Class<T> mAnnotationClass;

    AnnotationProxyInvocationHandler(
        T stubbed, PsiModifierListOwner listOwner, Class<T> annotationClass) {
      mStubbed = stubbed;
      mListOwner = listOwner;
      mAnnotationClass = annotationClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws IllegalAccessException {
      switch (method.getName()) {
        case "hashCode":
          return proxyHashCode();
        case "equals":
          return proxyEquals(args[0]);
        case "toString":
          return mStubbed.toString();
        default:
          return invoke(method, args);
      }
    }

    private Object invoke(Method method) throws IllegalAccessException {
      return invoke(method, null);
    }

    private Object invoke(Method method, Object[] args) throws IllegalAccessException {
      Class<?> returnType = method.getReturnType();
      if (returnType.isEnum()) {
        PsiAnnotation currentAnnotation =
            AnnotationUtil.findAnnotationInHierarchy(
                mListOwner, Collections.singleton(mAnnotationClass.getCanonicalName()));
        PsiExpression declaredValue =
            (PsiExpression) currentAnnotation.findAttributeValue(method.getName());
        if (declaredValue == null) {
          return method.getDefaultValue();
        }
        PsiIdentifier identifier = PsiTreeUtil.getChildOfType(declaredValue, PsiIdentifier.class);
        if (identifier == null) {
          return method.getDefaultValue();
        }
        return Enum.valueOf((Class<Enum>) returnType, identifier.getText());
      }

      try {
        if (args == null) {
          return method.invoke(mStubbed);
        }
        return method.invoke(mStubbed, args);
      } catch (InvocationTargetException e) {
        return method.getDefaultValue();
      }
    }

    /**
     * NOTE: Taken from Annotation.java. This is an approximation of the below Javadoc.
     *
     * <p>Returns the hash code of this annotation, as defined below:
     *
     * <p>The hash code of an annotation is the sum of the hash codes of its members (including
     * those with default values), as defined below:
     *
     * <p>The hash code of an annotation member is (127 times the hash code of the member-name as
     * computed by {@link String#hashCode()}) XOR the hash code of the member-value, as defined
     * below:
     *
     * <p>The hash code of a member-value depends on its type:
     *
     * <ul>
     *   <li>The hash code of a primitive value <tt><i>v</i></tt> is equal to
     *       <tt><i>WrapperType</i>.valueOf(<i>v</i>).hashCode()</tt>, where
     *       <tt><i>WrapperType</i></tt> is the wrapper type corresponding to the primitive type of
     *       <tt><i>v</i></tt> ({@link Byte}, {@link Character}, {@link Double}, {@link Float},
     *       {@link Integer}, {@link Long}, {@link Short}, or {@link Boolean}).
     *   <li>The hash code of a string, enum, class, or annotation member-value I <tt><i>v</i></tt>
     *       is computed as by calling <tt><i>v</i>.hashCode()</tt>. (In the case of annotation
     *       member values, this is a recursive definition.)
     *   <li>The hash code of an array member-value is computed by calling the appropriate
     *       overloading of {@link java.util.Arrays#hashCode(long[]) Arrays.hashCode} on the value.
     *       (There is one overloading for each primitive type, and one for object reference types.)
     * </ul>
     *
     * @return the hash code of this annotation
     */
    private <A> int proxyHashCode() {
      int hashCode = 0;
      for (Method method : mAnnotationClass.getDeclaredMethods()) {
        Object returnedObject;
        try {
          returnedObject = invoke(method);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(
              String.format("Failed to invoke method '%s' on annotation '%s'", method, mStubbed),
              e);
        }

        int memberHash = 127 * method.getName().hashCode();
        Class<?> returnType = method.getReturnType();
        if (returnType.isArray()) {
          A[] array = (A[]) returnedObject;
          memberHash ^= Arrays.hashCode(array);
        } else {
          memberHash ^= returnedObject.hashCode();
        }

        hashCode += memberHash;
      }

      return hashCode;
    }

    // Approximates the Annotation.equals() javadoc's implementation
    private <A> boolean proxyEquals(@Nullable Object other) {
      if (!mAnnotationClass.isInstance(other)) {
        return false;
      }
      boolean equals = true;
      for (Method method : mAnnotationClass.getDeclaredMethods()) {
        Class<?> returnType = method.getReturnType();

        Object thisReturnedObject;
        try {
          thisReturnedObject = invoke(method);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(
              String.format("Failed to invoke method '%s' on annotation '%s'", method, mStubbed),
              e);
        }
        Object otherReturnedObject;
        try {
          otherReturnedObject = method.invoke(other);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(
              String.format("Failed to invoke method '%s' on annotation '%s'", method, other), e);
        }

        if (returnType.isArray()) {
          A[] thisArray = (A[]) thisReturnedObject;
          A[] otherArray = (A[]) otherReturnedObject;
          equals &= Arrays.equals(thisArray, otherArray);
        } else {
          equals &= thisReturnedObject.equals(otherReturnedObject);
        }
      }

      return equals;
    }
  }
}
