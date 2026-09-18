package com.quickotp.app.data

import kotlinx.coroutines.flow.Flow

class OtpRepository(private val dao: OtpAccountDao) {
    fun observeAll(): Flow<List<OtpAccount>> = dao.observeAll()

    suspend fun getById(id: Long): OtpAccount? = dao.getById(id)

    suspend fun insert(account: OtpAccount): Long = dao.insert(account)

    suspend fun update(account: OtpAccount) = dao.update(account)

    suspend fun delete(id: Long) = dao.deleteById(id)
}
