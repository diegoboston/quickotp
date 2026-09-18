package com.quickotp.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OtpAccountDao {
    @Query("SELECT * FROM otp_accounts ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<OtpAccount>>

    @Query("SELECT * FROM otp_accounts WHERE id = :id")
    suspend fun getById(id: Long): OtpAccount?

    @Insert
    suspend fun insert(account: OtpAccount): Long

    @Update
    suspend fun update(account: OtpAccount)

    @Query("DELETE FROM otp_accounts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
