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

import com.facebook.litho.annotations.Param;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiCodeFragment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiEllipsisType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeCodeFragment;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.changeSignature.CallerChooserBase;
import com.intellij.refactoring.changeSignature.ChangeSignatureDialogBase;
import com.intellij.refactoring.changeSignature.JavaChangeSignatureDialog;
import com.intellij.refactoring.changeSignature.JavaMethodDescriptor;
import com.intellij.refactoring.changeSignature.JavaParameterTableModel;
import com.intellij.refactoring.changeSignature.ParameterInfoImpl;
import com.intellij.refactoring.changeSignature.ParameterTableModelItemBase;
import com.intellij.refactoring.ui.JavaComboBoxVisibilityPanel;
import com.intellij.refactoring.ui.VisibilityPanelBase;
import com.intellij.refactoring.util.RefactoringMessageUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Consumer;
import com.intellij.util.ui.table.EditorTextFieldJBTableRowRenderer;
import com.intellij.util.ui.table.JBTableRow;
import com.intellij.util.ui.table.JBTableRowEditor;
import com.intellij.util.ui.table.JBTableRowRenderer;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog similar to the {@link JavaChangeSignatureDialog} with differences specific to Litho
 * OnEvent method:
 *
 * <ul>
 *   <li>Visibility is fixed
 *   <li>Return type is fixed
 *   <li>No default value option for parameters
 *   <li>No error on absence of default value
 *   <li>No additional tab for exceptions
 *   <li>No Preview and Refactor buttons
 * </ul>
 */
final class OnEventChangeSignatureDialog
    extends ChangeSignatureDialogBase<
        ParameterInfoImpl,
        PsiMethod,
        String,
        JavaMethodDescriptor,
        ParameterTableModelItemBase<ParameterInfoImpl>,
        JavaParameterTableModel> {

  private final Map<String, PsiParameter> nameToParameter = new HashMap<>();
  private PsiMethod newMethod;

  /** @param method is used to fill the chooser table. It will not be modified. */
  OnEventChangeSignatureDialog(Project project, PsiMethod method, PsiElement context) {
    super(project, new OnEventMethodDescriptor(method), false, context);
    // Save initial method parameters
    PsiParameter[] parameters = method.getParameterList().getParameters();
    for (PsiParameter parameter : parameters) {
      nameToParameter.put(parameter.getName(), parameter);
    }
  }

  @Nullable
  PsiMethod getMethod() {
    return newMethod;
  }

  @Override
  protected JPanel createParametersPanel(boolean hasTabsInDialog) {
    super.createParametersPanel(hasTabsInDialog);
    return ToolbarDecorator.createDecorator(myParametersList.getTable()).createPanel();
  }

  @Override
  protected boolean isListTableViewSupported() {
    return true;
  }

  @Override
  protected ParametersListTable createParametersListTable() {
    return new OnEventParametersListTable();
  }

  @Override
  protected LanguageFileType getFileType() {
    return StdFileTypes.JAVA;
  }

  @Override
  protected VisibilityPanelBase<String> createVisibilityControl() {
    return new JavaComboBoxVisibilityPanel();
  }

  @NotNull
  @Override
  protected JavaParameterTableModel createParametersInfoModel(JavaMethodDescriptor descriptor) {
    final PsiParameterList parameterList = descriptor.getMethod().getParameterList();
    return new JavaParameterTableModel(parameterList, myDefaultValueContext, this);
  }

  @Nullable
  @Override
  protected BaseRefactoringProcessor createRefactoringProcessor() {
    return null;
  }

  @Nullable
  @Override
  protected PsiCodeFragment createReturnTypeCodeFragment() {
    return null;
  }

  @Nullable
  @Override
  protected CallerChooserBase<PsiMethod> createCallerChooser(
      String s, Tree tree, Consumer<Set<PsiMethod>> consumer) {
    return null;
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[] {getOKAction()};
  }

  @Override
  protected void invokeRefactoring(BaseRefactoringProcessor processor) {
    closeOKAction();
  }

  @Override
  protected String calculateSignature() {
    return doCalculateSignature(myMethod.getMethod());
  }

  @Nullable
  @Override
  protected String validateAndCommitData() {
    return validateAndCreateMethod();
  }

  /**
   * @return either an error message or null if no errors are found. This method has side effect of
   *     creating a new PsiMethod with chosen fields and method name.
   */
  @Nullable
  private String validateAndCreateMethod() {
    String methodName = getMethodName();
    if (!PsiNameHelper.getInstance(myProject).isIdentifier(methodName)) {
      return RefactoringMessageUtil.getIncorrectIdentifierMessage(methodName);
    }

    final PsiElementFactory factory = JavaPsiFacade.getInstance(myProject).getElementFactory();
    final PsiMethod oldMethod = myMethod.getMethod();
    final PsiMethod newMethod = factory.createMethod(methodName, oldMethod.getReturnType());

    final List<ParameterTableModelItemBase<ParameterInfoImpl>> tableModelItems =
        myParametersTableModel.getItems();
    final PsiParameterList parameterList = newMethod.getParameterList();
    for (final ParameterTableModelItemBase<ParameterInfoImpl> item : tableModelItems) {
      final String parameterName = item.parameter.getName();
      if (!PsiNameHelper.getInstance(myProject).isIdentifier(parameterName)) {
        return RefactoringMessageUtil.getIncorrectIdentifierMessage(parameterName);
      }

      final PsiType parameterType;
      try {
        parameterType = ((PsiTypeCodeFragment) item.typeCodeFragment).getType();
      } catch (PsiTypeCodeFragment.TypeSyntaxException e) {
        return RefactoringBundle.message(
            "changeSignature.wrong.type.for.parameter",
            item.typeCodeFragment.getText(),
            parameterName);
      } catch (PsiTypeCodeFragment.NoTypeException e) {
        return RefactoringBundle.message(
            "changeSignature.no.type.for.parameter", "return", parameterName);
      }
      if (PsiTypesUtil.hasUnresolvedComponents(parameterType)) {
        return RefactoringBundle.message("changeSignature.cannot.resolve.parameter.type");
      }
      if (parameterType instanceof PsiEllipsisType) {
        return "Don`t use varargs type for " + parameterName;
      }

      PsiParameter parameter =
          getInitialMethodParameter(parameterName, parameterType.getPresentableText());
      if (parameter == null) {
        parameter = factory.createParameter(parameterName, parameterType);
        final PsiModifierList parameterModifierList = parameter.getModifierList();
        if (parameterModifierList == null) {
          continue;
        }
        parameterModifierList.addAnnotation(Param.class.getName());
      }
      parameterList.add(parameter);
    }
    // TODO T39429594: Context should be not removable
    // TODO T39429594: Extract createMethod logic into Utility class
    // TODO T39429594: Check for duplicate parameter names

    final PsiModifierList modifierList = newMethod.getModifierList();
    for (PsiElement modifier : oldMethod.getModifierList().getChildren()) {
      modifierList.add(modifier);
    }
    modifierList.setModifierProperty(PsiModifier.PACKAGE_LOCAL, true);

    this.newMethod = newMethod;

    return null;
  }

  private String doCalculateSignature(PsiMethod method) {
    // This method could be called when object has not been fully constructed
    if (nameToParameter == null) {
      return "";
    }
    final StringBuilder buffer = new StringBuilder();
    final PsiModifierList modifierList = method.getModifierList();

    // New line after annotation
    final PsiElement[] modifiers = modifierList.getChildren();
    for (int i = 0, size = modifiers.length; i < size; i++) {
      final PsiElement modifier = modifiers[i];
      if (modifier instanceof PsiWhiteSpace) {
        continue;
      }
      buffer.append(modifier.getText());
      if (i == 0 && modifier instanceof PsiAnnotation) {
        buffer.append("\n");
      } else {
        buffer.append(" ");
      }
    }

    final PsiType returnType = method.getReturnType();
    if (returnType != null) {
      buffer.append(returnType.getPresentableText()).append(" ");
    }
    buffer.append(getMethodName()).append("(");

    final int lineBreakIdx = buffer.lastIndexOf("\n");
    final String indent =
        StringUtil.repeatSymbol(
            ' ', lineBreakIdx >= 0 ? buffer.length() - lineBreakIdx - 1 : buffer.length());

    final List<ParameterTableModelItemBase<ParameterInfoImpl>> currentTableItems =
        myParametersTableModel.getItems();

    for (int i = 0; i < currentTableItems.size(); i++) {
      final ParameterTableModelItemBase<ParameterInfoImpl> item = currentTableItems.get(i);
      if (i > 0) {
        buffer.append(",");
        buffer.append("\n");
        buffer.append(indent);
      }
      final String itemName = item.parameter.getName();
      final String itemType = item.typeCodeFragment.getText();
      final PsiParameter parameter = getInitialMethodParameter(itemName, itemType);
      if (parameter != null) {
        for (PsiElement annotation : parameter.getAnnotations()) {
          buffer.append(annotation.getText()).append(" ");
        }
      } else {
        buffer.append("@").append(Param.class.getSimpleName()).append(" ");
      }
      buffer.append(itemType).append(" ").append(itemName);
    }

    buffer.append(")");

    return buffer.toString();
  }

  /** @return parameter from initial method if name and type match any, null otherwise. */
  @Nullable
  // Package-private to be accessed from the inner class.
  PsiParameter getInitialMethodParameter(String name, String type) {
    final PsiParameter parameter = nameToParameter.get(name);
    if (parameter == null) {
      return null;
    }
    if (parameter.getType().getPresentableText().equals(type)) {
      return parameter;
    }
    return null;
  }

  /** Java Method Descriptor that doesn't allow to modify return type or change visibility. */
  private static class OnEventMethodDescriptor extends JavaMethodDescriptor {
    OnEventMethodDescriptor(PsiMethod method) {
      super(method);
    }

    @Override
    public boolean canChangeVisibility() {
      return false;
    }

    @Override
    public ReadWriteOption canChangeReturnType() {
      return ReadWriteOption.None;
    }
  }

  /**
   * Table that has not-modifiable fields for initial parameters. Implementation mostly copied from
   * the {@link JavaChangeSignatureDialog}.
   */
  private class OnEventParametersListTable extends ParametersListTable {
    private final EditorTextFieldJBTableRowRenderer myRowRenderer =
        new EditorTextFieldJBTableRowRenderer(
            getProject(), OnEventChangeSignatureDialog.this.getFileType(), myDisposable) {
          @Override
          protected String getText(JTable table, int row) {
            ParameterTableModelItemBase<ParameterInfoImpl> item = getRowItem(row);
            final String typeText = item.typeCodeFragment.getText();
            final String separator =
                StringUtil.repeatSymbol(' ', getTypesMaxLength() - typeText.length() + 1);
            return " " + (typeText + separator + item.parameter.getName());
          }
        };

    int getTypesMaxLength() {
      int len = 0;
      for (ParameterTableModelItemBase<ParameterInfoImpl> item :
          myParametersTableModel.getItems()) {
        final String text = item.typeCodeFragment == null ? null : item.typeCodeFragment.getText();
        len = Math.max(len, text == null ? 0 : text.length());
      }
      return len;
    }

    @Override
    protected boolean isRowEditable(int row) {
      // If table parameter is an initial method parameter, than it's not editable.
      if (row == 0) {
        stopEditing();
      }
      final List<ParameterTableModelItemBase<ParameterInfoImpl>> currentTableItems =
          myParametersTableModel.getItems();
      if (row > currentTableItems.size()) {
        return true;
      }
      final ParameterInfoImpl parameterInfo = currentTableItems.get(row).parameter;
      final PsiParameter initialMethodParameter =
          getInitialMethodParameter(parameterInfo.getName(), parameterInfo.getTypeText());
      return initialMethodParameter == null;
    }

    @Override
    protected JBTableRowRenderer getRowRenderer(int row) {
      return myRowRenderer;
    }

    @NotNull
    @Override
    protected JBTableRowEditor getRowEditor(
        final ParameterTableModelItemBase<ParameterInfoImpl> item) {
      return new JBTableRowEditor() {
        private EditorTextField myTypeEditor;
        private EditorTextField myNameEditor;

        @Override
        public void prepareEditor(JTable table, int row) {
          setLayout(new BorderLayout());
          final Document document =
              PsiDocumentManager.getInstance(getProject()).getDocument(item.typeCodeFragment);
          myTypeEditor = new EditorTextField(document, getProject(), getFileType());
          myTypeEditor.addDocumentListener(mySignatureUpdater);
          myTypeEditor.setPreferredWidth(getTable().getWidth() / 2);
          myTypeEditor.addDocumentListener(new RowEditorChangeListener(0));
          add(createLabeledPanel("Type:", myTypeEditor), BorderLayout.WEST);

          myNameEditor = new EditorTextField(item.parameter.getName(), getProject(), getFileType());
          myNameEditor.addDocumentListener(mySignatureUpdater);
          myNameEditor.addDocumentListener(new RowEditorChangeListener(1));
          add(createLabeledPanel("Name:", myNameEditor), BorderLayout.CENTER);
        }

        @Override
        public JBTableRow getValue() {
          return column -> {
            switch (column) {
              case 0:
                return item.typeCodeFragment;
              case 1:
                return myNameEditor.getText().trim();
              case 2:
                return item.defaultValueCodeFragment;
              case 3:
                return false;
            }
            return null;
          };
        }

        @Override
        public JComponent getPreferredFocusedComponent() {
          return myTypeEditor.getFocusTarget();
        }

        @Override
        public JComponent[] getFocusableComponents() {
          return new JComponent[] {myTypeEditor.getFocusTarget(), myNameEditor.getFocusTarget()};
        }
      };
    }

    @Override
    protected boolean isRowEmpty(int row) {
      final ParameterInfoImpl parameter = getRowItem(row).parameter;
      return StringUtil.isEmpty(parameter.getName()) && StringUtil.isEmpty(parameter.getTypeText());
    }
  }
}
