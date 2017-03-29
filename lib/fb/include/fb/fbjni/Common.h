/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

/** @file Common.h
 *
 * Defining the stuff that don't deserve headers of their own...
 */

#pragma once

#include <functional>

#include <jni.h>

#include <fb/visibility.h>
#include <fb/Environment.h>

#ifdef FBJNI_DEBUG_REFS
# ifdef __ANDROID__
#  include <android/log.h>
# else
#  include <cstdio>
# endif
#endif

