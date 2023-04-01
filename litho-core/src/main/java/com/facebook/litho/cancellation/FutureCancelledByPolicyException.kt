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

package com.facebook.litho.cancellation

/**
 * This exception should only be thrown whenever there is an [InterruptedException] or
 * [CancellationException] thrown inside a [TreeFuture] while being marked with
 * [TreeFuture.isCancelled]
 */
internal class FutureCancelledByPolicyException() : RuntimeException()
