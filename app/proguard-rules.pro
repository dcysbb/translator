# Keep ML Kit / model data classes if minification is enabled later.
# MVP keeps minify off, so this file is mostly a placeholder.

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }

# Gson model classes
-keep class com.example.translator.** { *; }
