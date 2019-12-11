#!/usr/bin/expect
set path "emulators"
set n 1
set base_name "Nexus_5X_API_28_"
set pwd [exec sh -c pwd]

spawn rm -rf "$path"
interact
spawn mkdir -p "$path/avd"
interact

# https://developer.android.com/studio/command-line/variables.html#android_emulator_home
set ::env(ANDROID_EMULATOR_HOME) "$pwd/emulators"
set ::env(ANDROID_AVD_HOME) "$pwd/emulators/avd"

for {set i 0} {$i<$n} {incr i} {
   spawn android-sdk/tools/bin/avdmanager --verbose create avd --force --name "$base_name$i" --package {system-images;android-28;google_apis;x86_64} --abi google_apis/x86_64
   send  -- "no\r"
   interact

   set configFile [open $path/avd/$base_name$i.avd/config.ini w]
   set configTemplate [exec cat avd.config]
   puts $configFile $configTemplate
   close $configFile

   spawn rm -f $path/avd/$base_name$i.avd/sdcard.img
   interact
   spawn android-sdk/emulator/mksdcard -l mySdCard 1024M $path/avd/$base_name$i.avd/sdcard.img
   interact
   spawn sed -i "s/hw.sdCard=no/hw.sdCard=yes\\nsdcard.path=$path\\/avd\\/$base_name$i.avd\\/sdcard.img\\nsdcard.size=1024 MiB/g" $path/avd/$base_name$i.avd/config.ini
   interact
}
