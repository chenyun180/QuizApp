package com.alri.quiz.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 品牌色板。选的是偏青蓝的「学习类 App」色调：长时间阅读不刺眼，
 * 深色模式下把同一个色相的明度压低、饱和度降一点，避免深色背景上发荧光。
 */

// Light
val BluePrimary = Color(0xFF1F6FEB)
val BlueOnPrimary = Color(0xFFFFFFFF)
val BluePrimaryContainer = Color(0xFFD7E6FF)
val BlueOnPrimaryContainer = Color(0xFF001B3E)

val TealSecondary = Color(0xFF0F7B72)
val TealOnSecondary = Color(0xFFFFFFFF)
val TealSecondaryContainer = Color(0xFFB8EFE8)
val TealOnSecondaryContainer = Color(0xFF00201C)

val AmberTertiary = Color(0xFF7A5900)
val AmberOnTertiary = Color(0xFFFFFFFF)
val AmberTertiaryContainer = Color(0xFFFFDEA6)
val AmberOnTertiaryContainer = Color(0xFF261A00)

val LightBackground = Color(0xFFFBFBFF)
val LightOnBackground = Color(0xFF1A1C1E)
val LightSurface = Color(0xFFFBFBFF)
val LightOnSurface = Color(0xFF1A1C1E)
val LightSurfaceVariant = Color(0xFFE0E2EC)
val LightOnSurfaceVariant = Color(0xFF44474E)
val LightOutline = Color(0xFF74777F)

// Dark
val BluePrimaryDark = Color(0xFFA9C7FF)
val BlueOnPrimaryDark = Color(0xFF00305F)
val BluePrimaryContainerDark = Color(0xFF004786)
val BlueOnPrimaryContainerDark = Color(0xFFD7E6FF)

val TealSecondaryDark = Color(0xFF9CD2CC)
val TealOnSecondaryDark = Color(0xFF003733)
val TealSecondaryContainerDark = Color(0xFF00504B)
val TealOnSecondaryContainerDark = Color(0xFFB8EFE8)

val AmberTertiaryDark = Color(0xFFF4BF48)
val AmberOnTertiaryDark = Color(0xFF3F2E00)
val AmberTertiaryContainerDark = Color(0xFF5C4300)
val AmberOnTertiaryContainerDark = Color(0xFFFFDEA6)

val DarkBackground = Color(0xFF1A1C1E)
val DarkOnBackground = Color(0xFFE3E2E6)
val DarkSurface = Color(0xFF1A1C1E)
val DarkOnSurface = Color(0xFFE3E2E6)
val DarkSurfaceVariant = Color(0xFF44474E)
val DarkOnSurfaceVariant = Color(0xFFC4C6D0)
val DarkOutline = Color(0xFF8E9099)

// 语义色：对/错。不放进 Material 的 colorScheme，
// 因为 Material 3 的 error 语义色只有一个（错误），没有「正确」。
val CorrectGreenLight = Color(0xFF1B6E2E)
val CorrectGreenDark = Color(0xFF7FD98C)
val WrongRedLight = Color(0xFFB3261E)
val WrongRedDark = Color(0xFFFFB4AB)
