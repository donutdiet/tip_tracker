# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

## Gson uses generic type information stored in a class file when working with fields. Proguard
## removes such information by default, so configure it to keep all of it.
#-keepattributes Signature
#
## Gson specific classes
#-keep class com.google.gson.reflect.TypeToken { *; }
#
## Application classes that use Gson
#-keep class com.example.tiptracker.** { *; }
#
## Keep class members that use Gson annotations
#-keepclassmembers class * {
#    @com.google.gson.annotations.SerializedName <fields>;
#}
#
## Keep classes that use Gson
#-keep class com.google.gson.** { *; }
#
## Keep the DiningLogData class and all its fields and methods
#-keepclassmembers class com.example.tiptracker.ui.DiningLogData {
#    *;
#}
#
## Hilt rules
#-keep class dagger.hilt.** { *; }
#-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
#-keep class dagger.hilt.internal.** { *; }
#-keep class dagger.hilt.EntryPoint { *; }
#-keepclassmembers class ** {
#    @dagger.hilt.InstallIn <fields>;
#    @dagger.hilt.InstallIn <methods>;
#}
#-keepclassmembers class ** {
#    @dagger.hilt.android.lifecycle.HiltViewModel <fields>;
#    @dagger.hilt.android.lifecycle.HiltViewModel <methods>;
#}
#-keep class dagger.hilt.components.SingletonComponent { *; }
#-keep class dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
#
## DataStore rules
#-keepattributes Signature
#-keep class kotlinx.** { *; }
#-keepclassmembers class kotlinx.** { *; }
#-keep class kotlin.Metadata { *; }
#-keepclassmembers class kotlin.Metadata { *; }
#-keep class kotlinx.coroutines.flow.Flow { *; }
#-keep class kotlinx.coroutines.flow.FlowCollector { *; }
#
## AndroidX Lifecycle and ViewModel rules
#-keep class androidx.lifecycle.** { *; }
#-keep class androidx.datastore.** { *; }
#
## Keep DataStore Preferences
#-keep class androidx.datastore.preferences.core.** { *; }
#
## Keep DataStore Preferences keys
#-keep class androidx.datastore.preferences.core.PreferencesKeys { *; }
#
## Prevent removal of DataStore related classes
#-keep class androidx.datastore.core.DataStore { *; }
#-keep class androidx.datastore.core.Serializer { *; }
#-keep class androidx.datastore.preferences.core.Preferences { *; }
#
## Prevent removal of injected classes
#-keepclassmembers class * {
#    @javax.inject.Inject <fields>;
#}
#-keep class javax.inject.** { *; }
#
## Coroutine Flow rules
#-keep class kotlinx.coroutines.flow.Flow { *; }
#-keep class kotlinx.coroutines.flow.FlowCollector { *; }
#-keepclassmembers class kotlinx.coroutines.flow.FlowCollector { *; }
#-keepclassmembers class kotlinx.coroutines.flow.Flow { *; }