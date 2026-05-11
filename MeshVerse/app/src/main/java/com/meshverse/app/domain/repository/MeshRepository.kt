package com.meshverse.app.domain.repository

import com.meshverse.app.domain.model.MeshRoute
import kotlinx.coroutines.flow.Flow

interface MeshRepository {
    suspend fun getBestRoute(destinationId: String): MeshRoute?
    fun getAllValidRoutes(): Flow<List<MeshRoute>>
    suspend fun addRoute(route: MeshRoute)
    suspend fun invalidateRoutesVia(peerId: String)
    suspend fun pruneStaleRoutes()
}
