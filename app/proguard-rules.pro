# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/wolfi/Android/Sdk/tools/proguard/proguard-android-optimize.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# The following rule is added to maintain parity with the previously used 'proguard-android.txt'
# which included '-dontoptimize'. Remove this to enable R8 optimizations.
-dontoptimize


# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# This is generated automatically by the Android Gradle plugin.-dontwarn org.conscrypt.Conscrypt$ProviderBuilder
-dontwarn org.conscrypt.Conscrypt
