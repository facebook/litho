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

package com.facebook.litho.intellij.extensions

/**
 * Extension point for other plugins to provide own templates.
 *
 * The process is:
 * 1. Create template in the `resources/fileTemplates/internal` folder `TemplateName.java.ft`
 * 2. Use `${NAME}ClassNameSuffix` pattern for class name in template.
 * 3. Create implementation of this interface, returning `TemplateName` and `ClassNameSuffix`.
 * 4. Register class in `plugin.xml` using tag `<templateProvider implementation="yourImpl"/>`
 */
interface TemplateProvider {
  fun getTemplateName(): String

  fun getClassNameSuffix(): String

  fun getTemplatePriority(): Int {
    return 0
  }
}
