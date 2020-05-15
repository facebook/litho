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

package com.facebook.litho.intellij.extensions;

/**
 * Extension point for other plugins to provide own templates.
 *
 * <p>The process is:
 *
 * <ol>
 *   <li>Create template in the <code>resources/fileTemplates/internal</code> folder <code>
 *       TemplateName.java.ft</code>
 *   <li>Use <code>${NAME}ClassNameSuffix</code> pattern for class name in template.
 *   <li>Create implementation of this interface, returning <code>TemplateName</code> and <code>
 *       ClassNameSuffix</code>.
 *   <li>Register class in <code>plugin.xml</code> using tag <code>
 *       &lt;templateProvider implementation="yourImpl"/&gt;</code>
 * </ul>
 */
public interface TemplateProvider {
  String getTemplateName();

  String getClassNameSuffix();
}
