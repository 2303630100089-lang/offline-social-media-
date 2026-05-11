package com.meshverse.app.data.repository

import com.meshverse.app.data.local.dao.MapReportDao
import com.meshverse.app.data.local.entity.MapReportEntity
import com.meshverse.app.domain.model.MapReport
import com.meshverse.app.domain.model.ReportCategory
import com.meshverse.app.domain.repository.MapReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MapReportRepositoryImpl @Inject constructor(
    private val mapReportDao: MapReportDao
) : MapReportRepository {

    override fun getActiveReports(): Flow<List<MapReport>> =
        mapReportDao.getActiveReports().map { it.map { e -> e.toDomain() } }

    override fun getReportsInBounds(
        minLat: Double, maxLat: Double, minLon: Double, maxLon: Double
    ): Flow<List<MapReport>> =
        mapReportDao.getReportsInBounds(minLat, maxLat, minLon, maxLon)
            .map { it.map { e -> e.toDomain() } }

    override suspend fun addReport(report: MapReport) =
        mapReportDao.upsertReport(report.toEntity())

    override suspend fun upvote(reportId: String) = mapReportDao.upvote(reportId)

    override suspend fun getUnSyncedReports(): List<MapReport> =
        mapReportDao.getUnSyncedReports().map { it.toDomain() }

    override suspend fun markSynced(reportId: String) = mapReportDao.markSynced(reportId)

    override suspend fun cleanExpiredReports() = mapReportDao.deleteExpiredReports()

    // ── Mapping ────────────────────────────────────────────────────────────

    private fun MapReportEntity.toDomain() = MapReport(
        reportId = reportId,
        authorId = authorId,
        category = runCatching { ReportCategory.valueOf(category) }.getOrDefault(ReportCategory.GENERAL),
        title = title,
        description = description,
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp,
        expiresAt = expiresAt,
        upvotes = upvotes,
        isVerified = isVerified,
        propagationHops = propagationHops
    )

    private fun MapReport.toEntity() = MapReportEntity(
        reportId = reportId,
        authorId = authorId,
        category = category.name,
        title = title,
        description = description,
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp,
        expiresAt = expiresAt,
        upvotes = upvotes,
        isVerified = isVerified,
        propagationHops = propagationHops,
        isSynced = false
    )
}
