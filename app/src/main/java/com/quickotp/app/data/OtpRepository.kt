package com.quickotp.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class OtpRepository(private val db: AppDatabase) {
    private val dao = db.otpAccountDao()

    fun observeAll(): Flow<List<OtpAccount>> = dao.observeAll()

    suspend fun getById(id: Long): OtpAccount? = dao.getById(id)

    /** New entries go to the top of the list. */
    suspend fun insert(account: OtpAccount): Long = db.withTransaction {
        dao.insert(account.copy(sortOrder = dao.minSortOrder() - 1))
    }

    suspend fun update(account: OtpAccount) = dao.update(account)

    suspend fun delete(id: Long) = dao.deleteById(id)

    /** Stores [ids] as the new list order, first id on top. */
    suspend fun reorder(ids: List<Long>) = db.withTransaction {
        ids.forEachIndexed { index, id -> dao.setSortOrder(id, index) }
    }
}
