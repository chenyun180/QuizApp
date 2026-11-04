package com.alri.quiz.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alri.quiz.core.ui.navigation.QuizMode
import com.alri.quiz.core.ui.navigation.Routes
import com.alri.quiz.feature.bankimport.ImportScreen
import com.alri.quiz.feature.quiz.banks.BanksScreen
import com.alri.quiz.feature.quiz.collection.CollectionKind
import com.alri.quiz.feature.quiz.collection.CollectionScreen
import com.alri.quiz.feature.quiz.session.QuizScreen
import com.alri.quiz.feature.quiz.session.QuizViewModel
import com.alri.quiz.feature.quiz.settings.SettingsScreen

/**
 * 全 App 的导航图。
 *
 * 放在 app 模块而不是某个 feature 里：导航需要同时知道所有页面，
 * 而 feature 之间不该互相依赖。app 是唯一允许「知道所有人」的模块。
 */
@Composable
fun QuizNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.BANKS,
        modifier = modifier,
    ) {
        composable(Routes.BANKS) {
            BanksScreen(
                onOpenBank = { bankId ->
                    navController.navigate(Routes.quiz(QuizMode.BANK, bankId))
                },
                onOpenRandom = { bankId ->
                    navController.navigate(Routes.quiz(QuizMode.RANDOM, bankId))
                },
                onOpenWrongBook = { navController.navigate(Routes.quizList(QuizMode.WRONG)) },
                onOpenFavorites = { navController.navigate(Routes.quizList(QuizMode.FAVORITE)) },
                onOpenImport = { navController.navigate(Routes.IMPORT) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.IMPORT) {
            ImportScreen(
                onBack = { navController.popBackStack() },
                // 导入成功后回到首页，用户立刻能看到新题库
                onImported = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.QUIZ_LIST,
            arguments = listOf(navArgument("mode") { type = NavType.StringType }),
        ) { entry ->
            val mode = entry.arguments?.getString("mode").orEmpty()
            val kind = if (mode == QuizMode.FAVORITE.name) CollectionKind.FAVORITE else CollectionKind.WRONG
            CollectionScreen(
                kind = kind,
                onBack = { navController.popBackStack() },
                onStartPractice = { navController.navigate(Routes.quiz(QuizMode.valueOf(mode))) },
            )
        }

        composable(
            route = Routes.QUIZ,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType; defaultValue = QuizMode.BANK.name },
                navArgument("bankId") { type = NavType.StringType; defaultValue = "" },
                navArgument("tag") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val viewModel: QuizViewModel = hiltViewModel()
            val mode = entry.arguments?.getString("mode")
            val bankId = entry.arguments?.getString("bankId")
            val tag = entry.arguments?.getString("tag")
            // start() 是幂等的：屏幕旋转重建时不会重复加载
            remember(mode, bankId, tag) {
                viewModel.start(mode, bankId, tag)
                true
            }
            QuizScreen(onBack = { navController.popBackStack() })
        }
    }
}
