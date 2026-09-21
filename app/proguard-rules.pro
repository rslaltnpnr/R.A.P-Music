# OZIN Music release shrinking rules.
-keepattributes *Annotation*
-keep class com.ozin.music.core.data.local.** { *; }
-keep class androidx.media3.** { *; }
-dontwarn org.slf4j.**
