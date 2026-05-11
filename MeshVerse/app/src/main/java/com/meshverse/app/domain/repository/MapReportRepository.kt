package com.meshverse.app.domain.repository

import com.meshverse.app.domain.model.MapReport
import kotlinx.coroutines.flow.Flow

interface MapReportRepository {
    fun getActiveReports(): Flow<List<MapReport>>
    fun getReportsInBounds(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): Flow<List<MapReport>>
    suspend fun addReport(report: MapReport)
    suspend fun upvote(reportId: String)
    suspend fun getUnSyncedReports(): List<MapReport>
    suspend fun markSynced(reportId: String)
    suspend fun cleanExpiredReports()
}
