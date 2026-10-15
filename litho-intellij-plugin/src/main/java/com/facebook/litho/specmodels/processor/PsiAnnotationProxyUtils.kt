/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.specmodels.processor

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.util.PsiTreeUtil
import java.lang.annotation.Annotation
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.Arrays

/**
 * Makes a more robustly-featured Proxy for wrapping Annotations in Psi-land. This makes Enums
 * usable and implements methods like `hashCode`, which go unimplemented by Psi's default Annotation
 * implementation.
 */
object PsiAnnotationProxyUtils {

  // Cached reference to the Class<T> overload of findAnnotationInHierarchy, which Kotlin
  // cannot resolve due to overload ambiguity with the Set<String> overload in IntelliJ stubs.
  private val FIND_IN_HIERARCHY: Method by lazy {
    AnnotationUtil::class
        .java
        .getMethod(
            "findAnnotationInHierarchy",
            PsiModifierListOwner::class.java,
            Class::class.java,
        )
  }

  @JvmStatic
  fun <T : Annotation> findAnnotationInHierarchy(
      listOwner: PsiModifierListOwner,
      annotationClass: Class<T>,
  ): T? {
    @Suppress("UNCHECKED_CAST")
    val basicProxy: T =
        FIND_IN_HIERARCHY.invoke(null, listOwner, annotationClass) as? T ?: return null

    @Suppress("UNCHECKED_CAST")
    val biggerProxy: T =
        Proxy.newProxyInstance(
            annotationClass.classLoader,
            arrayOf<Class<*>>(annotationClass),
            AnnotationProxyInvocationHandler(basicProxy, listOwner, annotationClass),
        ) as T

    return biggerProxy
  }

  private class AnnotationProxyInvocationHandler<T : Annotation>(
      private val stubbed: T,
      private val listOwner: PsiModifierListOwner,
      private val annotationClass: Class<T>,
  ) : InvocationHandler {

    @Throws(IllegalAccessException::class)
    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
      return when (method.name) {
        "hashCode" -> {
          proxyHashCode()
        }
        "equals" -> {
          proxyEquals(args?.get(0))
        }
        "toString" -> {
          stubbed.toString()
        }
        else -> {
          invoke(method, args)
        }
      }
    }

    @Throws(IllegalAccessException::class)
    private fun invoke(method: Method): Any? {
      return invoke(method, null)
    }

    @Throws(IllegalAccessException::class)
    private fun invoke(method: Method, args: Array<Any?>?): Any? {
      val returnType: Class<*> = method.returnType
      if (returnType.isEnum) {
        val currentAnnotation =
            AnnotationUtil.findAnnotationInHierarchy(
                listOwner,
                setOf(annotationClass.canonicalName),
            )
        val declaredValue = currentAnnotation?.findAttributeValue(method.name) as? PsiExpression
        if (declaredValue == null) {
          return method.defaultValue
        }
        val identifier = PsiTreeUtil.getChildOfType(declaredValue, PsiIdentifier::class.java)
        if (identifier == null) {
          return method.defaultValue
        }
        @Suppress("UNCHECKED_CAST") val enumConstants = returnType.enumConstants as Array<Enum<*>>
        return enumConstants.first { it.name == identifier.text }
      }

      try {
        if (args == null) {
          return method.invoke(stubbed)
        }
        return method.invoke(stubbed, *args)
      } catch (e: InvocationTargetException) {
        return method.defaultValue
      }
    }

    /**
     * NOTE: Taken from Annotation.java. This is an approximation of the below Javadoc.
     *
     * Returns the hash code of this annotation, as defined below:
     *
     * The hash code of an annotation is the sum of the hash codes of its members (including those
     * with default values), as defined below:
     *
     * The hash code of an annotation member is (127 times the hash code of the member-name as
     * computed by [String.hashCode]) XOR the hash code of the member-value, as defined below:
     *
     * The hash code of a member-value depends on its type:
     * - The hash code of a primitive value `v` is equal to `WrapperType.valueOf(v).hashCode()`,
     *   where `WrapperType` is the wrapper type corresponding to the primitive type of `v` ([Byte],
     *   [Character], [Double], [Float], [Integer], [Long], [Short], or [Boolean]).
     * - The hash code of a string, enum, class, or annotation member-value `v` is computed as by
     *   calling `v.hashCode()`. (In the case of annotation member values, this is a recursive
     *   definition.)
     * - The hash code of an array member-value is computed by calling the appropriate overloading
     *   of [Arrays.hashCode] on the value. (There is one overloading for each primitive type, and
     *   one for object reference types.)
     *
     * @return the hash code of this annotation
     */
    private fun proxyHashCode(): Int {
      var hashCode = 0
      for (method in annotationClass.declaredMethods) {
        val returnedObject: Any?
        try {
          returnedObject = invoke(method)
        } catch (e: IllegalAccessException) {
          throw RuntimeException(
              "Failed to invoke method '$method' on annotation '$stubbed'",
              e,
          )
        }

        var memberHash = 127 * method.name.hashCode()
        val returnType: Class<*> = method.returnType
        if (returnType.isArray) {
          @Suppress("UNCHECKED_CAST") val array = returnedObject as Array<Any?>
          memberHash = memberHash xor Arrays.hashCode(array)
        } else {
          memberHash = memberHash xor returnedObject.hashCode()
        }

        hashCode += memberHash
      }

      return hashCode
    }

    // Approximates the Annotation.equals() javadoc's implementation
    private fun proxyEquals(other: Any?): Boolean {
      if (!annotationClass.isInstance(other)) {
        return false
      }
      var equals = true
      for (method in annotationClass.declaredMethods) {
        val returnType: Class<*> = method.returnType

        val thisReturnedObject: Any?
        try {
          thisReturnedObject = invoke(method)
        } catch (e: IllegalAccessException) {
          throw RuntimeException(
              "Failed to invoke method '$method' on annotation '$stubbed'",
              e,
          )
        }
        val otherReturnedObject: Any?
        try {
          otherReturnedObject = method.invoke(other)
        } catch (e: IllegalAccessException) {
          throw RuntimeException(
              "Failed to invoke method '$method' on annotation '$other'",
              e,
          )
        } catch (e: InvocationTargetException) {
          throw RuntimeException(
              "Failed to invoke method '$method' on annotation '$other'",
              e,
          )
        }

        if (returnType.isArray) {
          @Suppress("UNCHECKED_CAST") val thisArray = thisReturnedObject as Array<Any?>
          @Suppress("UNCHECKED_CAST") val otherArray = otherReturnedObject as Array<Any?>
          equals = equals and Arrays.equals(thisArray, otherArray)
        } else {
          equals = equals and (thisReturnedObject == otherReturnedObject)
        }
      }

      return equals
    }
  }
}
