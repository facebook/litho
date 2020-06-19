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

package com.facebook.litho;

/**
 * Allows a new DynamicValue to be derived from an existing DynamicValue, with modifications
 * applied.
 *
 * @param <I> The type provided
 * @param <O> The type held by the DynamicValue from which this is derived
 */
public class DerivedDynamicValue<I, O> extends DynamicValue<O> {

  public interface Modifier<I, O> {
    O modify(I in);
  }

  public DerivedDynamicValue(final DynamicValue<I> dynamicValue, final Modifier<I, O> modifier) {
    super(modifier.modify(dynamicValue.get()));
    dynamicValue.attachListener(
        new OnValueChangeListener<I>() {
          @Override
          public void onValueChange(DynamicValue<I> value) {
            set(modifier.modify(dynamicValue.get()));
          }
        });
  }
}
