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

package com.facebook.litho;

import android.support.annotation.VisibleForTesting;
import com.facebook.litho.ComponentLifecycle.StateContainer;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.reference.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class ComponentUtils {

  /**
   * Given two object instances of the same type, this method accesses all their internal fields,
   * including the fields of StateContainer if the class type is Component, to check if they are
   * equivalent. There's special equality code to handle special class types e.g. Components,
   * EventHandlers, etc.
   *
   * @param obj1
   * @param obj2
   * @return true if the two instances are equivalent. False otherwise.
   */
  public static boolean hasEquivalentFields(Object obj1, Object obj2) {
    if (obj1 == null || obj2 == null || obj1.getClass() != obj2.getClass()) {
      throw new IllegalArgumentException("The input is invalid.");
    }

    for (Field field : obj1.getClass().getDeclaredFields()) {

      if (!(field.isAnnotationPresent(Prop.class)
          || field.isAnnotationPresent(TreeProp.class)
          || field.isAnnotationPresent(State.class)
          || StateContainer.class.isAssignableFrom(field.getType()))) {
        continue;
      }

      final Object val1;
      final Object val2;
      try {
        field.setAccessible(true);
        val1 = field.get(obj1);
        val2 = field.get(obj2);
        field.setAccessible(false);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Unable to get fields by reflection.", e);
      }

      final Class<?> classType = field.getType();
      final Type type = field.getGenericType();
      if (classType.isArray()) {
        if (!areArraysEquals(classType, val1, val2)) {
          return false;
        }

      } else if (Double.TYPE.isAssignableFrom(classType)) {
        if (Double.compare((Double) val1, (Double) val2) != 0) {
          return false;
        }

      } else if (Float.TYPE.isAssignableFrom(classType)) {
        if (Float.compare((Float) val1, (Float) val2) != 0) {
          return false;
        }

      } else if (Reference.class.isAssignableFrom(classType)) {
        if (Reference.shouldUpdate((Reference) val1, (Reference) val2)) {
          return false;
        }

      } else if (Collection.class.isAssignableFrom(classType)) {
        final Collection c1 = (Collection) val1;
        final Collection c2 = (Collection) val2;
        final int level = levelOfComponentsInCollection(type);
        if (level > 0) {
          if (!areComponentCollectionsEquals(level, c1, c2)) {
            return false;
          }
        } else {
          if (c1 != null ? !c1.equals(c2) : c2 != null) {
            return false;
          }
        }

      } else if (Component.class.isAssignableFrom(classType)) {
        if (val1 != null ? !((Component) val1).isEquivalentTo((Component) val2) : val2 != null) {
          return false;
        }

      } else if (EventHandler.class.isAssignableFrom(classType)
          || (type instanceof ParameterizedType
              && EventHandler.class.isAssignableFrom(
                  (Class) ((ParameterizedType) type).getRawType()))) {
        if (val1 != null
            ? !((EventHandler) val1).isEquivalentTo((EventHandler) val2)
            : val2 != null) {
          return false;
        }

        // StateContainers have also fields that we need to check for being equivalent.
      } else if (StateContainer.class.isAssignableFrom(classType)) {
        if (!hasEquivalentFields(val1, val2)) {
          return false;
        }

      } else if (val1 != null ? !val1.equals(val2) : val2 != null) {
        return false;
      }
    }

    return true;
  }

  private static boolean areArraysEquals(Class<?> classType, Object val1, Object val2) {
    final Class<?> innerClassType = classType.getComponentType();
    if (Byte.TYPE.isAssignableFrom(innerClassType)) {
      if (!Arrays.equals((byte[]) val1, (byte[]) val2)) {
        return false;
      }
    } else if (Short.TYPE.isAssignableFrom(innerClassType)) {
      if (!Arrays.equals((short[]) val1, (short[]) val2)) {
        return false;
      }
    } else if (Character.TYPE.isAssignableFrom(innerClassType)) {
      if (!Arrays.equals((char[]) val1, (char[]) val2)) {
        return false;
      }
    } else if (Integer.TYPE.isAssignableFrom(innerClassType)) {
      if (!Arrays.equals((int[]) val1, (int[]) val2)) {
        return false;
      }
    } else if (Long.TYPE.isAssignableFrom(innerClassType)) {
      if (!Arrays.equals((long[]) val1, (long[]) val2)) {
        return false;
      }
    } else if (Float.TYPE.isAssignableFrom(innerClassType)) {
      if (!Arrays.equals((float[]) val1, (float[]) val2)) {
        return false;
      }
    } else if (Double.TYPE.isAssignableFrom(innerClassType)) {
      if (!Arrays.equals((double[]) val1, (double[]) val2)) {
        return false;
      }
    } else if (Boolean.TYPE.isAssignableFrom(innerClassType)) {
      if (!Arrays.equals((boolean[]) val1, (boolean[]) val2)) {
        return false;
      }
    } else if (!Arrays.equals((Object[]) val1, (Object[]) val2)) {
      return false;
    }

    return true;
  }

  private static boolean areComponentCollectionsEquals(
      final int level, final Collection c1, final Collection c2) {
    if (level < 1) {
      throw new IllegalArgumentException("Level cannot be < 1");
    }

    if (c1 == null && c2 == null) {
      return true;
    }

    if (c1 != null ? (c2 == null || c1.size() != c2.size()) : c2 != null) {
      return false;
    }

    final Iterator i1 = c1.iterator();
    final Iterator i2 = c2.iterator();
    while (i1.hasNext() && i2.hasNext()) {
      if (level == 1) {
        if (!((Component) i1.next()).isEquivalentTo((Component) i2.next())) {
          return false;
        }
      } else {
        if (!areComponentCollectionsEquals(
            level - 1, (Collection) i1.next(), (Collection) i2.next())) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Calculate the level of the target Component/Section. The level here means how many bracket
   * pairs are needed to break until reaching the component type. For example, the level of
   * {@literal List<Component>} is 1, and the level of {@literal List<List<Component>>} is 2.
   *
   * @return the level of the target component, or 0 if the target isn't a component.
   */
  @VisibleForTesting
  static int levelOfComponentsInCollection(Type type) {
    int level = 0;

    while (true) {
      if (isParameterizedCollection(type)) {
        type = ((ParameterizedType) type).getActualTypeArguments()[0];
        level++;

      } else if (type instanceof WildcardType) {
        type = ((WildcardType) type).getUpperBounds()[0];

      } else {
        break;
      }
    }

    return (type instanceof Class) && Component.class.isAssignableFrom((Class) type) ? level : 0;
  }

  private static boolean isParameterizedCollection(Type type) {
    return (type instanceof ParameterizedType)
        && Collection.class.isAssignableFrom((Class) ((ParameterizedType) type).getRawType());
  }
}
