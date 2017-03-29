/*
 * Copyright (C) 2005 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * FB Wrapper for logging functions.
 *
 * The android logging API uses the macro "LOG()" for its logic, which means
 * that it conflicts with random other places that use LOG for their own
 * purposes and doesn't work right half the places you include it
 *
 * FBLOG uses exactly the same semantics (FBLOGD for debug etc) but because of
 * the FB prefix it's strictly better. FBLOGV also gets stripped out based on
 * whether NDEBUG is set, but can be overridden by FBLOG_NDEBUG
 *
 * Most of the rest is a copy of <cutils/log.h> with minor changes.
 */

//
// C/C++ logging functions.  See the logging documentation for API details.
//
// We'd like these to be available from C code (in case we import some from
// somewhere), so this has a C interface.
//
// The output will be correct when the log file is shared between multiple
// threads and/or multiple processes so long as the operating system
// supports O_APPEND.  These calls have mutex-protected data structures
// and so are NOT reentrant.  Do not use LOG in a signal handler.
//
#pragma once

#include <fb/visibility.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifdef ANDROID
#include <android/log.h>
#else
// These declarations are needed for our internal use even on non-Android
// builds.
// (they are borrowed from <android/log.h>)

/*
 * Android log priority values, in ascending priority order.
 */
typedef enum android_LogPriority {
  ANDROID_LOG_UNKNOWN = 0,
  ANDROID_LOG_DEFAULT, /* only for SetMinPriority() */
  ANDROID_LOG_VERBOSE,
  ANDROID_LOG_DEBUG,
  ANDROID_LOG_INFO,
  ANDROID_LOG_WARN,
  ANDROID_LOG_ERROR,
  ANDROID_LOG_FATAL,
  ANDROID_LOG_SILENT, /* only for SetMinPriority(); must be last */
} android_LogPriority;

/*
 * Send a simple string to the log.
 */
int __android_log_write(int prio, const char *tag, const char *text);

/*
 * Send a formatted string to the log, used like printf(fmt,...)
 */
int __android_log_print(int prio, const char *tag, const char *fmt, ...)
#if defined(__GNUC__)
    __attribute__((format(printf, 3, 4)))
