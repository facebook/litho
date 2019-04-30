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

package com.facebook.litho.testing;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
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
