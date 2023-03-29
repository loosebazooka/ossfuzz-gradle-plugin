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

import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.bcel.classfile.ClassParser;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/** Build an output directory that is setup for ossfuzz to run. */
public abstract class OssFuzzBuilderTask extends DefaultTask {

  @OutputDirectory
  public abstract DirectoryProperty getOutputDirectory();

  @InputFiles
  public abstract Property<FileCollection> getRuntimeFiles();

  @Input
  public abstract ListProperty<String> getIncludes();

  @Input
  public abstract ListProperty<String> getExcludes();

  @Input
  public abstract Property<String> getJvmLdLibraryPath();

  @Inject
  protected abstract FileSystemOperations getFileSystemOperations();

  @TaskAction
  public void generateOssFuzzDirectory() throws IOException {
    // copy all files into output directory
    getFileSystemOperations()
        .copy(
            spec -> {
              spec.from(getRuntimeFiles());
              spec.into(getOutputDirectory());
            });

    var runtimeClasspath =
        getOutputDirectory()
                .getAsFileTree()
                .matching(
                    filter -> {
                      filter.include("*.jar");
                    })
                .getFiles()
                .stream()
                .map(File::getName)
                .map(name -> "$_this_dir/" + name)
                .collect(Collectors.joining(":"))
            + ":$_this_dir";

    // filter for files we want
    var fuzzers =
        getOutputDirectory()
            .getAsFileTree()
            .matching(
                filter -> {
                  filter.include(getIncludes().get());
                  filter.exclude(getExcludes().get());
                })
            .getFiles()
            .stream()
            .filter(File::isFile)
            .filter(f -> f.getName().endsWith(".class"))
            .collect(Collectors.toList());

    // generate the fuzzer entrypoint scripts
    final var fuzzerTemplate =
        Resources.toString(
            Resources.getResource(this.getClass(), "fuzzer.template"), StandardCharsets.UTF_8);
    for (var fuzzer : fuzzers) {
      // MyFuzzer.class -> MyFuzzer
      String fuzzerName =
          fuzzer.getName().substring(0, fuzzer.getName().length() - ".class".length());
      // com.example
      String packageName =
          new ClassParser(fuzzer.getAbsolutePath()).parse().getPackageName();
      // com.example.MyFuzzer
      String targetName = packageName + "." + fuzzerName;

      // replace is probably slow, but it's pretty small.
      String fuzzerScript =
          fuzzerTemplate
              .replace("${JVM_LD_LIBRARY_PATH}", getJvmLdLibraryPath().get())
              .replace("${RUNTIME_CLASSPATH}", runtimeClasspath)
              .replace("${FUZZER_TARGET}", targetName);

      Path out = getOutputDirectory().file(fuzzerName).get().getAsFile().toPath();
      Files.write(out, fuzzerScript.getBytes(StandardCharsets.UTF_8));
      var permissions = Files.getPosixFilePermissions(out);
      permissions.add(PosixFilePermission.OWNER_EXECUTE);
      Files.setPosixFilePermissions(out, permissions);
    }
  }
}
