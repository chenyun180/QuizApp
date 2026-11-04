package com.alri.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.alri.quiz.navigation.QuizNavHost
import com.alri.quiz.core.ui.theme.QuizTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 唯一的 Activity。整 App 是单 Activity + Compose 导航，
 * 这是现在官方推荐的结构：页面切换由 Navigation 管理，不涉及 Activity 跳转。
 *
 * `@AndroidEntryPoint` 让 Hilt 能往这个 Activity 及其 Compose 树里的 ViewModel 注入依赖。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // enableEdgeToEdge：内容延伸到状态栏/导航栏下面，配合 Scaffold 的 insets 处理
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            QuizTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QuizNavHost()
                }
            }
        }
    }
}
