# ossfuzz-gradle-plugin
A prototype ossfuzz gradle plugin

⚠ This project is not ready for use, it is an experiment  ⚠

Try it out and see what works, don't depend on it yet, it will probably change

## Usage
This plugin is not published to mavenCentral or gradlePluginPortal, you need to build and deploy
locally and then use in your project

Install into local maven
```bash
$ git clone git@github.com:loosebazooka/ossfuzz-gradle-plugin
$ ./gradlew publishToMavenLocal
```

Since oss fuzz builds happen on docker, we need to have a way for the builder to access our local maven repo
Start a file server in local maven repo
```
$ cd ~/.m2/repository
$ python3 -m http.serer
```

Add local python fileserver as a plugin repository (settings.gradle.kts)
```kotlin
pluginManagement {
  repositories {
    gradlePluginPortal()
    maven {
      url = uri("http://localhost:8000")
      isAllowInsecureProtocol = true
    }
  }
}
```

Apply the plugin on your project
```kotlin
plugins {
  `java`
  ...
  id("com.loosebazooka.ossfuzz") version "0.0.1"
}

// the jazzer api is not automatically included, so include it as a dependency
...
dependencies {
  implementation("com.code-intelligence:jazzer-api:0.16.0")
}
```

setup your build script to call `ossfuzzbuild` (build.sh)
```
#!/bin/bash -eu

./gradlew :fuzzing:ossFuzzBuild
```


run oss-fuzz tools to build fuzzers (this example uses sigstore java)
first add --network="host" to the docker run config so we actually can access the local maven repo
```diff

@@ -680,7 +680,7 @@ def docker_run(run_args, print_output=True, architecture='x86_64'):
   """Calls `docker run`."""
   platform = 'linux/arm64' if architecture == 'aarch64' else 'linux/amd64'
   command = [
-      'docker', 'run', '--rm', '--privileged', '--shm-size=2g', '--platform',
+      'docker', 'run', '--network=host', '--rm', '--privileged', '--shm-size=2g', '--platform',
       platform
   ]
```

then run the build
```bash
$ python3 infra/helper.py build_fuzzers sigstore-java ~/src/sigstore-java
```

and you can run your fuzzers and you normally would
