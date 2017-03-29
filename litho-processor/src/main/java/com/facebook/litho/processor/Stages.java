/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.javapoet.JPUtil;
import com.facebook.litho.processor.GetTreePropsForChildrenMethodBuilder.CreateTreePropMethodData;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.PropDefaultModel;
import com.facebook.litho.specmodels.processor.PropDefaultsExtractor;
