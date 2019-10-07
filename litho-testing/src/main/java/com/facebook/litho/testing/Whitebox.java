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

package com.facebook.litho.testing;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Internal copy of Whitebox implementation of Powermock. */
public class Whitebox {

  @SuppressWarnings("unchecked")
  public static <T> T getInternalState(Object object, String fieldName) {
    Field foundField = findFieldInHierarchy(getType(object), fieldName);
    try {
      return (T) foundField.get(object);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(
          "Internal error: Failed to get field in method getInternalState.", e);
    }
  }

  public static void setInternalState(Object object, String fieldName, Object value) {
    Field foundField = findFieldInHierarchy(object.getClass(), fieldName);
    try {
      foundField.set(object, value);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(
          "Internal error: Failed to get field in method getInternalState.", e);
    }
  }

  private static Field findFieldInHierarchy(Class<?> startClass, String fieldName) {
    Field foundField = null;
    Class<?> currentClass = startClass;
    while (currentClass != null) {
      final Field[] declaredFields = currentClass.getDeclaredFields();

      for (Field field : declaredFields) {
        if (field.getName().equals(fieldName)) {
          if (foundField != null) {
            throw new IllegalArgumentException(
                "Two or more fields matching " + fieldName + " in " + startClass + ".");
          }

          foundField = field;
        }
      }
      if (foundField != null) {
        break;
      }
      currentClass = currentClass.getSuperclass();
    }
    if (foundField == null) {
      throw new IllegalArgumentException(
          "No fields matching " + fieldName + " in " + startClass + ".");
    }
    foundField.setAccessible(true);
    return foundField;
  }

  @SuppressWarnings("unchecked")
  public static <T> T invokeMethod(Object object, String methodName, Object... arguments) {
    Method foundMethod = findMethodInHierarchy(object.getClass(), methodName);
    try {
      return (T) foundMethod.invoke(object, arguments);
    } catch (Exception e) {
      throw new RuntimeException("Internal error: Failed to invoked method.", e);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T invokeMethod(Class<?> clazz, String methodName, Object... arguments) {
    Method foundMethod = findMethodInHierarchy(clazz, methodName);
    try {
      return (T) foundMethod.invoke(clazz, arguments);
    } catch (Exception e) {
      throw new RuntimeException("Internal error: Failed to invoked method.", e);
    }
  }

  private static Method findMethodInHierarchy(Class<?> startClass, String methodName) {
    Method foundMethod = null;
    Class<?> currentClass = startClass;

    while (currentClass != null) {
      final Method[] declaredMethods = currentClass.getDeclaredMethods();

      for (Method method : declaredMethods) {
        if (method.getName().equals(methodName)) {
          if (foundMethod != null) {
            throw new IllegalArgumentException(
                "Two or more methods matching " + methodName + " in " + startClass + ".");
          }

          foundMethod = method;
        }
      }
      if (foundMethod != null) {
        break;
      }
      currentClass = currentClass.getSuperclass();
    }
    if (foundMethod == null) {
      throw new IllegalArgumentException(
          "No methods matching " + methodName + " in " + startClass + ".");
    }
    foundMethod.setAccessible(true);
    return foundMethod;
  }

  public static Class<?> getType(Object object) {
    if (object instanceof Class<?>) {
      return (Class<?>) object;
    } else {
      return object.getClass();
    }
  }
}
