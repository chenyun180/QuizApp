package com.alri.quiz

import android.app.Application
import com.alri.quiz.bootstrap.SampleBankInstaller
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Hilt 的入口。`@HiltAndroidApp` 会在编译期生成 Application 级别的依赖容器。
 *
 * 这里只做一件事：首次启动塞一份示例题库，让 App 打开就有东西可刷。
 * 真正的初始化（数据库连接等）都交给 Hilt 惰性创建，启动路径越短越好。
 */
@HiltAndroidApp
class QuizApplication : Application() {

    @Inject
    lateinit var sampleBankInstaller: SampleBankInstaller

    override fun onCreate() {
        super.onCreate()
        // 此时 Hilt 已完成注入，可以安全使用注入的依赖
        sampleBankInstaller.installIfNeeded()
    }
}
