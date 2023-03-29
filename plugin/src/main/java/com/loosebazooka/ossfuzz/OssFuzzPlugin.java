/*
 * Copyright 2023 The Project Authors.
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
package com.loosebazooka.ossfuzz;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;

public class OssFuzzPlugin implements Plugin<Project> {
  public void apply(Project project) {
    project
        .getTasks()
        .register(
            "ossFuzzBuild",
            OssFuzzBuilderTask.class,
            t -> {
              t.getOutputDirectory()
                  .convention(project.getLayout().getBuildDirectory().dir("ossfuzz"));
              t.getRuntimeFiles()
                  .set(
                      project
                          .getExtensions()
                          .getByType(JavaPluginExtension.class)
                          .getSourceSets()
                          .named("main")
                          .get()
                          .getRuntimeClasspath());
              t.getIncludes().set(Collections.singleton("**/*Fuzzer.class"));
              t.getExcludes().convention(Collections.emptyList());
              t.getOutputDirectory()
                  .convention(
                      project
                          .getLayout()
                          .dir(
                              project.provider(
                                  () ->
                                      Paths.get(
                                              Optional.ofNullable(System.getenv("OUT"))
                                                  .orElseThrow(
                                                      () ->
                                                          new RuntimeException("env.OUT not set")))
                                          .toFile())));
              t.getJvmLdLibraryPath()
                  .convention(
                      Optional.ofNullable(System.getenv("JVM_LD_LIBRARY_PATH"))
                          .orElseThrow(
                              () -> new RuntimeException("env.JVM_LD_LIBRARY_PATH not set")));
            });
  }
}
