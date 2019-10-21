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

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.InjectPropModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Name;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;

/**
 * This store retains prop names across multi-module annotation processor runs. This is needed as
 * prop names are derived from method parameters which aren't persisted in the Java 7 bytecode and
 * thus cannot be inferred if compilation occurs across modules.
 *
 * <p>The props names are serialized and stored as resources within the output JAR, where they can
 * be read from again at a later point in time.
 */
public class PropNameInterStageStore {
  private final Filer mFiler;

  private static final String BASE_PATH = "META-INF/litho/";
  private static final String FILE_EXT = ".props";

  public PropNameInterStageStore(Filer filer) {
    this.mFiler = filer;
  }

  /**
   * @return List of names in order of definition. List may be empty if there are no custom props
   *     defined. Value may not be present if loading for the given spec model failed, i.e. we don't
   *     have inter-stage resources on the class path to facilitate the lookup.
   */
  public Optional<ImmutableList<String>> loadNames(Name qualifiedName) {
    final Optional<FileObject> resource =
        getResource(mFiler, StandardLocation.CLASS_PATH, "", BASE_PATH + qualifiedName + FILE_EXT);

    return resource.map(
        r -> {
          final List<String> props = new ArrayList<>();
          try (BufferedReader reader =
              new BufferedReader(new InputStreamReader(r.openInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
              props.add(line);
            }
          } catch (final IOException err) {
            // This can only happen due to buggy build systems.
            throw new RuntimeException(err);
          }

          return ImmutableList.copyOf(props);
        });
  }

  /** Saves the prop names of the given spec model at a well-known path within the resources. */
  public void saveNames(SpecModel specModel) throws IOException {
    // This is quite important, because we must not open resources without writing to them
    // due to a bug in the Buck caching layer.
    if (specModel.getRawProps().isEmpty()) {
      return;
    }

    final FileObject outputFile =
        mFiler.createResource(
            StandardLocation.CLASS_OUTPUT, "", BASE_PATH + specModel.getSpecTypeName() + FILE_EXT);

    try (Writer writer =
        new BufferedWriter(new OutputStreamWriter(outputFile.openOutputStream()))) {
      for (final PropModel propModel : specModel.getRawProps()) {
        writer.write(propModel.getName() + "\n");
      }

      for (final InjectPropModel propModel : specModel.getRawInjectProps()) {
        writer.write(propModel.getName() + "\n");
      }
    }
  }

  /**
   * Helper method for obtaining resources from a {@link Filer}, taking care of some javac
   * peculiarities.
   */
  private static Optional<FileObject> getResource(
      final Filer filer,
      final JavaFileManager.Location location,
      final String packageName,
      final String filePath) {
    try {
      final FileObject resource = filer.getResource(location, packageName, filePath);
      resource.openInputStream().close();
      return Optional.of(resource);
    } catch (final Exception e) {
      // ClientCodeException can be thrown by a bug in the javac ClientCodeWrapper
      if (!(e instanceof FileNotFoundException
          || e.getClass().getName().equals("com.sun.tools.javac.util.ClientCodeException"))) {
        throw new RuntimeException(
            String.format("Error opening resource %s/%s", packageName, filePath), e.getCause());
      }
      return Optional.empty();
    }
  }
}
