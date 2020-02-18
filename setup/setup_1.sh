#!/usr/bin/expect

# download Android tools
spawn rm -fr android-sdk
interact
spawn wget https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip
interact
spawn unzip sdk-tools-linux-4333796.zip -d android-sdk
interact
spawn rm sdk-tools-linux-4333796.zip
interact

# download sdk and emulator
spawn android-sdk/tools/bin/sdkmanager "system-images;android-29;google_apis;x86_64"
send  -- "y\r"
interact
spawn android-sdk/tools/bin/sdkmanager "platforms;android-29"
interact
spawn android-sdk/tools/bin/sdkmanager "build-tools;29.0.2"
interact

spawn android-sdk/tools/bin/sdkmanager "system-images;android-28;google_apis;x86_64"
interact
spawn android-sdk/tools/bin/sdkmanager "platforms;android-28"
interact
spawn android-sdk/tools/bin/sdkmanager "build-tools;28.0.3"
interact

spawn android-sdk/tools/bin/sdkmanager "platform-tools"
interact
spawn android-sdk/tools/bin/sdkmanager "extras;google;m2repository"
interact
spawn android-sdk/tools/bin/sdkmanager "extras;android;m2repository"
interact

# extras for ndk compilation
spawn android-sdk/tools/bin/sdkmanager "ndk-bundle"
interact

set cmakePackageCmd "android-sdk/tools/bin/sdkmanager --list 2>/dev/null | grep cmake | head -n 1 | cut -d' ' -f3"
set cmakePackage [exec sh -c $cmakePackageCmd]
spawn android-sdk/tools/bin/sdkmanager "$cmakePackage"
interact

set ndkPackageCmd "android-sdk/tools/bin/sdkmanager --list 2>/dev/null | grep 'ndk;' | head -n 1 | cut -d' ' -f3"
set ndkPackage [exec sh -c $ndkPackageCmd]
spawn android-sdk/tools/bin/sdkmanager "$ndkPackage"
interact