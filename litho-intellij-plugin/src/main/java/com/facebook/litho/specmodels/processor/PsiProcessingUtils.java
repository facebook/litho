/*
 * Copyright 2004-present Facebook, Inc.
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
package com.facebook.litho.specmodels.processor;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

/** A bunch of utilities to make it easier to work with the {@link com.intellij.psi} libraries. */
public class PsiProcessingUtils {

  public static Modifier[] extractModifiersToArray(PsiModifierList psiModifierList) {
    final ImmutableList<Modifier> modifierList = extractModifiers(psiModifierList);

    Modifier[] modifierArray = new Modifier[modifierList.size()];
    for (int i = 0, size = modifierList.size(); i < size; i++) {
      modifierArray[i] = modifierList.get(i);
    }

    return modifierArray;
  }

  public static ImmutableList<Modifier> extractModifiers(PsiModifierList modifierList) {
    List<Modifier> modifiers = new ArrayList<>();

    PsiElement[] children = modifierList.getChildren();
    for (int i = 0; i < children.length; i++) {
      PsiElement child = children[i];

      if (child instanceof PsiModifier) {
        modifiers.add(psiModifierToModifier(child));
      }
    }

    return ImmutableList.copyOf(modifiers);
  }

  public static Modifier psiModifierToModifier(PsiElement psiModifier) {
    switch (psiModifier.getText()) {
      case PsiModifier.ABSTRACT:
        return Modifier.ABSTRACT;
      case PsiModifier.FINAL:
        return Modifier.FINAL;
      case PsiModifier.NATIVE:
        return Modifier.NATIVE;
      case PsiModifier.PRIVATE:
        return Modifier.PRIVATE;
      case PsiModifier.PROTECTED:
        return Modifier.PROTECTED;
      case PsiModifier.PUBLIC:
        return Modifier.PUBLIC;
      case PsiModifier.STATIC:
        return Modifier.STATIC;
      case PsiModifier.STRICTFP:
        return Modifier.STRICTFP;
      case PsiModifier.SYNCHRONIZED:
        return Modifier.SYNCHRONIZED;
      case PsiModifier.TRANSIENT:
        return Modifier.TRANSIENT;
      case PsiModifier.VOLATILE:
        return Modifier.VOLATILE;
      default:
        // TODO better error message, ideally w/ line number
        throw new ComponentsProcessingException(
            "Unexpected Modifier, modifier is: " + psiModifier.getText());
    }
  }
}
