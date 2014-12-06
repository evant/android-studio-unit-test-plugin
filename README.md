android-studio-unit-test-plugin
===============================

Android Studio (and Intellij 14) IDE support for Android gradle [unit tests](https://github.com/JCAndKSolutions/android-unit-test).

This plugin will mark test directories and resolve `testCompile` dependencies. It also sets up the correct system properties so that Robolectric will work if you are using it.

![alt tag](https://raw.githubusercontent.com/evant/android-studio-unit-test-plugin/master/screenshots/idea.png)

### Requirements
* Android Studio `0.6.0+`
* Android Gradle Plugin `0.11.0+`
* JCAndKSolutions' [android-unit-test](https://github.com/JCAndKSolutions/android-unit-test) gradle plugin `1.2.2+`

## Install IDE the plugin
In Android Studio go to `Settings -> Plugins -> Browse Repositories...` and search for 'Android Studio Unit Test'.

If you feel like living on the edge, can download the [zip](https://github.com/evant/android-studio-unit-test-plugin/raw/master/AndroidStudioUnitTestPlugin/AndroidStudioUnitTestPlugin.zip) then go to `Settings -> Plugins -> Install plugin from disk..` to install.

## Install the gradle plugin
To added unit testing support to your gradle project, you need JCAndKSolutions' android-unit-test gradle plugin.
You need to set it up as described in the [README](https://github.com/JCAndKSolutions/android-unit-test).
Make sure you have at least version `1.2.2`.

## Troubleshooting

* Running tests from the IDE gives a `ClassNotFoundException` or something similar.

**If you are running Android Stuido `0.8.9+`**.
The latest version of this plugin will compile your test classes before running the test. However, the very first
time you run them after they have been cleaned, you will still get this error. Simply attempting to run again will fix it
and any subsiquent runs will work correctly.

**If you are running intellij or and older version of Android Studio**.
If your app includes a library project `compile project(":myLib")` then the JUnit test runner will attempt to run `testClasses` on that project. Since it doesn't have that task it will fail and your test classes will not be generated. To fix, add the needed task to your library project.

```groovy
task testClasses {}
```

* The relative path for Robolectric's `@Config(manifest = "path")` is different between gradle and Android Studio.

  This is because when creating a run configuration, the path is by default relative to your project root, whereas when running it from gradle it's correctly relative to your apps root. To fix, edit the JUnit run configuration and change `Working Directory` to point to your app root.
