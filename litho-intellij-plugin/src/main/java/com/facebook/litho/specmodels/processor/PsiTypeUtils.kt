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

package com.facebook.litho.specmodels.processor

import com.facebook.litho.specmodels.internal.ImmutableList
import com.facebook.litho.specmodels.model.ClassNames
import com.facebook.litho.specmodels.model.TypeSpec
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeVisitor
import com.intellij.psi.util.PsiTypesUtil
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName
import java.util.Arrays
import java.util.Collections
import java.util.regex.Pattern
import java.util.stream.Collectors

/** Converts [PsiType] to [TypeName]. */
object PsiTypeUtils {

  private val EXTENDS_PATTERN: Pattern = Pattern.compile("\\? extends ([^ ]+)")
  private val SUPER_PATTERN: Pattern = Pattern.compile("\\? super ([^ ]+)")

  @JvmStatic
  fun getTypeName(type: PsiType): TypeName {
    if (type is PsiPrimitiveType) {
      // float
      return getPrimitiveTypeName(type)
    } else if (type is PsiClassType && type.parameterCount > 0) {
      // Set<Integer>
      return ParameterizedTypeName.get(
          guessClassName(type.rawType().canonicalText),
          *getTypeNameArray(type.parameters),
      )
    } else if (type is PsiArrayType) {
      // int[]
      val componentType = type.componentType
      return ArrayTypeName.of(getTypeName(componentType))
    } else if (type.canonicalText.contains("?")) {
      // ? extends Type
      return getWildcardTypeName(type.canonicalText)
    } else {
      return guessClassName(type.canonicalText)
    }
  }

  /** Returns a [ClassName] for the given class name string. */
  @JvmStatic
  fun guessClassName(typeName: String): ClassName {
    return ClassName.bestGuess(typeName)
  }

  @JvmStatic
  fun generateTypeSpec(type: PsiType): TypeSpec {
    return type.accept(
        object : PsiTypeVisitor<TypeSpec>() {
          override fun visitType(type: PsiType): TypeSpec {
            return TypeSpec(getTypeName(type))
          }

          override fun visitClassType(classType: PsiClassType): TypeSpec {
            val psiClass = classType.resolve()
            if (psiClass == null && !ApplicationManager.getApplication().isUnitTestMode) {
              return visitType(classType)
            }
            val qualifiedName = getQualifiedName(psiClass, classType)
            val superInterfaceSpecs = getSuperInterfaceSpecs(psiClass)
            val superClassSpec = getSuperClassSpec(psiClass)

            val typeArguments =
                if (
                    guessClassName(qualifiedName) == ClassNames.DIFF ||
                        superInterfaceSpecs.stream().anyMatch { typeSpec ->
                          typeSpec.isSubInterface(ClassNames.COLLECTION)
                        }
                ) {
                  Arrays.stream(classType.parameters)
                      .map { psiType -> generateTypeSpec(psiType) }
                      .collect(Collectors.toList())
                } else {
                  Collections.emptyList()
                }
            return TypeSpec.DeclaredTypeSpec(
                getTypeName(type),
                qualifiedName,
                { superClassSpec },
                { ImmutableList.copyOf(superInterfaceSpecs) },
                { ImmutableList.copyOf(typeArguments) },
            )
          }
        },
    )
  }

  private fun getSuperInterfaceSpecs(psiClass: PsiClass?): List<TypeSpec> {
    if (psiClass == null) {
      return Collections.emptyList()
    }
    val superInterfaces = psiClass.implementsList
    return if (
        superInterfaces != null &&
            superInterfaces.referencedTypes != null &&
            superInterfaces.referencedTypes.isNotEmpty()
    ) {
      Arrays.stream(superInterfaces.referencedTypes)
          .map { psiType -> generateTypeSpec(psiType) }
          .collect(Collectors.toList())
    } else {
      Collections.emptyList()
    }
  }

  private fun getQualifiedName(psiClass: PsiClass?, classType: PsiClassType): String {
    // In case of Generic like E extends ..., getQualifiedName() returns null
    val canonicalText = classType.canonicalText
    if (psiClass == null) {
      val pos = canonicalText.indexOf('<')
      return if (pos == -1) {
        canonicalText
      } else {
        canonicalText.substring(0, pos)
      }
    }
    val qualifiedName = psiClass.qualifiedName
    return qualifiedName ?: canonicalText
  }

  private fun getSuperClassSpec(psiClass: PsiClass?): TypeSpec? {
    if (psiClass == null) {
      return null
    }
    val superClass = psiClass.superClass
    return if (superClass != null && !superClass.isInterface) {
      generateTypeSpec(PsiTypesUtil.getClassType(superClass))
    } else {
      null
    }
  }

  private fun getTypeNameArray(psiTypes: Array<PsiType>): Array<TypeName> {
    val typeNames = Array(psiTypes.size) { i -> getTypeName(psiTypes[i]) }
    return typeNames
  }

  @JvmStatic
  @VisibleForTesting
  fun getWildcardTypeName(wildcardTypeName: String): TypeName {
    if (wildcardTypeName == "?") {
      return WildcardTypeName.subtypeOf(Any::class.java)
    }
    var matcher = SUPER_PATTERN.matcher(wildcardTypeName)
    if (matcher.find()) {
      return WildcardTypeName.supertypeOf(guessClassName(matcher.group(1)))
    }
    matcher = EXTENDS_PATTERN.matcher(wildcardTypeName)
    if (matcher.find()) {
      return WildcardTypeName.subtypeOf(guessClassName(matcher.group(1)))
    }
    return guessClassName(wildcardTypeName)
  }

  private fun getPrimitiveTypeName(type: PsiPrimitiveType): TypeName {
    return when (type.canonicalText) {
      "void" -> {
        TypeName.VOID
      }
      "boolean" -> {
        TypeName.BOOLEAN
      }
      "byte" -> {
        TypeName.BYTE
      }
      "short" -> {
        TypeName.SHORT
      }
      "int" -> {
        TypeName.INT
      }
      "long" -> {
        TypeName.LONG
      }
      "char" -> {
        TypeName.CHAR
      }
      "float" -> {
        TypeName.FLOAT
      }
      "double" -> {
        TypeName.DOUBLE
      }
      else -> {
        throw UnsupportedOperationException("Unknown primitive type: " + type.canonicalText)
      }
    }
  }
}
