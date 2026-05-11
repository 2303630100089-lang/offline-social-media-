package com.meshverse.app.data.local.dao

import androidx.room.*
import com.meshverse.app.data.local.entity.MapReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MapReportDao {

    @Upsert
    suspend fun upsertReport(report: MapReportEntity)

    @Query("SELECT * FROM map_reports WHERE expiresAt > :now ORDER BY timestamp DESC")
    fun getActiveReports(now: Long = System.currentTimeMillis()): Flow<List<MapReportEntity>>

    @Query("SELECT * FROM map_reports WHERE reportId = :reportId LIMIT 1")
    suspend fun getReportById(reportId: String): MapReportEntity?

    @Query("SELECT * FROM map_reports WHERE isSynced = 0 AND expiresAt > :now")
    suspend fun getUnSyncedReports(now: Long = System.currentTimeMillis()): List<MapReportEntity>

    @Query("UPDATE map_reports SET isSynced = 1 WHERE reportId = :reportId")
    suspend fun markSynced(reportId: String)

    @Query("UPDATE map_reports SET upvotes = upvotes + 1 WHERE reportId = :reportId")
    suspend fun upvote(reportId: String)

    @Query("DELETE FROM map_reports WHERE expiresAt < :now")
    suspend fun deleteExpiredReports(now: Long = System.currentTimeMillis())

    /**
     * Returns reports within a bounding box.
     * Used by the map overlay to load only visible area.
     */
    @Query("""
        SELECT * FROM map_reports
        WHERE latitude  BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLon AND :maxLon
          AND expiresAt > :now
        ORDER BY timestamp DESC
    """)
    fun getReportsInBounds(
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double,
        now: Long = System.currentTimeMillis()
    ): Flow<List<MapReportEntity>>
}
