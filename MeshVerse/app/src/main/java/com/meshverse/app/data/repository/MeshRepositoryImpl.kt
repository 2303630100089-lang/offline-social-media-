package com.meshverse.app.data.repository

import com.meshverse.app.data.local.dao.MeshRouteDao
import com.meshverse.app.data.local.entity.MeshRouteEntity
import com.meshverse.app.domain.model.MeshRoute
import com.meshverse.app.domain.repository.MeshRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MeshRepositoryImpl @Inject constructor(
    private val meshRouteDao: MeshRouteDao
) : MeshRepository {

    override suspend fun getBestRoute(destinationId: String): MeshRoute? =
        meshRouteDao.getBestRoute(destinationId)?.toDomain()

    override fun getAllValidRoutes(): Flow<List<MeshRoute>> =
        meshRouteDao.getAllValidRoutes().map { it.map { e -> e.toDomain() } }

    override suspend fun addRoute(route: MeshRoute) =
        meshRouteDao.insert(route.toEntity())

    override suspend fun invalidateRoutesVia(peerId: String) =
        meshRouteDao.invalidateRoutesVia(peerId)

    override suspend fun pruneStaleRoutes() {
        meshRouteDao.invalidateExpiredRoutes()
        meshRouteDao.pruneStaleRoutes(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
    }

    private fun MeshRouteEntity.toDomain() = MeshRoute(
        routeId, destinationId, nextHopId, hopCount, routingCost,
        sequenceNumber, isValid, expiresAt, packetDeliveryRate
    )

    private fun MeshRoute.toEntity() = MeshRouteEntity(
        routeId = routeId,
        destinationId = destinationId,
        nextHopId = nextHopId,
        hopCount = hopCount,
        routingCost = routingCost,
        sequenceNumber = sequenceNumber,
        isValid = isValid,
        expiresAt = expiresAt,
        packetDeliveryRate = packetDeliveryRate
    )
}
