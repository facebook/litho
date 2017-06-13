/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class ReflectionHelperTest {
  @Test
  public void testSetStaticFinal() throws NoSuchFieldException, IllegalAccessException {
    final MyTestClass myTestClass = new MyTestClass();
    final Field field = MyTestClass.class.getDeclaredField("sHiddenField");
    ReflectionHelper.setFinalStatic(field, 3);

    assertThat(myTestClass.getHiddenField()).isEqualTo(3);
  }

  @Test
  public void testSetStaticFinalConvenience() throws NoSuchFieldException, IllegalAccessException {
    final MyTestClass myTestClass = new MyTestClass();
    ReflectionHelper.setFinalStatic(MyTestClass.class, "sHiddenField", 2);

    assertThat(myTestClass.getHiddenField()).isEqualTo(2);
  }

  static class MyTestClass {
    private static final Integer sHiddenField = 1;

    public int getHiddenField() {
      return MyTestClass.sHiddenField;
    }
  }
}
