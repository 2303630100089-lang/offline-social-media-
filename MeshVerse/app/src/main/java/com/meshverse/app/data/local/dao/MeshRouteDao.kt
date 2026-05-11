package com.meshverse.app.data.local.dao

import androidx.room.*
import com.meshverse.app.data.local.entity.MeshRouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeshRouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: MeshRouteEntity)

    @Update
    suspend fun update(route: MeshRouteEntity)

    @Query("SELECT * FROM mesh_routes WHERE destinationId = :destinationId AND isValid = 1 ORDER BY routingCost ASC LIMIT 1")
    suspend fun getBestRoute(destinationId: String): MeshRouteEntity?

    @Query("SELECT * FROM mesh_routes WHERE isValid = 1")
    fun getAllValidRoutes(): Flow<List<MeshRouteEntity>>

    @Query("UPDATE mesh_routes SET isValid = 0 WHERE expiresAt < :now")
    suspend fun invalidateExpiredRoutes(now: Long = System.currentTimeMillis())

    @Query("UPDATE mesh_routes SET isValid = 0 WHERE nextHopId = :peerId")
    suspend fun invalidateRoutesVia(peerId: String)

    @Query("UPDATE mesh_routes SET lastUsed = :ts WHERE routeId = :routeId")
    suspend fun updateLastUsed(routeId: String, ts: Long = System.currentTimeMillis())

    @Query("DELETE FROM mesh_routes WHERE isValid = 0 AND lastUsed < :threshold")
    suspend fun pruneStaleRoutes(threshold: Long)
}
