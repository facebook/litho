/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

/**
 * This interface allows access to specific stores which save information across independent
 * annotation processor runs.
 *
 * <p>It only contains one concrete store for now, but allows for easier extensibility in the
 * future.
 */
public interface InterStageStore {
  PropNameInterStageStore getPropNameInterStageStore();
}
