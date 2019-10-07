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

import java.util.List;
import javax.annotation.processing.Messager;

public class MultiPrintableException extends PrintableException {
  private final List<PrintableException> mExceptions;

  MultiPrintableException(List<PrintableException> exceptions) {
    mExceptions = exceptions;
  }

  @Override
  public void print(Messager messager) {
    for (PrintableException e : mExceptions) {
      e.print(messager);
    }
  }
}
