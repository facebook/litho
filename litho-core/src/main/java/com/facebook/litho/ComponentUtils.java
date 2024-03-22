/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
import com.facebook.litho.annotations.EventHandlerRebindMode;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.rendercore.Equivalence;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import javax.annotation.Nullable;

public class ComponentUtils {

  public static boolean isSameComponentType(@Nullable Component a, @Nullable Component b) {
    if (a == b) {
      return true;
    } else if (a == null || b == null) {
      return false;
    }
    return a.getClass().equals(b.getClass());
  }

  /**
   * Given two components this method accesses all their internal fields, excluding the fields of
   * StateContainer if the class type is a Component, to check if they are equivalent. There's
   * special equality code to handle special class types e.g. Components, EventHandlers, etc.
   *
   * @param current current Component
   * @param next next Component
   * @return {@code true} iff the component fields are equivalent.
   */
  public static boolean isEquivalent(
      @Nullable final Component current, @Nullable final Component next) {
    if (current == next) {
      return true;
    }

    if (current == null || next == null) {
      return false;
    }

    return current.isEquivalentTo(next);
  }

  public static boolean hasEquivalentState(
      @Nullable StateContainer stateContainer1, @Nullable StateContainer stateContainer2) {
    if (stateContainer1 == null && stateContainer2 == null) {
      return true;
    }

    if (stateContainer1 == null || stateContainer2 == null) {
      return false;
    }

    return hasEquivalentFields(stateContainer1, stateContainer2);
  }

  /**
   * Given two object instances of the same type, this method accesses all their internal fields, to
   * check if they are equivalent. There's special equality code to handle special class types e.g.
   * Components, EventHandlers, etc. Components are considered equivalent if they have the same
   * props.
   *
   * @param obj1
   * @param obj2
   * @return true if the two instances are equivalent. False otherwise.
   */
  public static boolean hasEquivalentFields(final Object obj1, final Object obj2) {
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

      boolean intermediateResult = isEquivalentUtil(field, classType, val1, val2);
      if (!intermediateResult) {
        return false;
      }
    }

    return true;
  }

  private static boolean isEquivalentUtil(
      Field field, Class<?> classType, @Nullable Object val1, @Nullable Object val2) {
    @Comparable.Type int comparableType;
    try {
      comparableType = field.getAnnotation(Comparable.class).type();
    } catch (IncompatibleClassChangeError | NullPointerException ignore) {
      /*
       * Libraries which uses annotations is facing this intermittently in Lollypop 5.0, 5.0.1 &
       * 5.0.2). Google closed this saying it is infeasible to fix this in older OS versions.
       *
       * <p>https://issuetracker.google.com/issues/37045084
       * https://github.com/google/gson/issues/726
       */
      return false;
    }
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
        if (val1 != null ? !((Component) val1).isEquivalentTo((Component) val2) : val2 != null) {
          return false;
        }
        break;
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

  static boolean areArraysEquals(Class<?> classType, Object val1, Object val2) {
    final Class<?> innerClassType = classType.getComponentType();
    if (Byte.TYPE.isAssignableFrom(innerClassType)) {
      return Arrays.equals((byte[]) val1, (byte[]) val2);
    } else if (Short.TYPE.isAssignableFrom(innerClassType)) {
      return Arrays.equals((short[]) val1, (short[]) val2);
    } else if (Character.TYPE.isAssignableFrom(innerClassType)) {
      return Arrays.equals((char[]) val1, (char[]) val2);
    } else if (Integer.TYPE.isAssignableFrom(innerClassType)) {
      return Arrays.equals((int[]) val1, (int[]) val2);
    } else if (Long.TYPE.isAssignableFrom(innerClassType)) {
      return Arrays.equals((long[]) val1, (long[]) val2);
    } else if (Float.TYPE.isAssignableFrom(innerClassType)) {
      return Arrays.equals((float[]) val1, (float[]) val2);
    } else if (Double.TYPE.isAssignableFrom(innerClassType)) {
      return Arrays.equals((double[]) val1, (double[]) val2);
    } else if (Boolean.TYPE.isAssignableFrom(innerClassType)) {
      return Arrays.equals((boolean[]) val1, (boolean[]) val2);
    } else return Arrays.equals((Object[]) val1, (Object[]) val2);
  }

  static boolean areCollectionsEquals(Type type, @Nullable Collection c1, @Nullable Collection c2) {
    final int level = levelOfComponentsInCollection(type);
    if (level > 0) {
      return areComponentCollectionsEquals(level, c1, c2);
    }
    return c1 != null ? c1.equals(c2) : c2 == null;
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
  static String treeToString(@Nullable LithoNode root) {
    if (root == null) {
      return "null";
    }

    final StringBuilder builder = new StringBuilder();
    final Deque<LithoNode> stack = new LinkedList<>();
    stack.addLast(null);
    stack.addLast(root);
    int level = 0;
    while (!stack.isEmpty()) {
      final LithoNode node = stack.removeLast();
      if (node == null) {
        level--;
        continue;
      }

      final Component component = node.getTailComponent();

      if (node != root) {
        builder.append('\n');
        boolean isLast;
        final Iterator<LithoNode> iterator = stack.iterator();
        iterator.next();
        iterator.next();
        for (int index = 0; index < level - 1; index++) {
          isLast = iterator.next() == null;
          if (!isLast) {
            while (iterator.next() != null)
              ;
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

  /**
   * Reraise an error event up the hierarchy so it can be caught by another component, or reach the
   * root and cause the application to crash.
   *
   * @param c The component context the error event was caught in.
   * @param e The original exception.
   */
  public static void raise(ComponentContext c, Exception e) {
    throw new ReThrownException(e, c.getErrorEventHandler());
  }

  /** Utility to dispatch an unhandled exception to a component. To be used by the framework. */
  static void dispatchErrorEvent(ComponentContext c, Exception e) {
    final ErrorEvent event = new ErrorEvent();
    event.exception = e;
    event.componentContext = c;
    dispatchErrorEvent(c, event);
  }

  /** Utility to dispatch an error event to a component. To be used by the generated component. */
  static void dispatchErrorEvent(ComponentContext c, ErrorEvent e) {
    final EventHandler<ErrorEvent> handler = c.getErrorEventHandler();
    if (handler != null) {
      handler.dispatchEvent(e);
    }
  }

  /**
   * Utility to get a component to handle an exception gracefully during the layout phase when
   * dealing with component hierarchy.
   */
  static void handleWithHierarchy(
      ComponentContext parent, Component component, Exception exception) {
    final EventHandler<ErrorEvent> nextHandler = parent.getErrorEventHandler();
    final EventHandler<ErrorEvent> lastHandler;
    Exception exceptionToThrow = exception;

    if (exception instanceof ReThrownException) {
      exceptionToThrow = ((ReThrownException) exception).original;
      lastHandler = ((ReThrownException) exception).lastHandler;
    } else if (exception instanceof LithoMetadataExceptionWrapper) {
      lastHandler = ((LithoMetadataExceptionWrapper) exception).lastHandler;
    } else {
      lastHandler = null;
    }

    final LithoMetadataExceptionWrapper metadataWrapper =
        wrapWithMetadata(parent, exceptionToThrow);
    metadataWrapper.addComponentNameForLayoutStack(component.getSimpleName());

    // This means it was already handled by this handler so throw it up to the next frame until we
    // get a new handler or get to the root
    if (lastHandler == nextHandler) {
      metadataWrapper.lastHandler = lastHandler;
      throw metadataWrapper;
    } else if (nextHandler instanceof ErrorEventHandler) { // at the root
      ((ErrorEventHandler) nextHandler).onError(parent, metadataWrapper);
    } else { // Handle again with new handler
      try {
        dispatchErrorEvent(parent, exceptionToThrow);
      } catch (ReThrownException ex) { // error handler re-raised the exception
        metadataWrapper.lastHandler = nextHandler;
        throw metadataWrapper;
      }
    }
  }

  /**
   * Utility to get a component to handle an exception gracefully outside the layout phase. If the
   * component re-raises the exception using {@link #raise(ComponentContext, Exception)} then the
   * utility will rethrow the exception out of Litho.
   */
  static void handle(ComponentContext c, Exception exception) {

    final boolean isTracing = ComponentsSystrace.isTracing();

    try {

      if (isTracing) {
        ComponentsSystrace.beginSection("handleError");
      }

      if (c.getComponentScope() != null) {
        // acquire component hierarchy metadata leveraging the global key
        LithoMetadataExceptionWrapper metadataExceptionWrapper = wrapWithMetadata(c, exception);
        LinkedList<String> hierarchy = Component.generateHierarchy(c.getGlobalKey());
        for (String componentName : hierarchy) {
          metadataExceptionWrapper.addComponentNameForLayoutStack(componentName);
        }
        dispatchErrorEvent(c, metadataExceptionWrapper);
      } else {
        // we're not able to get global key hierarchy metadata from a ComponentContext without a
        // scope
        dispatchErrorEvent(c, exception);
      }
    } catch (ReThrownException re) {
      throw wrapWithMetadata(c, exception);
    } catch (Exception e) {
      throw wrapWithMetadata(c, e);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  /** Utility to re-throw exceptions. */
  static void rethrow(Exception e) {
    if (e instanceof ReThrownException) {
      rethrow(((ReThrownException) e).original);
    } else if (e instanceof RuntimeException) {
      throw (RuntimeException) e;
    } else {
      throw new RuntimeException(e);
    }
  }

  /**
   * Uses the given ComponentContext to add metadata to a wrapper exception (if the wrapper doesn't
   * already exist) and return it.
   */
  public static LithoMetadataExceptionWrapper wrapWithMetadata(ComponentContext c, Exception e) {
    if (e instanceof LithoMetadataExceptionWrapper) {
      return (LithoMetadataExceptionWrapper) e;
    }
    return new LithoMetadataExceptionWrapper(c, e);
  }

  /**
   * Uses the given ComponentTree to add metadata to a wrapper exception (if the wrapper doesn't
   * already exist) and return it.
   */
  public static LithoMetadataExceptionWrapper wrapWithMetadata(BaseMountingView view, Exception e) {
    if (view instanceof LithoView) {
      return wrapWithMetadata(((LithoView) view).getComponentTree(), e);
    }
    // TODO T149859358 support other implementations of BaseMountingView

    return new LithoMetadataExceptionWrapper(e);
  }

  /**
   * Uses the given ComponentTree to add metadata to a wrapper exception (if the wrapper doesn't
   * already exist) and return it.
   */
  public static LithoMetadataExceptionWrapper wrapWithMetadata(ComponentTree c, Exception e) {
    if (e instanceof LithoMetadataExceptionWrapper) {
      return (LithoMetadataExceptionWrapper) e;
    }
    return new LithoMetadataExceptionWrapper(c, e);
  }

  public static EventHandler<ErrorEvent> createOrGetErrorEventHandler(
      Component component, ComponentContext parentContext, ComponentContext scopedContext) {
    if (component instanceof SpecGeneratedComponent
        && ((SpecGeneratedComponent) component).hasOwnErrorHandler()) {
      return new EventHandler<>(
          Component.ERROR_EVENT_HANDLER_ID,
          EventHandlerRebindMode.NONE,
          new EventDispatchInfo((SpecGeneratedComponent) component, scopedContext),
          null);
    } else {
      return parentContext.getErrorEventHandler();
    }
  }
}
