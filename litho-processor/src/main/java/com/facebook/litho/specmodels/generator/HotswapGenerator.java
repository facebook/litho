/*
 * Copyright 2014-present Facebook, Inc.
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
package com.facebook.litho.specmodels.generator;

import com.facebook.litho.specmodels.generator.DelegateMethodGenerator.ParamTypeAndName;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.List;

/** Generator for code relating to hotswap functionality. */
public class HotswapGenerator {

  private HotswapGenerator() {}

  /**
   * Generates a method that delegates to the Spec class using a ClassLoader specified by the Litho
   * HotswapManager. Delegates directly to the spec if no ClassLoader is specified. Throws {@link
   * RuntimeException} if the ClassLoader is unable to load and invoke the delegate method
   * correctly.
   */
  public static CodeBlock generateDelegatingMethod(
      SpecModel specModel,
      String methodName,
      TypeName returnType,
      ImmutableList<ParamTypeAndName> params,
      CodeBlock directDelegation) {
    final CodeBlock.Builder code =
        CodeBlock.builder()
            .addStatement(
                "$T classLoader = $T.getClassLoader()",
                ClassNames.CLASS_LOADER,
                ClassNames.HOTSWAP_MANAGER)
            .beginControlFlow("if (classLoader == null)")
            .add(directDelegation)
            .nextControlFlow("else")
            .addStatement("$T specClass", ClassNames.CLASS)
            .beginControlFlow("try")
            .addStatement(
                "specClass = classLoader.loadClass(\"$L\")",
                getTypeNameString(specModel.getSpecTypeName()))
            .nextControlFlow("catch (ClassNotFoundException _e)")
            .addStatement("throw new RuntimeException(_e)")
            .endControlFlow()
            .beginControlFlow("try")
            .add("final $T method = specClass.getDeclaredMethod(", ClassNames.JAVA_METHOD)
            .indent();

    if (params.isEmpty()) {
      code.add("\"$L\");\n", methodName);
    } else {
      code.add("\"$L\",\n", methodName);
      for (int i = 0, size = params.size(); i < size; i++) {
        if (i < size - 1) {
          code.add("$T.class,\n", getTypeUsedInDeclaredMethod(params.get(i).type));
        } else {
          code.add("$T.class);\n", getTypeUsedInDeclaredMethod(params.get(i).type));
        }
      }
    }

    code.unindent().addStatement("method.setAccessible(true)");

    if (returnType.equals(TypeName.VOID)) {
      code.add("method.invoke(");
    } else {
      code.add("_result = ($T) method.invoke(", returnType);
    }

    code.indent();

    if (params.isEmpty()) {
      code.add("null);\n");
    } else {
      code.add("null,\n");
      for (int i = 0, size = params.size(); i < size; i++) {
        if (i < size - 1) {
          code.add("($T) $L,\n", params.get(i).type, params.get(i).name);
        } else {
          code.add("($T) $L);\n", params.get(i).type, params.get(i).name);
        }
      }
    }

    return code.unindent()
        .nextControlFlow("catch (Exception _e)")
        .addStatement("throw new RuntimeException(_e)")
        .endControlFlow()
        .endControlFlow()
        .build();
  }

  private static TypeName getTypeUsedInDeclaredMethod(TypeName typeName) {
    if (typeName instanceof TypeVariableName) {
      final List<TypeName> bounds = ((TypeVariableName) typeName).bounds;
      if (bounds.isEmpty()) {
        return TypeName.OBJECT;
      } else {
        return getBaseType(bounds.get(0));
      }
    }

    return getBaseType(typeName);
  }

  private static TypeName getBaseType(TypeName typeName) {
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).rawType;
    }

    return typeName;
  }

  private static String getTypeNameString(ClassName className) {
    ClassName enclosingClass = className.enclosingClassName();
    if (enclosingClass == null) {
      return className.toString();
    }

    return enclosingClass.toString() + "$" + className.simpleName();
  }
}

