package com.facebook.litho.specmodels.generator;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public class TypeElementUtils {
  private TypeElementUtils() {
    // No instances
  }

  public static TypeElementCtorsInfo extractCtorsInfo(TypeElement element) {
    final List<? extends Element> classEnclosedElements = element.getEnclosedElements();
    final List<? extends Element> availableCtors = classEnclosedElements.stream()
        .filter(
            (Predicate<Element>) innerElement -> innerElement.getKind() == ElementKind.CONSTRUCTOR)
        .collect(Collectors.toList());

    if (availableCtors.size() == 1) {
      final boolean isEmptyCtor =
          ((ExecutableElement) availableCtors.get(0)).getParameters().size() == 0;

      if (isEmptyCtor) {
        return TypeElementCtorsInfo.SINGLE_EMPTY;
      }

      return TypeElementCtorsInfo.SINGLE_NON_EMPTY;
    }

    return TypeElementCtorsInfo.MULTIPLE;
  }

  public static Set<String> extractAccessorMethodNames(TypeElement element) {
    if (element.getKind() != ElementKind.CLASS) {
      return Collections.emptySet();
    }

    return element.getEnclosedElements()
        .stream()
        .filter((Predicate<Element>) enclosedElement -> {
          final String methodName = enclosedElement.getSimpleName().toString();
          return enclosedElement.getKind() == ElementKind.METHOD && (methodName.startsWith("get")
              || methodName.startsWith("is"));
        })
        .map((Function<Element, String>) enclosedElement -> enclosedElement.getSimpleName()
            .toString())
        .collect(Collectors.toSet());
  }
}
