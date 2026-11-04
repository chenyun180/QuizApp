plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
}

// 纯 Kotlin / JVM 模块：不依赖 Android，单元测试跑得飞快（./gradlew :core:model:test）
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// 显式 API 模式：所有公开 API 必须写明可见性与返回类型，防止无意中改坏二进制兼容性。
// 只加在 main 上——JUnit4 的测试方法必须是 public，加在测试上会一片报错。
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    compilerOptions.freeCompilerArgs.add("-Xexplicit-api=strict")
}

dependencies {
    testImplementation(libs.junit)
}
