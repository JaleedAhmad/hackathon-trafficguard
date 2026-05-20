for dev in $(adb devices | awk 'NR>1 {print $1}'); do adb -s $dev install app/build/outputs/apk/debug/app-debug.apk; done

for dev in $(adb devices | awk 'NR>1 {print $1}'); do adb -s $dev reverse tcp:8000 tcp:8000; done
