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

import com.facebook.litho.annotations.Comparable;
import com.facebook.litho.drawable.ComparableDrawable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import javax.annotation.Nullable;

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
      if (!field.isAnnotationPresent(Comparable.class)) {
        continue;
      }

      final Class<?> classType = field.getType();
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

      @Comparable.Type int comparableType = field.getAnnotation(Comparable.class).type();
      switch (comparableType) {
        case Comparable.FLOAT:
          if (Float.compare((Float) val1, (Float) val2) != 0) {
            return false;
          }
          break;

        case Comparable.DOUBLE:
          if (Double.compare((Double) val1, (Double) val2) != 0) {
            return false;
          }
          break;

        case Comparable.ARRAY:
          if (!areArraysEquals(classType, val1, val2)) {
            return false;
          }
          break;

        case Comparable.PRIMITIVE:
          if (!val1.equals(val2)) {
            return false;
          }
          break;

        case Comparable.COMPARABLE_DRAWABLE:
          if (!((ComparableDrawable) val1).isEquivalentTo((ComparableDrawable) val2)) {
            return false;
          }
          break;

        case Comparable.COLLECTION_COMPLEVEL_0:
          final Collection c1 = (Collection) val1;
          final Collection c2 = (Collection) val2;
          if (c1 != null ? !c1.equals(c2) : c2 != null) {
            return false;
          }
          break;

        case Comparable.COLLECTION_COMPLEVEL_1:
        case Comparable.COLLECTION_COMPLEVEL_2:
        case Comparable.COLLECTION_COMPLEVEL_3:
        case Comparable.COLLECTION_COMPLEVEL_4:
          // N.B. This relies on the IntDef to be in increasing order.
          int level = comparableType - Comparable.COLLECTION_COMPLEVEL_0;
          if (!areComponentCollectionsEquals(level, (Collection) val1, (Collection) val2)) {
            return false;
          }
          break;

        case Comparable.COMPONENT:
        case Comparable.SECTION:
          if (val1 != null ? !((Equivalence) val1).isEquivalentTo(val2) : val2 != null) {
            return false;
          }
          break;

        case Comparable.EVENT_HANDLER:
        case Comparable.EVENT_HANDLER_IN_PARAMETERIZED_TYPE:
          if (val1 != null
              ? !((EventHandler) val1).isEquivalentTo((EventHandler) val2)
              : val2 != null) {
            return false;
          }
          break;

        case Comparable.OTHER:
          if (val1 != null ? !val1.equals(val2) : val2 != null) {
            return false;
          }
          break;

        case Comparable.STATE_CONTAINER:
          // If we have a state container field, we need to recursively call this method to
          // inspect the state fields.
          if (!hasEquivalentFields(val1, val2)) {
            return false;
          }
          break;
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
   * @return String representation of the tree with the root at the passed node For example:
   *     PlaygroundComponent |-Text[trans.key="text_transition_key";] |-Row | +-Text
   *     +-Text[manual.key="text2";]
   */
  static String treeToString(@Nullable InternalNode root) {
    if (root == null) {
      return "null";
    }

    final StringBuilder builder = new StringBuilder();
    final Deque<InternalNode> stack = new LinkedList<>();
    stack.addLast(null);
    stack.addLast(root);
    int level = 0;
    while (!stack.isEmpty()) {
      final InternalNode node = stack.removeLast();
      if (node == null) {
        level--;
        continue;
      }

      final Component component = node.getTailComponent();
      if (component == null) {
        continue;
      }

      if (node != root) {
        builder.append('\n');
        boolean isLast;
        final Iterator<InternalNode> iterator = stack.iterator();
        iterator.next();
        iterator.next();
        for (int index = 0; index < level - 1; index++) {
          isLast = iterator.next() == null;
          if (!isLast) {
            while (iterator.next() != null) ;
          }
          builder.append(isLast ? ' ' : "\u2502").append(' ');
        }
        builder.append(stack.getLast() == null ? "\u2514\u2574" : "\u251C\u2574");
      }

      builder.append(component.getSimpleName());

      if (component.hasManualKey() || node.hasTransitionKey() || node.getTestKey() != null) {
        builder.append('[');
        if (component.hasManualKey()) {
          builder.append("manual.key=\"").append(component.getKey()).append("\";");
        }
        if (node.hasTransitionKey()) {
          builder.append("trans.key=\"").append(node.getTransitionKey()).append("\";");
        }
        if (node.getTestKey() != null) {
          builder.append("test.key=\"").append(node.getTestKey()).append("\";");
        }
        builder.append(']');
      }

      if (node.getChildCount() == 0) {
        continue;
      }

      stack.addLast(null);
      for (int index = node.getChildCount() - 1; index >= 0; index--) {
        stack.addLast(node.getChildAt(index));
      }
      level++;
    }

    return builder.toString();
  }
}
