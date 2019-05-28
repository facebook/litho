/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Params can be used together with {@link Event} callbacks i.e. {@code EventHandler}. An argument
 * in the event handler annotated with {@code Param} will add it in the generated {@code Component}.
 * This will allow to pass relevant arguments to the event handler while dispatching events. This is
 * especially useful when event handlers can be reused.
 *
 * <p><b>For Example:</b> <br>
 *
 * <pre><code>{@literal @LayoutSpec}
 * class FacePileComponentSpec {
 *
 *  {@literal @OnCreateLayout}
 *   static Component onCreateLayout(
 *       LayoutContext c,
 *      {@literal @Prop} Uri[] faces) {
 *
 *     Component.Builder builder = Column.create(c);
 *     for (Uri face : faces) {
 *       builder.child(
 *           FrescoImage.create(c)
 *               .uri(face)
 *               .clickHandler(FacePileComponent.onFaceClicked(c, face));
 *     }
 *
 *     return builder.build();
 *   }
 *
 *  {@literal @OnEvent(ClickEvent.class)}
 *   static void onFaceClicked(
 *       ComponentContext c,
 *      {@literal @Param} Uri face) {
 *
 *     // Use the param face here
 *     Log.d("FacePileComponent", "Face clicked: " + face);
 *   }
 * }</code></pre>
 */
@Retention(RetentionPolicy.CLASS)
public @interface Param {}
