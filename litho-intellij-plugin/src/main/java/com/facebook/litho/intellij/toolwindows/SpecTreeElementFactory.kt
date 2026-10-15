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

package com.facebook.litho.intellij.toolwindows

import com.facebook.litho.specmodels.model.EventDeclarationModel
import com.facebook.litho.specmodels.model.MethodParamModel
import com.facebook.litho.specmodels.model.PropModel
import com.facebook.litho.specmodels.model.SpecMethodModel
import com.facebook.litho.specmodels.model.SpecModel
import com.facebook.litho.specmodels.model.UpdateStateMethod
import com.facebook.litho.specmodels.model.WorkingRangeMethodModel
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.pom.Navigatable
import java.util.stream.Collectors
import javax.swing.Icon

/** Factory with helper methods for creating [TreeElement]s for Litho classes. */
internal object SpecTreeElementFactory {

  @JvmStatic
  fun createTree(model: SpecModel): StructureViewTreeElement {
    val children = ArrayList<TreeElement>(1)
    add(
        children,
        "Props",
        model.props
            .stream()
            .map { propModel -> createProp(propModel) }
            .collect(Collectors.toList()),
    )
    add(
        children,
        "States",
        model.stateValues
            .stream()
            .map { paramModel -> createParam(paramModel) }
            .collect(Collectors.toList()),
    )
    add(
        children,
        "Lifecycle Methods",
        model.delegateMethods
            .stream()
            .map { methodModel -> createDelegate(methodModel) }
            .collect(Collectors.toList()),
    )
    add(
        children,
        "Events",
        model.eventMethods
            .stream()
            .map { methodModel -> createEvent(methodModel) }
            .collect(Collectors.toList()),
    )
    add(
        children,
        "Triggers",
        model.triggerMethods
            .stream()
            .map { methodModel -> createEvent(methodModel) }
            .collect(Collectors.toList()),
    )
    val updateStateMethods =
        ArrayList<SpecMethodModel<UpdateStateMethod, Void>>(model.updateStateMethods)
    updateStateMethods.addAll(model.updateStateWithTransitionMethods)
    add(
        children,
        "State Updates",
        updateStateMethods
            .stream()
            .map { methodModel -> createMethod(methodModel) }
            .collect(Collectors.toList()),
    )
    add(
        children,
        "Working Ranges",
        model.workingRangeMethods
            .stream()
            .map { workingRangeModel -> createWorkingRange(workingRangeModel) }
            .collect(Collectors.toList()),
    )
    return PresentableTreeElement(model.specName, model.representedObject, children)
  }

  private fun add(target: MutableList<TreeElement>, name: String, children: List<TreeElement>) {
    //    if (!children.isEmpty()) {
    target.add(create(name, children))
    //    }
  }

  private fun create(name: String, children: List<TreeElement>): TreeElement {
    return PresentableTreeElement(name, null, children)
  }

  private fun createProp(propModel: PropModel): TreeElement {
    return PresentableTreeElement(
        (if (propModel.isOptional) "" else "(R) ") + getName(propModel),
        propModel.representedObject,
        emptyList(),
    )
  }

  private fun createParam(model: MethodParamModel): TreeElement {
    return PresentableTreeElement(getName(model), model.representedObject, emptyList())
  }

  private fun createMethod(methodModel: SpecMethodModel<*, *>): TreeElement {
    return PresentableTreeElement(
        methodModel.name.toString(),
        methodModel.representedObject,
        emptyList(),
    )
  }

  private fun createEvent(methodModel: SpecMethodModel<*, EventDeclarationModel>): TreeElement {
    return PresentableTreeElement(
        methodModel.name.toString() +
            " (@" +
            StringUtil.getShortName(methodModel.typeModel!!.reflectionName) +
            ")",
        methodModel.representedObject,
        emptyList(),
    )
  }

  private fun createDelegate(methodModel: SpecMethodModel<*, *>): TreeElement {
    return PresentableTreeElement(
        methodModel.name.toString() +
            " (@" +
            StringUtil.getShortName(methodModel.annotations[0].annotationClass.java.simpleName) +
            ")",
        methodModel.representedObject,
        emptyList(),
    )
  }

  private fun createWorkingRange(workingRangeMethodModel: WorkingRangeMethodModel): TreeElement {
    val children = ArrayList<TreeElement>(2)
    val enteredRangeModel = workingRangeMethodModel.enteredRangeModel
    if (enteredRangeModel != null) {
      children.add(createMethod(enteredRangeModel))
    }
    val exitedRangeModel = workingRangeMethodModel.exitedRangeModel
    if (exitedRangeModel != null) {
      children.add(createMethod(exitedRangeModel))
    }
    return create(workingRangeMethodModel.name, children)
  }

  private fun getName(paramModel: MethodParamModel): String {
    return paramModel.name
  }

  private class PresentableTreeElement(name: String, value: Any?, children: List<out TreeElement>) :
      StructureViewTreeElement, ItemPresentation {

    private val presentation: String = name
    private val value: Any = value ?: Any()
    private val children: Array<TreeElement> = children.toTypedArray()

    override fun getValue(): Any {
      return value
    }

    override fun getPresentation(): ItemPresentation {
      return this
    }

    override fun getChildren(): Array<TreeElement> {
      return children
    }

    override fun navigate(requestFocus: Boolean) {
      if (canNavigate()) {
        (value as Navigatable).navigate(requestFocus)
      }
    }

    override fun canNavigate(): Boolean {
      return value is Navigatable && (value as Navigatable).canNavigate()
    }

    override fun canNavigateToSource(): Boolean {
      return canNavigate()
    }

    override fun getIcon(open: Boolean): Icon? {
      if (value is Iconable) {
        return (value as Iconable).getIcon(Iconable.ICON_FLAG_READ_STATUS)
      }
      return null
    }

    override fun getPresentableText(): String? {
      return presentation
    }

    override fun getLocationString(): String? {
      return null
    }
  }
}
