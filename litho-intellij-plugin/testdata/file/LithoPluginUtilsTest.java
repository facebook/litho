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

class LayoutSpec {
    static class InnerClass1 {
        static class InnerClass2 {}
    }
}

@com.facebook.litho.annotations.MountSpec
class MountSpec {}

@com.facebook.litho.annotations.LayoutSpec
class LithoActivitySpec {
    @com.facebook.litho.annotations.OnCreateLayout
    static Component onCreateLayout(ComponentContext c) {
        dummyMethod();
        return Text.create(c).clickHandler();
    }

    static void methodWithoutEventHandlerArgument();
}

static class Text {
    static Builder create(ComponentContext context);
}

static class Builder {
    static void clickHandler(com.facebook.litho.EventHandler<TypeClass> handler);
}

static class TypeClass {}
