package com.alri.quiz.core.model

/** 一次判分的结果。判分后 UI 就靠它显示对错、正确答案和解析。 */
public data class GradeResult(
    val questionId: String,
    val selected: Set<String>,
    val correctAnswers: Set<String>,
    val isCorrect: Boolean,
)

/** 判分规则的唯一实现。放在 core:model 里，所以可以脱离 Android 直接单测。 */
public object Grading {

    /**
     * 判定一道题答得对不对。
     *
     * 规则：
     * - 单选 / 判断：选中的必须恰好等于答案集合（也就是恰好选那一个）
     * - 多选：选中集合必须与答案集合**完全相等**，多选一个、少选一个都算错
     * - 一个都没选：算错（不允许空提交当正确答案，空答案本身在导入阶段就已被拒绝）
     *
     * @throws IllegalArgumentException 当 [selected] 里出现题目不存在的选项 key 时。
     *         这类数据只可能来自程序 bug，早失败比默默判错好。
     */
    public fun grade(question: Question, selected: Set<String>): GradeResult {
        val validKeys = question.optionKeys
        val unknown = selected - validKeys
        require(unknown.isEmpty()) {
            "题目 ${question.id} 不存在选项 $unknown（合法选项：$validKeys）"
        }
        val isCorrect = when (question.type) {
            QuestionType.SINGLE, QuestionType.TRUE_FALSE ->
                selected.size == 1 && selected == question.answers
            QuestionType.MULTIPLE ->
                selected.isNotEmpty() && selected == question.answers
        }
        return GradeResult(
            questionId = question.id,
            selected = selected,
            correctAnswers = question.answers,
            isCorrect = isCorrect,
        )
    }

    /**
     * 正确率，范围 0.0..1.0。分母为 0 时返回 0.0（UI 显示 "--" ，不要显示 NaN）。
     */
    public fun accuracy(correctCount: Int, totalCount: Int): Double {
        require(correctCount >= 0) { "correctCount 不能为负：$correctCount" }
        require(totalCount >= 0) { "totalCount 不能为负：$totalCount" }
        if (totalCount == 0) return 0.0
        return correctCount.toDouble() / totalCount.toDouble()
    }

    /** 把正确率格式化成 "83%" 这种展示文本。 */
    public fun formatAccuracy(correctCount: Int, totalCount: Int): String {
        if (totalCount == 0) return "--"
        return "${(accuracy(correctCount, totalCount) * 100).toInt()}%"
    }
}
