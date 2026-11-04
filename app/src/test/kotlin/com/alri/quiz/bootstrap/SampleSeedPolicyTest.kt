package com.alri.quiz.bootstrap

import com.alri.quiz.bootstrap.SampleSeedPolicy.Action
import com.alri.quiz.core.model.BankSummary
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 内置示例题库的写入策略。每一条用例都对应一类真实的用户状态，
 * 判错的后果是删掉别人的题库，或者老用户升级后看到旧题库。
 */
class SampleSeedPolicyTest {

    private fun bank(id: String, title: String = "某题库", count: Int = 5) =
        BankSummary(id = id, title = title, questionCount = count)

    private fun legacySample() = bank(
        id = SampleSeedPolicy.LEGACY_SAMPLE_BANK_ID,
        title = SampleSeedPolicy.LEGACY_SAMPLE_TITLE,
        count = SampleSeedPolicy.LEGACY_SAMPLE_QUESTION_COUNT,
    )

    private val currentSample = bank(SampleSeedPolicy.SAMPLE_BANK_ID, "Android 基础 · 30 题", 30)

    @Test
    fun `fresh install imports the sample bank`() {
        assertEquals(Action.IMPORT_SAMPLE, SampleSeedPolicy.decide(emptyList(), 0))
    }

    @Test
    fun `already handled version does nothing even when the database is empty`() {
        assertEquals(
            Action.NOTHING,
            SampleSeedPolicy.decide(emptyList(), SampleSeedPolicy.CURRENT_SEED_VERSION),
        )
    }

    @Test
    fun `v1_0_0 users with the 14-question demo bank get it replaced`() {
        assertEquals(
            Action.REPLACE_LEGACY_SAMPLE,
            SampleSeedPolicy.decide(listOf(legacySample()), recordedSeedVersion = 0),
        )
    }

    @Test
    fun `legacy sample is replaced while user-authored banks survive untouched`() {
        val mine = bank("my-own-bank", "我自己的题库", 120)
        val action = SampleSeedPolicy.decide(listOf(legacySample(), mine), recordedSeedVersion = 0)
        assertEquals(Action.REPLACE_LEGACY_SAMPLE, action)
        // 策略只挑出旧示例，用户题库不在被删集合里
        val toDelete = listOf(legacySample(), mine).filter { SampleSeedPolicy.run { it.isLegacySample() } }
        assertEquals(listOf("demo-basic"), toDelete.map { it.id })
    }

    @Test
    fun `current sample bank already present is left alone`() {
        assertEquals(Action.NOTHING, SampleSeedPolicy.decide(listOf(currentSample), recordedSeedVersion = 0))
    }

    @Test
    fun `user who deleted the sample bank is not force-fed it again`() {
        assertEquals(
            Action.NOTHING,
            SampleSeedPolicy.decide(listOf(bank("my-bank", "我的", 30)), recordedSeedVersion = 0),
        )
    }

    @Test
    fun `bank that merely reuses the demo id with different title or count is not touched`() {
        val sameIdOtherContent = bank(SampleSeedPolicy.LEGACY_SAMPLE_BANK_ID, "我的题库", 3)
        val sameTitleOtherId = bank("other-id", SampleSeedPolicy.LEGACY_SAMPLE_TITLE, 14)
        val sameCountOtherId = bank("third-id", "另一个", SampleSeedPolicy.LEGACY_SAMPLE_QUESTION_COUNT)
        assert(!SampleSeedPolicy.run { sameIdOtherContent.isLegacySample() })
        assert(!SampleSeedPolicy.run { sameTitleOtherId.isLegacySample() })
        assert(!SampleSeedPolicy.run { sameCountOtherId.isLegacySample() })
        assertEquals(
            Action.NOTHING,
            SampleSeedPolicy.decide(
                listOf(sameIdOtherContent, sameTitleOtherId, sameCountOtherId),
                recordedSeedVersion = 0,
            ),
        )
    }

    @Test
    fun `seed version is higher than the legacy one so upgrades are detected`() {
        assert(SampleSeedPolicy.CURRENT_SEED_VERSION >= 1)
    }
}
