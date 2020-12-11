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

package com.facebook.litho.specmodels.processor;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

public class ParamNameExtractor {
  static List<String> getNames(ExecutableElement method) {
    final List<? extends VariableElement> params = method.getParameters();
    final List<Name> savedParameterNames = getSavedParameterNames(method);
    final List<String> paramNames = new ArrayList<>(params.size());
    for (int i = 0, size = params.size(); i < size; i++) {
      final VariableElement param = params.get(i);
      final String paramName =
          savedParameterNames == null
              ? param.getSimpleName().toString()
              : savedParameterNames.get(i).toString();
      paramNames.add(paramName);
    }
    return paramNames;
  }

  /**
   * Attempt to recover saved parameter names for a method. This will likely only work for code
   * compiled with javac >= 8, but it's often the only chance to get named parameters as opposed to
   * 'arg0', 'arg1', ...
   */
  @Nullable
  private static List<Name> getSavedParameterNames(ExecutableElement method) {
    try {
      if (method instanceof Symbol.MethodSymbol) {
        final Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) method;
        //noinspection unchecked
        return (List<Name>)
            Symbol.MethodSymbol.class.getField("savedParameterNames").get(methodSymbol);
      }
    } catch (NoSuchFieldError
        | IllegalAccessException
        | NoSuchFieldException
        | ClassFormatError ignored) {
      // This can happen on JVM versions >= 10. However, we need to keep this workaround for JVM
      // versions < 8 which do not provide the '-parameters' javac option which is the Right Way
      // to achieve this.
      return null;
    }
    return null;
  }
}
