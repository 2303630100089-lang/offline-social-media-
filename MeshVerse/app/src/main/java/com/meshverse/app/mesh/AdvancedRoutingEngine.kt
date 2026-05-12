package com.meshverse.app.mesh

import android.util.Log
import com.meshverse.app.domain.model.Peer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced Routing Engine – Phase 2
 *
 * Extends the base AODV-like routing in [MeshNetworkManager] with:
 *
 * 1. **Link-Quality Aware Routing** – Maintains per-link quality scores based on
 *    delivery success rate, round-trip latency, and peer reputation. Routes are
 *    selected to maximise quality rather than simply minimise hop count.
 *
 * 2. **Reputation-Based Relay Selection** – Peers that consistently relay packets
 *    reliably earn higher reputation scores and are preferred as next-hop relays.
 *
 * 3. **Adaptive Path Switching** – Monitors link degradation and proactively
 *    switches to a better path before packets are lost.
 *
 * 4. **Congestion-Aware Load Balancing** – When multiple equal-quality paths
 *    exist, traffic is spread across them to prevent bottlenecks.
 *
 * 5. **Geographic Routing Assist** – Uses GPS coordinates (when available) to
 *    prefer relays that are physically closer to the destination.
 */
@Singleton
class AdvancedRoutingEngine @Inject constructor() {
    companion object {
        private const val TAG = "AdvancedRoutingEngine"

        // Quality score weights
        private const val WEIGHT_SUCCESS_RATE = 0.50f
        private const val WEIGHT_LATENCY = 0.25f
        private const val WEIGHT_REPUTATION = 0.25f

        // Reputation decay per routing cycle (prevents stale high scores)
        private const val REPUTATION_DECAY = 0.98f

        // Minimum quality threshold to use a link
        private const val MIN_LINK_QUALITY = 0.10f

        // How often to recompute and prune route quality (ms)
        private const val QUALITY_UPDATE_INTERVAL_MS = 20_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // peerId → LinkQualityStats
    private val linkQuality = ConcurrentHashMap<String, LinkQualityStats>()

    // peerId → reputation score [0.0, 1.0]
    private val reputationScores = ConcurrentHashMap<String, Float>()

    // peerId → in-flight packet count (congestion metric)
    private val congestionCounters = ConcurrentHashMap<String, Int>()

    private var isRunning = false

    /** Start the routing engine background tasks. */
    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            while (isRunning) {
                delay(QUALITY_UPDATE_INTERVAL_MS)
                decayReputations()
                pruneStaleLinks()
            }
        }
        Log.d(TAG, "AdvancedRoutingEngine started")
    }

    fun stop() {
        isRunning = false
    }

    // ── Route selection ────────────────────────────────────────────────────

    /**
     * Select the best next-hop peer for a given destination from available candidates.
     *
     * @param destinationId  Target node ID
     * @param candidates     Directly reachable peers that could relay the packet
     * @param destLatitude   Optional destination latitude for geo-assist
     * @param destLongitude  Optional destination longitude for geo-assist
     * @return Best next-hop peer, or null if no suitable relay exists
     */
    fun selectNextHop(
        destinationId: String,
        candidates: List<Peer>,
        destLatitude: Double? = null,
        destLongitude: Double? = null
    ): Peer? {
        if (candidates.isEmpty()) return null

        // Direct connection takes priority
        val direct = candidates.firstOrNull { it.peerId == destinationId }
        if (direct != null) return direct

        return candidates
            .map { peer -> peer to computeScore(peer, destLatitude, destLongitude) }
            .filter { (_, score) -> score >= MIN_LINK_QUALITY }
            .maxByOrNull { (_, score) -> score }
            ?.first
    }

    /**
     * Compute a composite quality score for a candidate relay peer.
     * Score ∈ [0.0, 1.0] – higher is better.
     */
    private fun computeScore(peer: Peer, destLat: Double?, destLon: Double?): Float {
        val stats = linkQuality[peer.peerId] ?: LinkQualityStats()
        val reputation = reputationScores[peer.peerId] ?: 0.5f
        val congestion = congestionCounters[peer.peerId] ?: 0

        val successScore = stats.successRate
        val latencyScore = if (stats.avgLatencyMs <= 0) 0.5f
                           else (1f - (stats.avgLatencyMs / 2000f)).coerceIn(0f, 1f)
        val reputationScore = reputation
        val congestionPenalty = (congestion / 10f).coerceIn(0f, 0.5f)

        var score = WEIGHT_SUCCESS_RATE * successScore +
                    WEIGHT_LATENCY * latencyScore +
                    WEIGHT_REPUTATION * reputationScore -
                    congestionPenalty

        // Geographic bonus: prefer peers physically closer to destination
        if (destLat != null && destLon != null &&
            peer.latitude != null && peer.longitude != null) {
            val dist = haversineKm(peer.latitude, peer.longitude, destLat, destLon)
            val geoBonus = (1f - (dist / 100f).toFloat()).coerceIn(0f, 0.2f)
            score += geoBonus
        }

        return score.coerceIn(0f, 1f)
    }

    // ── Feedback callbacks ─────────────────────────────────────────────────

    /** Record a successful packet delivery through [peerId] with round-trip [latencyMs]. */
    fun onPacketDelivered(peerId: String, latencyMs: Long) {
        linkQuality.compute(peerId) { _, stats ->
            (stats ?: LinkQualityStats()).recordSuccess(latencyMs)
        }
        reputationScores.compute(peerId) { _, rep ->
            ((rep ?: 0.5f) + 0.02f).coerceAtMost(1f)
        }
        congestionCounters.compute(peerId) { _, c -> ((c ?: 1) - 1).coerceAtLeast(0) }
        Log.v(TAG, "Packet delivered via $peerId in ${latencyMs}ms")
    }

    /** Record a failed packet transmission attempt through [peerId]. */
    fun onPacketFailed(peerId: String) {
        linkQuality.compute(peerId) { _, stats ->
            (stats ?: LinkQualityStats()).recordFailure()
        }
        reputationScores.compute(peerId) { _, rep ->
            ((rep ?: 0.5f) - 0.05f).coerceAtLeast(0f)
        }
        congestionCounters.compute(peerId) { _, c -> ((c ?: 0) + 1).coerceAtMost(20) }
        Log.v(TAG, "Packet failed via $peerId")
    }

    /** Called when a new packet is in-flight through [peerId]. */
    fun onPacketInFlight(peerId: String) {
        congestionCounters.compute(peerId) { _, c -> ((c ?: 0) + 1).coerceAtMost(20) }
    }

    // ── Maintenance ────────────────────────────────────────────────────────

    private fun decayReputations() {
        reputationScores.replaceAll { _, score -> (score * REPUTATION_DECAY).coerceAtLeast(0f) }
    }

    private fun pruneStaleLinks() {
        val staleThresholdMs = System.currentTimeMillis() - 300_000L
        val stale = linkQuality.entries
            .filter { it.value.lastUpdatedMs < staleThresholdMs }
            .map { it.key }
        stale.forEach { peerId ->
            linkQuality.remove(peerId)
            reputationScores.remove(peerId)
            congestionCounters.remove(peerId)
        }
        if (stale.isNotEmpty()) Log.d(TAG, "Pruned ${stale.size} stale link(s)")
    }

    // ── Geo helper ─────────────────────────────────────────────────────────

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).let { it * it }
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}

/** Per-link delivery statistics tracked by the advanced routing engine. */
data class LinkQualityStats(
    val totalPackets: Int = 0,
    val successPackets: Int = 0,
    val avgLatencyMs: Long = 0L,
    val lastUpdatedMs: Long = System.currentTimeMillis()
) {
    val successRate: Float
        get() = if (totalPackets == 0) 0.5f else successPackets.toFloat() / totalPackets

    fun recordSuccess(latencyMs: Long): LinkQualityStats {
        val newTotal = totalPackets + 1
        val newSuccess = successPackets + 1
        val newAvg = if (avgLatencyMs == 0L) latencyMs
                     else (avgLatencyMs * 7 + latencyMs) / 8  // EWMA
        return copy(
            totalPackets = newTotal,
            successPackets = newSuccess,
            avgLatencyMs = newAvg,
            lastUpdatedMs = System.currentTimeMillis()
        )
    }

    fun recordFailure(): LinkQualityStats = copy(
        totalPackets = totalPackets + 1,
        lastUpdatedMs = System.currentTimeMillis()
    )
}
