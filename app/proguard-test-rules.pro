# R8 rules for the androidTest apk when testing the minified ciRelease build.
# The test apk is shrunk against the mapping of the app, the test code itself must stay untouched.

-dontoptimize
-dontobfuscate
-dontwarn **

-keep class es.wolfi.app.passman.offline.** { *; }
-keep class es.wolfi.app.passman.e2e.** { *; }
-keep class androidx.test.** { *; }
-keep class org.junit.** { *; }
-keep class junit.** { *; }
-keep class org.hamcrest.** { *; }
