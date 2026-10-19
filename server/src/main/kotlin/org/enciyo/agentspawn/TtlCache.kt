package org.enciyo.agentspawn

import arrow.core.Either
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Caches results of expensive CLI/ACP calls for a limited time. Concurrent requests for the same key are coalesced into one call;
 * only successful (Right) results are stored.
 */
class TtlCache<K : Any, V>(private val ttl: Duration) {
    private class Entry<V>(val value: V, val expiresAt: TimeSource.Monotonic.ValueTimeMark)

    private val entries = ConcurrentHashMap<K, Entry<V>>()
    private val locks = ConcurrentHashMap<K, Mutex>()

    suspend fun <E> getOrLoad(key: K, load: suspend () -> Either<E, V>): Either<E, V> {
        fresh(key)?.let { return Either.Right(it) }
        return locks.getOrPut(key) { Mutex() }.withLock {
            fresh(key)?.let { return@withLock Either.Right(it) }
            load().onRight { entries[key] = Entry(it, TimeSource.Monotonic.markNow() + ttl) }
        }
    }

    fun put(key: K, value: V) {
        entries[key] = Entry(value, TimeSource.Monotonic.markNow() + ttl)
    }

    fun invalidate(key: K) {
        entries.remove(key)
    }

    private fun fresh(key: K): V? = entries[key]?.takeIf { it.expiresAt.hasNotPassedNow() }?.value
}
