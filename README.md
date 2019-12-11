## ETG: Espresso Test Generator

This repository contains the source code of ETG, a tool for translating sequences of widget-based actions into an Espresso test suite.

The ETG tool leverages on existing widget-based sequences of Android UI interactions.
The current ETG prototype relies on an extension of the MATE widget-based testing tool.

### Setup instructions

These instructions assume Ubuntu 18.04 as Operating System.

1. Run `./setup/setup_0.sh` script to install `expect` program.
2. Run `./setup/setup_1.sh` script to install Android SDK in ETG's folder.
3. Run `./setup/setup_2.sh` script to install an Android emulator in ETG's folder.
4. Run `./setup/setup_3.sh` script to download MATE's extension.

### Preparing ETG's config file for application

ETG needs a per application config file with some data in order to be able to produce compilable and executable Espresso test cases.

A template of such config file can be found in `config/template.config`.

The fields in the config file are the following:

- `jsonPath`: MATE test cases dump json path
- `packageName`: App package name (i.e., the one used for the Activities)
- `testPackageName`: Test package name (i.e., the one used for the test cases)
- `compiledPackageName`: Compiled package name (i.e., the one used when compiling with the provided build type and product flavors)
- `buildType`: Build type to compile and filter APKs
- `productFlavors`: Product flavors (comma separated) to compile and filter APKs
- `rootProjectPath`: Root of the project
- `outputPath`: Folder where to output generated test cases

### Run instructions    

- Set the working directory to the project's root folder
```bash
cd etg 
```
- Set up environment variables for Android SDK and emulators
```bash
export ANDROID_HOME=$(pwd)/android-sdk
export ANDROID_EMULATOR_HOME=$(pwd)/emulators
export ANDROID_AVD_HOME=$(pwd)/emulators/avd
```
-  Fire up an emulator
```bash
$ANDROID_HOME/emulator/emulator -no-snapshot -no-boot-anim -writable-system -wipe-data -no-accel -avd Nexus_5X_API_28_0 > $(pwd)emulator.log 2>&1 &
```
- Prepare and run MATE's server
```bash
etg-mate-server/gradlew -p etg-mate-server/ fatJar
pkill -f mate-server
cd etg-mate-server 
java -jar build/libs/mate-server-all-1.0-SNAPSHOT.jar > $(pwd)mate_server.log 2>&1 &
cd ..
```
- Prepare MATE's client
```bash
etg-mate/gradlew -p etg-mate/ installDebug > $(pwd)mate_client.log 2>&1
etg-mate/gradlew -p etg-mate/ installDebugAndroidTest >> $(pwd)mate_client.log 2>&1
```
- Install desired APK in emulator
```bash
$ANDROID_HOME/platform-tools/adb install <path to apk>
```
- Fire up App's Main Activity
```bash
$ANDROID_HOME/platform-tools/adb shell monkey -p <package name> -c android.intent.category.LAUNCHER 1
```
- Run MATE's client with one of its algorithms
```bash
$ANDROID_HOME/platform-tools/adb shell am instrument -w -r -e debug false -e class org.mate.ExecuteMATERandomWalkActivityCoverage org.mate.test/android.support.test.runner.AndroidJUnitRunner 
```
- Copy JSON output to subjects folder
```bash
cp etg-mate-server/mate-test-cases.json <path to subject source code>/
```
- Prepare and run ETG
```bash
gradle build
java -jar build/libs/etg-1.0-SNAPSHOT.jar <etg config file path>
```