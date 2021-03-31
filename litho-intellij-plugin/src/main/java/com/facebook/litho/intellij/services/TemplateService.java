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

package com.facebook.litho.intellij.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ex.DecodeDefaultsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.util.JdomKt;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.Nullable;

public class TemplateService implements Disposable {
  private static final String TAG_ROOT = "templateSet";
  private final Map<String, PsiMethod> templates = new HashMap<>();

  @Nullable
  public PsiMethod getMethodTemplate(@Nullable String prefix, String targetName, Project project) {
    if (prefix != null) {
      final PsiMethod templateFromFullName = getMethodTemplate(prefix + "." + targetName, project);
      if (templateFromFullName != null) {
        return templateFromFullName;
      }
    }

    return getMethodTemplate(targetName, project);
  }

  @Nullable
  public PsiMethod getMethodTemplate(String targetName, Project project) {
    final PsiMethod savedTemplate = templates.get(targetName);
    if (savedTemplate != null) return savedTemplate;

    final PsiMethod template = readMethodTemplate(targetName, project);
    if (template != null) {
      templates.putIfAbsent(targetName, template);
    }
    return template;
  }

  private PsiMethod readMethodTemplate(String targetName, Project project) {
    // TemplateSettings#loadDefaultLiveTemplates
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
    return Optional.ofNullable(
            DecodeDefaultsUtil.getDefaultsInputStream(this, "methodTemplates/methods"))
        .map(TemplateService::load)
        .filter(element -> TAG_ROOT.equals(element.getName()))
        .map(element -> element.getChild(targetName))
        .map(template -> template.getAttributeValue("method"))
        .map(method -> factory.createMethodFromText(method, null))
        .orElse(null);
  }

  @Nullable
  private static Element load(InputStream stream) {
    try {
      return JdomKt.loadElement(stream);
    } catch (IOException | JDOMException ignored) {
      return null;
    }
  }

  @Override
  public void dispose() {
    templates.clear();
  }
}
