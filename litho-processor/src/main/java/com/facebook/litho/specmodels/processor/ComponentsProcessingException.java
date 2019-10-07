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

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class ComponentsProcessingException extends PrintableException {
  private final Element mElement;
  private final AnnotationMirror mAnnotationMirror;

  public ComponentsProcessingException(String message) {
    this(null, message);
  }

  public ComponentsProcessingException(Element element, String message) {
    this(element, null, message);
  }

  public ComponentsProcessingException(
      Element element, AnnotationMirror annotationMirror, String message) {
    super(message);
    mElement = element;
    mAnnotationMirror = annotationMirror;
  }

  @Override
  public void print(Messager messager) {
    messager.printMessage(Diagnostic.Kind.ERROR, getMessage(), mElement, mAnnotationMirror);
  }
}
