# Moshi
-keep class com.squareup.moshi.** { * }
-dontwarn com.squareup.moshi.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Room
-keep class androidx.room.** { * }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Compose & Lifecycle
-keepattributes *Annotation*,InnerClasses,Signature
-keep class androidx.compose.** { * }

# Firebase & ML Kit
-keep class com.google.firebase.** { * }
-keep class com.google.mlkit.** { * }
-dontwarn com.google.firebase.**
-dontwarn com.google.mlkit.**
