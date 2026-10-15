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

package com.facebook.litho.intellij.services

import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.DebounceEventLogger
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import java.io.IOException
import java.io.InputStream
import org.jdom.Element
import org.jdom.JDOMException

class TemplateService : Disposable {

  private val templates = HashMap<String, PsiMethod>()

  fun getMethodTemplate(prefix: String?, targetName: String, project: Project): PsiMethod? {
    if (prefix != null) {
      val templateFromFullName = getMethodTemplate("$prefix.$targetName", project)
      if (templateFromFullName != null) {
        return templateFromFullName
      }
    }

    return getMethodTemplate(targetName, project)
  }

  fun getMethodTemplate(targetName: String, project: Project): PsiMethod? {
    val savedTemplate = templates[targetName]
    if (savedTemplate != null) {
      return savedTemplate
    }

    val template = readMethodTemplate(targetName, project)
    if (template != null) {
      templates.putIfAbsent(targetName, template)
    } else {
      val metadata = HashMap<String, String>()
      metadata[EventLogger.KEY_CLASS] = "TemplateService"
      metadata[EventLogger.KEY_TYPE] = targetName
      LOGGER.log(EventLogger.EVENT_ERROR, metadata)
    }
    return template
  }

  override fun dispose() {
    templates.clear()
  }

  companion object {
    private const val TAG_ROOT = "templateSet"
    private val LOGGER: EventLogger = DebounceEventLogger(60_000)

    private fun readMethodTemplate(targetName: String, project: Project): PsiMethod? {
      // TemplateSettings#loadDefaultLiveTemplates
      val stream =
          TemplateService::class.java.classLoader.getResourceAsStream("methodTemplates/methods.xml")
              ?: return null
      val element = stream.use { load(it) } ?: return null
      if (TAG_ROOT != element.name) {
        return null
      }
      val child = element.getChild(targetName) ?: return null
      val method = child.getAttributeValue("method") ?: return null
      return JavaPsiFacade.getElementFactory(project).createMethodFromText(method, null)
    }

    private fun load(stream: InputStream): Element? {
      return try {
        JDOMUtil.load(stream)
      } catch (e: IOException) {
        null
      } catch (e: JDOMException) {
        null
      }
    }
  }
}
