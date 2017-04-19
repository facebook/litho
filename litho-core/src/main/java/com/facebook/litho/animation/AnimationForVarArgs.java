/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.animation;

/**
 * Either a {@link AnimationBinding} or a builder that can create one.
 *
 * This interface only exists because Java's typing system is less than ideal. We need a type that
 * represents either an animation, or an animation builder that can return an animation so that we
 * can expose var args methods like {@link Animated#sequence}.
 */
public interface AnimationForVarArgs {
}
