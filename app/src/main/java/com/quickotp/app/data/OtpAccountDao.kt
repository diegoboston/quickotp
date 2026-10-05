package com.quickotp.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OtpAccountDao {
    @Query("SELECT * FROM otp_accounts ORDER BY sortOrder ASC, createdAt DESC, id DESC")
    fun observeAll(): Flow<List<OtpAccount>>

    @Query("SELECT * FROM otp_accounts WHERE id = :id")
    suspend fun getById(id: Long): OtpAccount?

    @Query("SELECT COALESCE(MIN(sortOrder), 0) FROM otp_accounts")
    suspend fun minSortOrder(): Int

    @Query("UPDATE otp_accounts SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Int)

    @Insert
    suspend fun insert(account: OtpAccount): Long

    @Update
    suspend fun update(account: OtpAccount)

    @Query("DELETE FROM otp_accounts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
