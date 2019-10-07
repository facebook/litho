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

class DelegateMethodExtractionClass {

    @com.facebook.litho.annotations.OnEvent(java.lang.Object.class)
    public void ignored() {}

    @com.facebook.litho.annotations.OnUpdateState
    public void alsoIgnored() {}

    @com.facebook.litho.annotations.OnCreateLayout
    public void testMethod(
            @com.facebook.litho.annotations.Prop boolean testProp,
            @com.facebook.litho.annotations.State int testState,
            @com.facebook.litho.annotations.Event java.lang.Object testPermittedAnnotation) {
        // Don't do anything.
    }
}
