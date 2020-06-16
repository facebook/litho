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

package com.facebook.litho.intellij.toolwindows;

import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.facebook.litho.specmodels.model.UpdateStateMethod;
import com.facebook.litho.specmodels.model.WorkingRangeMethodModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.Icon;
import org.jetbrains.annotations.Nullable;

/** Factory with helper methods for creating {@link TreeElement}s for Litho classes. */
class SpecTreeElementFactory {

  static StructureViewTreeElement createTree(SpecModel model) {
    List<TreeElement> children = new ArrayList<>(1);
    add(
        children,
        "Props",
        model.getProps().stream().map(SpecTreeElementFactory::create).collect(Collectors.toList()));
    add(
        children,
        "States",
        model.getStateValues().stream()
            .map(SpecTreeElementFactory::create)
            .collect(Collectors.toList()));
    add(
        children,
        "Lifecycle Methods",
        model.getDelegateMethods().stream()
            .map(SpecTreeElementFactory::createDelegate)
            .collect(Collectors.toList()));
    add(
        children,
        "Events",
        model.getEventMethods().stream()
            .map(SpecTreeElementFactory::createEvent)
            .collect(Collectors.toList()));
    add(
        children,
        "Triggers",
        model.getTriggerMethods().stream()
            .map(SpecTreeElementFactory::createEvent)
            .collect(Collectors.toList()));
    final List<SpecMethodModel<UpdateStateMethod, Void>> updateStateMethods =
        new ArrayList<>(model.getUpdateStateMethods());
    updateStateMethods.addAll(model.getUpdateStateWithTransitionMethods());
    add(
        children,
        "State Updates",
        updateStateMethods.stream()
            .map(SpecTreeElementFactory::create)
            .collect(Collectors.toList()));
    add(
        children,
        "Working Ranges",
        model.getWorkingRangeMethods().stream()
            .map(SpecTreeElementFactory::create)
            .collect(Collectors.toList()));
    return new PresentableTreeElement(model.getSpecName(), model.getRepresentedObject(), children);
  }

  private static void add(List<TreeElement> target, String name, List<TreeElement> children) {
    //    if (!children.isEmpty()) {
    target.add(create(name, children));
    //    }
  }

  private static TreeElement create(String name, List<TreeElement> children) {
    return new PresentableTreeElement(name, null, children);
  }

  private static TreeElement create(PropModel propModel) {
    return new PresentableTreeElement(
        (propModel.isOptional() ? "" : "(R) ") + getName(propModel),
        propModel.getRepresentedObject(),
        Collections.emptyList());
  }

  private static TreeElement create(StateParamModel stateModel) {
    return new PresentableTreeElement(
        getName(stateModel), stateModel.getRepresentedObject(), Collections.emptyList());
  }

  private static TreeElement create(SpecMethodModel<?, ?> methodModel) {
    return new PresentableTreeElement(
        methodModel.name.toString(), methodModel.representedObject, Collections.emptyList());
  }

  private static TreeElement createEvent(SpecMethodModel<?, EventDeclarationModel> methodModel) {
    return new PresentableTreeElement(
        methodModel.name.toString()
            + " (@"
            + StringUtil.getShortName(methodModel.typeModel.name.simpleName())
            + ")",
        methodModel.representedObject,
        Collections.emptyList());
  }

  private static TreeElement createDelegate(SpecMethodModel<?, ?> methodModel) {
    return new PresentableTreeElement(
        methodModel.name.toString()
            + " (@"
            + StringUtil.getShortName(
                methodModel.annotations.get(0).annotationType().getSimpleName())
            + ")",
        methodModel.representedObject,
        Collections.emptyList());
  }

  private static TreeElement create(WorkingRangeMethodModel workingRangeMethodModel) {
    List<TreeElement> children = new ArrayList<>(2);
    if (workingRangeMethodModel.enteredRangeModel != null) {
      children.add(create(workingRangeMethodModel.enteredRangeModel));
    }
    if (workingRangeMethodModel.exitedRangeModel != null) {
      children.add(create(workingRangeMethodModel.exitedRangeModel));
    }
    return create(workingRangeMethodModel.name, children);
  }

  private static String getName(MethodParamModel paramModel) {
    return paramModel.getName();
  }

  private static class PresentableTreeElement
      implements StructureViewTreeElement, ItemPresentation {
    private final String presentation;
    private final Object value;
    private final TreeElement[] children;

    <T extends TreeElement> PresentableTreeElement(
        String name, @Nullable Object value, List<T> children) {
      this.presentation = name;
      this.value = value == null ? new Object() : value;
      this.children = children.toArray(new TreeElement[0]);
    }

    @Override
    public Object getValue() {
      return value;
    }

    @Override
    public ItemPresentation getPresentation() {
      return this;
    }

    @Override
    public TreeElement[] getChildren() {
      return children;
    }

    @Override
    public void navigate(boolean requestFocus) {
      if (canNavigate()) {
        ((Navigatable) value).navigate(requestFocus);
      }
    }

    @Override
    public boolean canNavigate() {
      return value instanceof Navigatable && ((Navigatable) value).canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
      return canNavigate();
    }

    @Override
    public Icon getIcon(boolean open) {
      if (value instanceof Iconable) {
        return ((Iconable) value).getIcon(Iconable.ICON_FLAG_READ_STATUS);
      }
      return null;
    }

    @Nullable
    @Override
    public String getPresentableText() {
      return presentation;
    }

    @Nullable
    @Override
    public String getLocationString() {
      return null;
    }
  }
}
