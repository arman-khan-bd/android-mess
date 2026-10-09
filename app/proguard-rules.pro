# =========================================================================
# SmartMess Commercial Proguard Rules (Release APK Optimization)
# Prevents Obfuscation Errors for SQLite, Retrofit, OkHttp, and DTOs
# =========================================================================

# Keep Reflection & Serialized Annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Gson Serialized Names & Fields
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# Keep SmartMess Model Entities & DTOs
-keep class com.smartmess.android.model.** { *; }
-keepclassmembers class com.smartmess.android.model.** { *; }
-keep class com.smartmess.android.data.remote.dto.** { *; }
-keepclassmembers class com.smartmess.android.data.remote.dto.** { *; }

# Keep SQLite Contract & DAO
-keep class com.smartmess.android.data.local.SQLiteContract** { *; }
-keepclassmembers class com.smartmess.android.data.local.SQLiteContract** { *; }
-keep class com.smartmess.android.data.local.DatabaseHelper { *; }
-keep class com.smartmess.android.data.local.DatabaseManager { *; }

# Keep Retrofit 2 Interfaces & Annotations
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep interface com.smartmess.android.data.remote.ApiService { *; }

# Keep OkHttp & Conscrypt / TLS
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep AndroidX WorkManager & Receivers
-keep class androidx.work.** { *; }
-keep class com.smartmess.android.data.sync.SyncWorker { *; }
-keep class com.smartmess.android.data.sync.NetworkChangeReceiver { *; }
-keep class com.smartmess.android.notification.** { *; }

# Keep Glide Image Loading
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.** { *; }

# Suppress standard warnings
-dontwarn javax.annotation.**
-dontwarn java.lang.invoke.**
