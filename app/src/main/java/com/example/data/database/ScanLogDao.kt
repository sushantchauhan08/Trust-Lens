package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanLogDao {
    @Query("SELECT * FROM scan_logs ORDER BY timestamp DESC")
    fun getAllScanLogs(): Flow<List<ScanLog>>

    @Query("SELECT * FROM scan_logs WHERE id = :id")
    fun getScanLogById(id: Int): Flow<ScanLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanLog(scanLog: ScanLog): Long

    @Delete
    suspend fun deleteScanLog(scanLog: ScanLog)

    @Query("DELETE FROM scan_logs")
    suspend fun clearAllScanLogs()
}
