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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.FieldModel;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** Extracts {@link FieldModel} from the given input. */
public class FieldsExtractor {

  public static <T extends Element> ImmutableList<FieldModel> extractFields(T element) {
    final List<FieldModel> fields = new ArrayList<>();

    for (Element enclosedElement : element.getEnclosedElements()) {
      if (ElementKind.FIELD.equals(enclosedElement.getKind())) {
        Set<Modifier> modifiers = enclosedElement.getModifiers();
        fields.add(
            new FieldModel(
                FieldSpec.builder(
                        TypeName.get(enclosedElement.asType()),
                        enclosedElement.getSimpleName().toString(),
                        modifiers.toArray(new Modifier[modifiers.size()]))
                    .addAnnotations(AnnotationExtractor.extractValidAnnotations(enclosedElement))
                    .build(),
                enclosedElement));
      }
    }

    return ImmutableList.copyOf(fields);
  }
}
