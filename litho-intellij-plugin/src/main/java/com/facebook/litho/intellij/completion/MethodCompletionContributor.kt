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

package com.facebook.litho.intellij.completion

import com.facebook.litho.intellij.LithoClassNames
import com.facebook.litho.intellij.PsiSearchUtils
import com.facebook.litho.intellij.completion.CompletionUtils.METHOD_ANNOTATION
import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.LithoLoggerProvider
import com.facebook.litho.intellij.services.TemplateService
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import java.util.HashMap
import java.util.function.Predicate

class MethodCompletionContributor : CompletionContributor() {

  init {
    extend(CompletionType.BASIC, METHOD_ANNOTATION, LayoutSpecMethodAnnotationsProvider.INSTANCE)
    extend(CompletionType.BASIC, METHOD_ANNOTATION, MountSpecMethodAnnotationsProvider.INSTANCE)
    extend(CompletionType.BASIC, METHOD_ANNOTATION, SectionSpecMethodAnnotationsProvider.INSTANCE)
  }

  companion object {
    @JvmStatic
    internal fun addMethodCompletions(
        position: PsiElement,
        condition: Predicate<PsiClass>,
        annotationFQNs: Collection<String>,
        result: CompletionResultSet,
    ) {
      addMethodCompletions(position, condition, null, annotationFQNs, result)
    }

    @JvmStatic
    internal fun addMethodCompletions(
        position: PsiElement,
        condition: Predicate<PsiClass>,
        prefix: String?,
        annotationFqns: Collection<String>,
        result: CompletionResultSet,
    ) {
      val maybeCls = CompletionUtils.findFirstParent(position, condition)
      if (!maybeCls.isPresent) {
        return
      }

      val project = position.project
      for (annotationFQN in annotationFqns) {
        val lookup =
            PrioritizedLookupElement.withPriority(
                createMethodLookup(prefix, annotationFQN, project, maybeCls.get()),
                Int.MAX_VALUE.toDouble(),
            )
        result.addElement(lookup)
      }
    }

    private fun createMethodLookup(
        prefix: String?,
        annotationFqn: String,
        project: Project,
        specClass: PsiClass,
    ): LookupElement {
      val annotation = StringUtil.getShortName(annotationFqn)
      val templateService = project.getService(TemplateService::class.java)
      val psiMethod = templateService.getMethodTemplate(prefix, annotation, project)
      if (psiMethod != null) {
        return createMethodLookup(
            psiMethod,
            listOf(psiMethod),
            specClass,
            getOrCreateClass(annotationFqn, project),
            "@$annotation",
            { log(annotation) },
        )
      }
      return SpecLookupElement.create(annotationFqn, project) { _, _ -> log(annotation) }
    }

    @JvmStatic
    internal fun getOrCreateClass(qualifiedClassName: String, project: Project): PsiClass {
      var cls: PsiClass? = PsiSearchUtils.getInstance().findClass(project, qualifiedClassName)
      if (cls == null) {
        cls =
            JavaPsiFacade.getElementFactory(project)
                .createClass(LithoClassNames.shortName(qualifiedClassName))
      }
      return cls
    }

    @JvmStatic
    internal fun createMethodLookup(
        mainMethod: PsiMethod,
        methods: List<PsiMethod>,
        specClass: PsiClass,
        documentationCls: PsiClass,
        lookupString: String,
        postProcessor: Runnable,
    ): LookupElementBuilder {
      return LookupElementBuilder.createWithIcon(mainMethod)
          .withPresentableText(lookupString)
          .withLookupString(lookupString)
          .withCaseSensitivity(false)
          .withInsertHandler { context, _ ->
            handleInsert(methods, context, specClass)
            postProcessor.run()
          }
          .appendTailText(getReadableParameterLists(mainMethod), true)
          .withTypeText("Litho", true)
          .withPsiElement(documentationCls)
    }

    private fun log(annotation: String) {
      val data = HashMap<String, String>()
      data[EventLogger.KEY_TARGET] = EventLogger.VALUE_COMPLETION_TARGET_METHOD
      data[EventLogger.KEY_CLASS] = annotation
      LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_COMPLETION, data)
    }

    private fun handleInsert(
        methods: List<PsiMethod>,
        insertionContext: InsertionContext,
        specClass: PsiClass,
    ) {
      // Remove lookup string. As in the JavaGenerateMemberCompletionContributor
      insertionContext.document.deleteString(
          insertionContext.startOffset - 1,
          insertionContext.tailOffset,
      )
      insertionContext.commitDocument()

      MethodGenerateHandler(
          methods,
          specClass,
          insertionContext.document,
          insertionContext.project,
      )
          .invoke(insertionContext.project, insertionContext.editor, insertionContext.file)
    }

    private fun getReadableParameterLists(psiMethod: PsiMethod): String {
      return psiMethod.parameterList.parameters.joinToString(", ", "(", ")") { psiParam ->
        "${psiParam.type.presentableText} ${psiParam.name}"
      }
    }
  }
}
