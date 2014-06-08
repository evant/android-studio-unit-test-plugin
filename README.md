android-studio-unit-test-plugin
===============================

Android Studio IDE support for Android gradle unit tests. Prepared for Robolectric.

This plugin will mark test directories and resolve `testCompile` depenencies. It aslo sets up the correct system properties for Robolectric.

![alt tag](https://raw.githubusercontent.com/evant/android-studio-unit-test-plugin/master/screenshots/idea.png)

## Install IDE the plugin
Download the [zip](https://github.com/evant/android-studio-unit-test-plugin/raw/master/AndroidStudioUnitTestPlugin/AndroidStudioUnitTestPlugin.zip) then go to `Settings -> Plugins -> Install plugin from disk..` to install.

## Install the gradle plugin
Currently you need a forked version of JCAndKSolutions's [android-unit-test](https://github.com/evant/android-unit-test). It depends on a new library which shares an interface between the gradle plugin and the IDE plugin. Therefore, your stps are:

1. Install the depenency.

  ```bash
  git clone https://github.com/evant/android-studio-unit-test-plugin.git
  cd android-studio-unit-test-plugin
  gradle install
  ```

2. Install the forked version of android-unit-test.

  ```bash
  git clone https://github.com/evant/android-unit-test.git
  cd android-unit-test
  gradle install
  ```

3. Set up the plugin as described [here](https://github.com/JCAndKSolutions/android-unit-test).

  The only difference is you need to point to the forked version you installed before.
  ```groovy
  buildscript {
    dependencies {
      repositories {
        mavenCentral()
        mavenLocal()
      }

      classpath 'com.github.jcandksolutions.gradle:android-unit-test:1.2.1-SNAPSHOT'
    }
  }
  ```
