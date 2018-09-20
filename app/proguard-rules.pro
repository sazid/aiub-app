# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Sazid\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:
#-keep class * extends android.webkit.WebChromeClient { *; }
#-dontwarn im.delight.android.webview.**

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keep class * extends android.webkit.WebChromeClient { *; }
-dontwarn im.delight.android.webview.**

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep public class com.google.android.gms.ads.** {
    public *;
}

-keep public class com.google.ads.** {
    public *;
}

-dontwarn com.google.android.gms.**

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keep class com.firebase.** { *; }
-keep class org.apache.** { *; }
-keepnames class com.shaded.fasterxml.** { *; }
-keepnames class com.fasterxml.jackson.** { *; }
-keepnames class javax.servlet.** { *; }
-keepnames class org.ietf.jgss.** { *; }
-dontwarn org.apache.**
-dontwarn org.w3c.dom.**
