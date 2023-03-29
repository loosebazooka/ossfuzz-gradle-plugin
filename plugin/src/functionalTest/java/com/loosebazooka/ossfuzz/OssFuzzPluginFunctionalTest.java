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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OssFuzzPluginFunctionalTest {
  @TempDir File projectDir;

  private File getBuildFile() {
    return new File(projectDir, "build.gradle");
  }

  private File getSettingsFile() {
    return new File(projectDir, "settings.gradle");
  }

  @Test
  void canRunTask() throws IOException {
    writeString(getSettingsFile(), "");
    writeString(
        getBuildFile(),
        "plugins {"
            + " id('java')\n"
            + "  id('com.loosebazooka.ossfuzz')\n"
            + "}\n"
            + "repositories {\n"
            + "  mavenCentral()\n"
            + "}\n"
            + "dependencies {\n"
            + "  implementation 'dev.sigstore:sigstore-java:0.3.0'\n"
            + "}\n");

    Path main = projectDir.toPath().resolve(Paths.get("src/main/java/main/MainFuzzer.java"));
    Files.createDirectories(main.getParent());
    writeString(
        Files.createFile(main).toFile(),
        "package main;\n"
            + "public class MainFuzzer {\n"
            + "  public static void main(String[] args) { System.out.println(); }\n"
            + "}");
    // Run the build

    Path out = projectDir.toPath().resolve("build").resolve("out");

    GradleRunner runner = GradleRunner.create();
    runner.withEnvironment(
        Map.of("OUT", out.toAbsolutePath().toString(), "JVM_LD_LIBRARY_PATH", "goose"));
    runner.forwardOutput();
    runner.withPluginClasspath();
    runner.withArguments("ossFuzzBuild");
    runner.withProjectDir(projectDir);
    BuildResult result = runner.build();

    // Verify the result
    Assertions.assertTrue(Files.exists(out.resolve("MainFuzzer")));
    Assertions.assertTrue(Files.exists(out.resolve("main").resolve("MainFuzzer.class")));
  }

  private void writeString(File file, String string) throws IOException {
    try (Writer writer = new FileWriter(file)) {
      writer.write(string);
    }
  }
}
