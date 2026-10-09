package com.example.data.repository

import com.example.data.database.ScanLog
import com.example.data.database.ScanLogDao
import kotlinx.coroutines.flow.Flow

class ScanLogRepository(private val scanLogDao: ScanLogDao) {
    val allScanLogs: Flow<List<ScanLog>> = scanLogDao.getAllScanLogs()

    fun getScanLogById(id: Int): Flow<ScanLog?> {
        return scanLogDao.getScanLogById(id)
    }

    suspend fun insertScanLog(scanLog: ScanLog): Long {
        return scanLogDao.insertScanLog(scanLog)
    }

    suspend fun deleteScanLog(scanLog: ScanLog) {
        scanLogDao.deleteScanLog(scanLog)
    }

    suspend fun clearScanLogs() {
        scanLogDao.clearAllScanLogs()
    }
}
