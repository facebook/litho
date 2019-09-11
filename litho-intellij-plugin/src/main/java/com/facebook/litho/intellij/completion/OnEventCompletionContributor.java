/*
 * Copyright 2019-present Facebook, Inc.
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
package com.facebook.litho.intellij.completion;

import static com.intellij.patterns.StandardPatterns.or;

import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import java.util.Collections;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

/** Contributor suggests completion for the Click event in the Litho Spec. */
public class OnEventCompletionContributor extends CompletionContributor {

  public OnEventCompletionContributor() {
    extend(
        CompletionType.BASIC,
        or(annotationInClass(), annotationAboveMethod()),
        typeCompletionProvider());
  }

  private static ElementPattern<? extends PsiElement> annotationAboveMethod() {
    // PsiIdentifier -> PsiJavaCodeReference -> PsiAnnotation -> PsiModifierList -> PsiMethod
    return PlatformPatterns.psiElement(PsiIdentifier.class)
        .withSuperParent(2, PsiAnnotation.class)
        .withSuperParent(4, PsiMethod.class)
        .withLanguage(JavaLanguage.INSTANCE);
  }

  private static ElementPattern<? extends PsiElement> annotationInClass() {
    // PsiIdentifier -> PsiJavaCodeReference -> PsiAnnotation -> PsiModifierList -> PsiClass
    return PlatformPatterns.psiElement(PsiIdentifier.class)
        .withSuperParent(2, PsiAnnotation.class)
        .withSuperParent(4, PsiClass.class)
        .withLanguage(JavaLanguage.INSTANCE);
  }

  private static CompletionProvider<CompletionParameters> typeCompletionProvider() {
    return new CompletionProvider<CompletionParameters>() {
      @Override
      protected void addCompletions(
          @NotNull CompletionParameters parameters,
          ProcessingContext context,
          @NotNull CompletionResultSet result) {
        PsiElement element = parameters.getPosition();
        PsiElement parentElement = PsiTreeUtil.findFirstParent(element, PsiClass.class::isInstance);
        if (parentElement == null) {
          return;
        }
        PsiClass lithoSpecCls = (PsiClass) parentElement;
        if (!LithoPluginUtils.isLithoSpec(lithoSpecCls)) {
          return;
        }
        PsiClass clickEventCls =
            getOrCreateClass(lithoSpecCls.getProject(), LithoClassNames.CLICK_EVENT_CLASS_NAME);
        result.addElement(
            createMethodLookup(
                OnEventGenerateUtils.createOnEventMethod(
                    lithoSpecCls, clickEventCls, Collections.emptyList()),
                lithoSpecCls,
                OnEventGenerateUtils.createOnEventLookupString(clickEventCls)));
      }
    };
  }

  private static PsiClass getOrCreateClass(Project project, String qualifiedClassName) {
    PsiClass cls = PsiSearchUtils.findClass(project, qualifiedClassName);
    if (cls == null) {
      cls =
          JavaPsiFacade.getElementFactory(project)
              .createClass(LithoClassNames.shortName(qualifiedClassName));
    }
    return cls;
  }

  private static LookupElementBuilder createMethodLookup(
      PsiMethod method, PsiClass parentClass, String lookupString) {
    Icon icon = method.getIcon(Iconable.ICON_FLAG_VISIBILITY);
    return LookupElementBuilder.create(method)
        .withPresentableText(lookupString)
        .withLookupString(lookupString)
        .withCaseSensitivity(false)
        .withInsertHandler(getOnEventInsertHandler(method))
        .appendTailText(" {...}", true)
        .withTypeText(getTypeText(parentClass))
        .withIcon(icon);
  }

  /** Creates handler to insert given method in the lookup element insertion context. */
  private static InsertHandler<LookupElement> getOnEventInsertHandler(PsiMethod method) {
    return (insertionContext, item) -> {
      // Remove lookup string. As in the JavaGenerateMemberCompletionContributor
      insertionContext
          .getDocument()
          .deleteString(insertionContext.getStartOffset() - 1, insertionContext.getTailOffset());
      insertionContext.commitDocument();

      // Insert generation infos
      new MethodGenerateHandler(method)
          .invoke(
              insertionContext.getProject(),
              insertionContext.getEditor(),
              insertionContext.getFile());

      LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_ON_EVENT_COMPLETION);

      LithoPluginUtils.getFirstLayoutSpec(insertionContext.getFile())
          .ifPresent(ComponentGenerateUtils::updateLayoutComponent);
    };
  }

  private static String getTypeText(PsiClass parentClass) {
    return LithoPluginUtils.hasLithoSectionAnnotation(parentClass) ? "SectionSpec" : "LayoutSpec";
  }
}
