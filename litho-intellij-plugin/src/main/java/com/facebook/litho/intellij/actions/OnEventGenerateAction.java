/*
 * Copyright 2004-present Facebook, Inc.
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
package com.facebook.litho.intellij.actions;

import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.GenerateMembersHandlerBase;
import com.intellij.codeInsight.generation.GenerationInfo;
import com.intellij.codeInsight.generation.PsiGenerationInfo;
import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.ide.util.TreeJavaClassChooserDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/** Generates a method handling Litho event. https://fblitho.com/docs/events-overview */
public class OnEventGenerateAction extends BaseGenerateAction {
  public OnEventGenerateAction() {
    super(new OnEventGenerateHandler());
  }

  @Override
  public void update(AnActionEvent e) {
    // Applies visibility of the Generate Action group
    super.update(e);
    final PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
    if (!LithoPluginUtils.isLithoSpec(file)) {
      e.getPresentation().setEnabledAndVisible(false);
    }
  }

  static class OnEventGenerateHandler extends GenerateMembersHandlerBase {
    private static final String CONTEXT_PARAMETER_NAME = "c";

    OnEventGenerateHandler() {
      super("");
    }

    /** @return method based on user choice. */
    @Override
    protected ClassMember[] chooseOriginalMembers(PsiClass aClass, Project project) {
      // Choose event to generate method for
      final TreeJavaClassChooserDialog chooseEventDialog =
          new TreeJavaClassChooserDialog(
              "Choose Event",
              project,
              GlobalSearchScope.allScope(project),
              LithoPluginUtils::isEvent,
              aClass /* Any initial class */);
      chooseEventDialog.show();
      final PsiClass eventClass = chooseEventDialog.getSelected();
      if (eventClass == null) {
        return ClassMember.EMPTY_ARRAY;
      }

      final List<PsiParameter> propsAndStates =
          LithoPluginUtils.getPsiParameterStream(null, aClass.getMethods())
              .filter(LithoPluginUtils::isPropOrState)
              .collect(Collectors.toList());

      final PsiMethod onEventMethod = createOnEventMethod(aClass, eventClass, propsAndStates);

      final OnEventChangeSignatureDialog onEventMethodSignatureChooser =
          new OnEventChangeSignatureDialog(project, onEventMethod, aClass);
      onEventMethodSignatureChooser.show();
      final PsiMethod customMethod = onEventMethodSignatureChooser.getMethod();

      if (customMethod == null) {
        return ClassMember.EMPTY_ARRAY;
      }

      addComment(aClass, customMethod);

      return new ClassMember[] {new PsiMethodMember(customMethod)};
    }

    /** Adds comment to the given method "// An event handler MySpec.onClickEvent(c, param) */
    private static void addComment(PsiClass contextClass, PsiMethod method) {
      final Project project = contextClass.getProject();
      final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

      final StringBuilder builder =
          new StringBuilder("// An event handler ")
              .append(LithoPluginUtils.getLithoComponentNameFromSpec(contextClass.getName()))
              .append(".")
              .append(method.getName())
              .append("(")
              .append(CONTEXT_PARAMETER_NAME);
      for (PsiParameter parameter : method.getParameterList().getParameters()) {
        if (LithoPluginUtils.isParam(parameter)) {
          builder.append(", ").append(parameter.getName());
        }
      }

      builder.append(")");
      final PsiComment comment = factory.createCommentFromText(builder.toString(), method);
      method.addBefore(comment, method.getModifierList());
    }

    private static PsiMethod createOnEventMethod(
        PsiClass contextClass, PsiClass eventClass, Collection<PsiParameter> additionalParameters) {

      final Project project = contextClass.getProject();
      final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

      final PsiType methodReturnType = PsiType.VOID;
      final String methodName = "on" + eventClass.getName();
      final PsiMethod method = factory.createMethod(methodName, methodReturnType, contextClass);

      final PsiParameterList parameterList = method.getParameterList();
      final PsiParameter contextParameter =
          factory.createParameter(
              CONTEXT_PARAMETER_NAME,
              PsiType.getTypeByName(
                  LithoClassNames.COMPONENT_CONTEXT_CLASS_NAME,
                  project,
                  GlobalSearchScope.allScope(project)),
              contextClass);
      parameterList.add(contextParameter);

      final PsiField[] eventFields = eventClass.getFields();
      for (PsiField field : eventFields) {
        final String fieldName = field.getName();
        if (fieldName == null) {
          continue;
        }
        final PsiParameter parameter = factory.createParameter(fieldName, field.getType());
        final PsiModifierList parameterModifierList = parameter.getModifierList();
        if (parameterModifierList == null) {
          continue;
        }
        parameterModifierList.addAnnotation(LithoClassNames.FROM_EVENT_ANNOTATION_NAME);
        parameterList.add(parameter);
      }
      for (PsiParameter parameter : additionalParameters) {
        parameterList.add(parameter);
      }

      final PsiModifierList methodModifierList = method.getModifierList();
      methodModifierList.addAnnotation(
          LithoClassNames.ON_EVENT_ANNOTATION_NAME
              + "("
              + eventClass.getQualifiedName()
              + ".class)");

      methodModifierList.setModifierProperty(PsiModifier.PACKAGE_LOCAL, true);
      methodModifierList.setModifierProperty(PsiModifier.STATIC, true);

      return method;
    }

    @Override
    protected GenerationInfo[] generateMemberPrototypes(PsiClass psiClass, ClassMember classMember)
        throws IncorrectOperationException {
      return generateMemberPrototypes(psiClass, new ClassMember[] {classMember})
          .toArray(GenerationInfo.EMPTY_ARRAY);
    }

    /** @return a list of objects to insert into generated code. */
    @NotNull
    @Override
    protected List<? extends GenerationInfo> generateMemberPrototypes(
        PsiClass aClass, ClassMember[] members) throws IncorrectOperationException {
      final List<GenerationInfo> prototypes = new ArrayList<>();
      for (ClassMember member : members) {
        if (member instanceof PsiMethodMember) {
          PsiMethodMember methodMember = (PsiMethodMember) member;
          prototypes.add(new PsiGenerationInfo<>(methodMember.getElement()));
        }
      }
      return prototypes;
    }

    @Override
    protected ClassMember[] getAllOriginalMembers(PsiClass psiClass) {
      return ClassMember.EMPTY_ARRAY;
    }
  }
}
