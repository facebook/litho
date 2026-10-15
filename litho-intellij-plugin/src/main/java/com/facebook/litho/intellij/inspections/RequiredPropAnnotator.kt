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

package com.facebook.litho.intellij.inspections

import com.facebook.litho.annotations.RequiredProp
import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.DebounceEventLogger
import com.facebook.litho.specmodels.model.SpecModelValidationError
import com.facebook.litho.specmodels.processor.PsiAnnotationProxyUtils
import com.google.common.annotations.VisibleForTesting
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiReturnStatement
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import java.util.function.BiConsumer
import java.util.function.Function

/**
 * Annotator adds error to the `Component.create()` call in a statement, when this statement misses
 * required prop.
 */
class RequiredPropAnnotator
@VisibleForTesting
internal constructor(
    private val generatedClassResolver: Function<PsiMethodCallExpression, PsiClass?>,
) : Annotator {

  constructor() : this(GENERATED_CLASS_RESOLVER)

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    annotate(
        element,
        BiConsumer { missingRequiredProps, methodExpression ->
          val data = HashMap<String, String>()
          data[EventLogger.KEY_TYPE] = "required_prop"
          LOGGER.log(EventLogger.EVENT_ANNOTATOR, data)
          AnnotatorUtils.addError(
              holder,
              SpecModelValidationError(methodExpression, createErrorMessage(missingRequiredProps)),
          )
        },
        generatedClassResolver,
    )
  }

  companion object {
    private val LOGGER: EventLogger = DebounceEventLogger(60 * 60_000)

    @JvmField
    val GENERATED_CLASS_RESOLVER: Function<PsiMethodCallExpression, PsiClass?> =
        Function { methodCallExpression ->
          val psiMethod = methodCallExpression.resolveMethod() ?: return@Function null
          val topLevelClass = PsiUtil.getTopLevelClass(psiMethod)
          if (LithoPluginUtils.isGeneratedClass(topLevelClass)) {
            return@Function topLevelClass
          }
          null
        }

    @JvmStatic
    fun createErrorMessage(missingRequiredProps: Collection<String>): String {
      return "The following props are not marked as optional and were not supplied: " +
          StringUtil.join(missingRequiredProps, ", ")
    }

    /**
     * If element is a [PsiStatement][com.intellij.psi.PsiStatement] with `Component.create()` call,
     * finds missing required props for it.
     *
     * @param element element to verify
     * @param errorHandler handles a list of missing required props and reference to the
     *   `Component.create()` call in the statement
     * @param generatedClassResolver returns generated Litho class, or null if the provided method
     *   doesn't belong to any
     */
    @JvmStatic
    fun annotate(
        element: PsiElement,
        errorHandler: BiConsumer<Collection<String>, PsiReferenceExpression>,
        generatedClassResolver: Function<PsiMethodCallExpression, PsiClass?>,
    ) {
      if (element is PsiDeclarationStatement) {
        for (declaredElement in element.declaredElements) {
          if (declaredElement is PsiVariable) {
            handleIfMethodCall(declaredElement.initializer, errorHandler, generatedClassResolver)
          }
        }
      } else if (element is PsiExpressionStatement) {
        handleIfMethodCall(element.expression, errorHandler, generatedClassResolver)
      } else if (element is PsiReturnStatement) {
        handleIfMethodCall(element.returnValue, errorHandler, generatedClassResolver)
      }
    }

    private fun handleIfMethodCall(
        expression: PsiExpression?,
        errorHandler: BiConsumer<Collection<String>, PsiReferenceExpression>,
        generatedClassResolver: Function<PsiMethodCallExpression, PsiClass?>,
    ) {
      if (expression is PsiMethodCallExpression) {
        handleMethodCall(expression, HashSet(), errorHandler, generatedClassResolver)
      }
    }

    private fun handleMethodCall(
        currentMethodCall: PsiMethodCallExpression,
        methodNamesCalled: MutableSet<String>,
        errorHandler: BiConsumer<Collection<String>, PsiReferenceExpression>,
        generatedClassResolver: Function<PsiMethodCallExpression, PsiClass?>,
    ) {
      val methodExpression = currentMethodCall.methodExpression
      methodNamesCalled.add(methodExpression.referenceName.orEmpty())

      // Assumption to find next method in a call chain
      val nextMethodCall =
          PsiTreeUtil.getChildOfType(methodExpression, PsiMethodCallExpression::class.java)
      if (nextMethodCall != null) {
        handleMethodCall(nextMethodCall, methodNamesCalled, errorHandler, generatedClassResolver)
      } else if ("create" == methodExpression.referenceName) {
        // Finish call chain
        val generatedCls = generatedClassResolver.apply(currentMethodCall)
        if (generatedCls != null) {
          val missingRequiredProps = collectMissingRequiredProps(generatedCls, methodNamesCalled)
          if (missingRequiredProps.isNotEmpty()) {
            errorHandler.accept(missingRequiredProps, methodExpression)
          }
        }
      }

      val argumentList = currentMethodCall.argumentList
      for (argument in argumentList.expressions) {
        handleIfMethodCall(argument, errorHandler, generatedClassResolver)
      }
    }

    /**
     * @param generatedCls class containing inner Builder class with methods annotated as
     *   [RequiredProp] marking which Props are required for this class.
     * @param methodNames methods from the given class.
     * @return names of the generatedCls required props, that were not set after all methodNames
     *   calls.
     */
    private fun collectMissingRequiredProps(
        generatedCls: PsiClass,
        methodNames: Collection<String>,
    ): Collection<String> {
      val propToMethods = getRequiredPropsToMethodNames(generatedCls)
      if (propToMethods.isEmpty()) {
        return emptySet()
      }
      val missingRequiredProps = HashSet(propToMethods.keys)
      val methodToProp = inverse(propToMethods)
      for (methodName in methodNames) {
        val prop = methodToProp[methodName]
        if (prop != null) {
          missingRequiredProps.remove(prop)
        }
      }
      return missingRequiredProps
    }

    /**
     * @param generatedCls class containing inner Builder class with methods annotated as
     *   [RequiredProp] marking which Props are required for this class.
     * @return mapping from required prop name to the method names setting this prop. Return empty
     *   map if the provided class doesn't contain Builder inner class, or doesn't have methods with
     *   [RequiredProp] annotation.
     */
    private fun getRequiredPropsToMethodNames(generatedCls: PsiClass): Map<String, Set<String>> {
      val builder = generatedCls.findInnerClassByName("Builder", false) ?: return emptyMap()
      val propToMethods = HashMap<String, MutableSet<String>>()
      for (method in builder.methods) {
        @Suppress("UNCHECKED_CAST")
        val requiredProp =
            PsiAnnotationProxyUtils.findAnnotationInHierarchy(
                method,
                RequiredProp::class.java as Class<java.lang.annotation.Annotation>,
            ) as RequiredProp? ?: continue
        val methodNames = propToMethods.getOrPut(requiredProp.value) { HashSet() }
        methodNames.add(method.name)
      }
      return propToMethods
    }

    private fun <K, V> inverse(keyToValue: Map<K, Collection<V>>): Map<V, K> {
      val inversed = HashMap<V, K>()
      for ((key, values) in keyToValue) {
        for (value in values) {
          inversed[value] = key
        }
      }
      return inversed
    }
  }
}
