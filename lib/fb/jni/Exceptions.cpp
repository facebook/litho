/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <fb/fbjni/CoreClasses.h>

#include <fb/assert.h>
#include <fb/log.h>

#include <alloca.h>
#include <cstdlib>
#include <ios>
#include <stdexcept>
#include <stdio.h>
#include <string>
#include <system_error>

#include <jni.h>


namespace facebook {
namespace jni {

namespace {
class JRuntimeException : public JavaClass<JRuntimeException, JThrowable> {
