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

package com.facebook.litho.specmodels.processor;

import static com.facebook.litho.specmodels.internal.ImmutableList.copyOf;

import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.TypeSpec;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeVisitor;
import com.intellij.psi.util.PsiTypesUtil;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

/** Converts {@link PsiType} to {@link TypeName}. */
class PsiTypeUtils {
  private static final Pattern EXTENDS_PATTERN = Pattern.compile("\\? extends ([^ ]+)");
  private static final Pattern SUPER_PATTERN = Pattern.compile("\\? super ([^ ]+)");

  static TypeName getTypeName(PsiType type) {
    if (type instanceof PsiPrimitiveType) {
      // float
      return getPrimitiveTypeName((PsiPrimitiveType) type);
    } else if (type instanceof PsiClassType && ((PsiClassType) type).getParameterCount() > 0) {
      // Set<Integer>
      PsiClassType classType = (PsiClassType) type;
      return ParameterizedTypeName.get(
          guessClassName(classType.rawType().getCanonicalText()),
          getTypeNameArray(classType.getParameters()));
    } else if (type instanceof PsiArrayType) {
      // int[]
      PsiType componentType = ((PsiArrayType) type).getComponentType();
      return ArrayTypeName.of(getTypeName(componentType));
    } else if (type.getCanonicalText().contains("?")) {
      // ? extends Type
      return getWildcardTypeName(type.getCanonicalText());
    } else {
      return guessClassName(type.getCanonicalText());
    }
  }

  /** @return Returns a {@link ClassName} for the given class name string. */
  static ClassName guessClassName(String typeName) {
    return ClassName.bestGuess(typeName);
  }

  static TypeSpec generateTypeSpec(PsiType type) {
    return type.accept(
        new PsiTypeVisitor<TypeSpec>() {
          @Override
          public TypeSpec visitType(PsiType type) {
            return new TypeSpec(getTypeName(type));
          }

          @Override
          public TypeSpec visitClassType(PsiClassType classType) {
            final PsiClass psiClass = classType.resolve();
            if (psiClass == null && !ApplicationManager.getApplication().isUnitTestMode()) {
              return visitType(classType);
            }
            final String qualifiedName = getQualifiedName(psiClass, classType);
            final List<TypeSpec> superInterfaceSpecs = getSuperInterfaceSpecs(psiClass);
            final TypeSpec superClassSpec = getSuperClassSpec(psiClass);

            final List<TypeSpec> typeArguments =
                guessClassName(qualifiedName).equals(ClassNames.DIFF)
                        || superInterfaceSpecs.stream()
                            .anyMatch(typeSpec -> typeSpec.isSubInterface(ClassNames.COLLECTION))
                    ? Arrays.stream(classType.getParameters())
                        .map(PsiTypeUtils::generateTypeSpec)
                        .collect(Collectors.toList())
                    : Collections.emptyList();
            return new TypeSpec.DeclaredTypeSpec(
                getTypeName(type),
                qualifiedName,
                () -> superClassSpec,
                () -> copyOf(superInterfaceSpecs),
                () -> copyOf(typeArguments));
          }
        });
  }

  private static List<TypeSpec> getSuperInterfaceSpecs(@Nullable PsiClass psiClass) {
    if (psiClass == null) {
      return Collections.emptyList();
    }
    final PsiReferenceList superInterfaces = psiClass.getImplementsList();
    return superInterfaces != null
            && superInterfaces.getReferencedTypes() != null
            && superInterfaces.getReferencedTypes().length > 0
        ? Arrays.stream(superInterfaces.getReferencedTypes())
            .map(PsiTypeUtils::generateTypeSpec)
            .collect(Collectors.toList())
        : Collections.emptyList();
  }

  private static String getQualifiedName(@Nullable PsiClass psiClass, PsiClassType classType) {
    // In case of Generic like E extends ..., getQualifiedName() returns null;]
    final String canonicalText = classType.getCanonicalText();
    if (psiClass == null) {
      int pos = canonicalText.indexOf('<');
      return pos == -1 ? canonicalText : canonicalText.substring(0, pos);
    }
    return psiClass.getQualifiedName() != null ? psiClass.getQualifiedName() : canonicalText;
  }

  private static @Nullable TypeSpec getSuperClassSpec(@Nullable PsiClass psiClass) {
    if (psiClass == null) {
      return null;
    }
    final PsiClass superClass = psiClass.getSuperClass();
    return (superClass != null && !superClass.isInterface())
        ? generateTypeSpec(PsiTypesUtil.getClassType(superClass))
        : null;
  }

  private static TypeName[] getTypeNameArray(PsiType[] psiTypes) {
    final TypeName[] typeNames = new TypeName[psiTypes.length];
    for (int i = 0, size = psiTypes.length; i < size; i++) {
      typeNames[i] = getTypeName(psiTypes[i]);
    }

    return typeNames;
  }

  @VisibleForTesting
  static TypeName getWildcardTypeName(String wildcardTypeName) {
    if (wildcardTypeName.equals("?")) {
      return WildcardTypeName.subtypeOf(Object.class);
    }
    Matcher matcher = SUPER_PATTERN.matcher(wildcardTypeName);
    if (matcher.find()) {
      return WildcardTypeName.supertypeOf(guessClassName(matcher.group(1)));
    }
    matcher = EXTENDS_PATTERN.matcher(wildcardTypeName);
    if (matcher.find()) {
      return WildcardTypeName.subtypeOf(guessClassName(matcher.group(1)));
    }
    return guessClassName(wildcardTypeName);
  }

  private static TypeName getPrimitiveTypeName(PsiPrimitiveType type) {
    switch (type.getCanonicalText()) {
      case "void":
        return TypeName.VOID;
      case "boolean":
        return TypeName.BOOLEAN;
      case "byte":
        return TypeName.BYTE;
      case "short":
        return TypeName.SHORT;
      case "int":
        return TypeName.INT;
      case "long":
        return TypeName.LONG;
      case "char":
        return TypeName.CHAR;
      case "float":
        return TypeName.FLOAT;
      case "double":
        return TypeName.DOUBLE;
      default:
        throw new UnsupportedOperationException(
            "Unknown primitive type: " + type.getCanonicalText());
    }
  }
}
