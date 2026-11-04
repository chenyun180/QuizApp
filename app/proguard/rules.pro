# ============================================================================
# R8 规则
#
# 原则：能用注解/生成器解决的，不要靠文本规则硬保。
#   - kotlinx.serialization 的 @Serializable 类由编译器生成 serializer，
#     官方做法是让插件生成，R8 需要保留的是 serializer 和 Companion。
#   - Room 生成的实现类由 Room 自己声明 @Keep（在生成代码里），不需要在这里保。
#   - Hilt 生成的代码同理已经是最终代码，不需要额外 keep。
# 这里只放开那些「反射间接调用、静态分析看不出来」的部分。
# ============================================================================

# ---------- kotlinx.serialization ----------
# 序列化器通过反射查找 Companion.serializer()，R8 无法证明它被调用
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 所有标了 @Serializable 的类，保留它们的序列化器
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static **$* *;
}
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------- Room ----------
# Room 的实体/DAO 是通过生成的 Impl 静态调用的，正常不需要 keep。
# 但 schema 里的列名依赖字段名，混淆字段名会让 @ColumnInfo 缺失时出错。
# 本工程所有列都显式写了 @ColumnInfo，因此只保留 Room 自身的入口类。
-keep class androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>();
}
# 生成代码里 @Database 的实现类由 Room 引用，防止被裁掉
-keep class com.alri.quiz.core.database.db.**_Impl { *; }

# ---------- Hilt / Dagger ----------
# Dagger 生成的是编译期确定好的代码，一般无需 keep。
# 但 @HiltAndroidApp 生成的 Hilt_ 前缀类是反射加载的，要保留。
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ---------- ViewModel ----------
# ViewModel 由 Hilt/Compose 通过工厂实例化，构造函数签名必须保留
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ---------- 枚举 ----------
# QuestionType.valueOf() 依赖枚举常量的名字，混淆会抛 IllegalArgumentException
-keepclassmembers enum com.alri.quiz.core.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** fromValue(java.lang.String);
}

# ---------- Kotlin 元数据 ----------
# 保留 Kotlin 元数据，让 kotlin-reflect / 协程调试信息仍可用（体积影响很小）
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes Signature,Exceptions,SourceFile,LineNumberTable

# ---------- Compose ----------
# Compose 的编译器插件已经处理好必要信息；这里只是防止裁剪掉用于工具/预览的类
-dontwarn androidx.compose.**

# 去掉所有日志调用（release 包里不该有 Log.d 输出）
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
