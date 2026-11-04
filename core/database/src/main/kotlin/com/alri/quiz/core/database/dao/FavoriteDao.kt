package com.alri.quiz.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.alri.quiz.core.database.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/** 收藏。用主键 upsert/delete 实现幂等的「切换收藏」。 */
@Dao
interface FavoriteDao {

    @Upsert
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE bank_id = :bankId AND question_id = :questionId")
    suspend fun remove(bankId: String, questionId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE bank_id = :bankId AND question_id = :questionId)")
    suspend fun isFavorite(bankId: String, questionId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE bank_id = :bankId AND question_id = :questionId)")
    fun observeIsFavorite(bankId: String, questionId: String): Flow<Boolean>

    @Query("SELECT bank_id || '/' || question_id FROM favorites")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()
}
