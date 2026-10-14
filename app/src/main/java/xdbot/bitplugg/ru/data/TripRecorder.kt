package xdbot.bitplugg.ru.data

import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xdbot.bitplugg.ru.protocol.ScooterStatus

/** One timestamped reading captured during a trip. */
data class TripSample(
    val timestampMs: Long,
    val speedKmh: Double?,
    val current: Double?,
    val batteryPercent: Int?,
    val rangeKm: Double?,
    val tripKm: Double?,
)

/**
 * Records [ScooterStatus] readings on a fixed interval while active and exposes
 * them for a live chart and CSV export. Purely local — nothing leaves the device.
 */
class TripRecorder(
    private val status: () -> ScooterStatus,
    private val intervalMs: Long = 1000L,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    private val _samples = MutableStateFlow<List<TripSample>>(emptyList())
    val samples: StateFlow<List<TripSample>> = _samples.asStateFlow()

    private var job: Job? = null

    fun start() {
        if (_recording.value) return
        _samples.value = emptyList()
        _recording.value = true
        job = scope.launch {
            while (isActive) {
                val s = status()
                _samples.value = (_samples.value + s.toSample(System.currentTimeMillis()))
                    .takeLast(MAX_SAMPLES)
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _recording.value = false
    }

    fun clear() {
        if (!_recording.value) _samples.value = emptyList()
    }

    fun csv(): String {
        val builder = StringBuilder(HEADER).append('\n')
        val first = _samples.value.firstOrNull()?.timestampMs ?: 0L
        _samples.value.forEach { s ->
            builder
                .append(s.timestampMs).append(',')
                .append(fmt((s.timestampMs - first) / 1000.0, 1)).append(',')
                .append(s.speedKmh?.let { fmt(it, 1) }.orEmpty()).append(',')
                .append(s.current?.let { fmt(it, 2) }.orEmpty()).append(',')
                .append(s.batteryPercent?.toString().orEmpty()).append(',')
                .append(s.rangeKm?.let { fmt(it, 2) }.orEmpty()).append(',')
                .append(s.tripKm?.let { fmt(it, 2) }.orEmpty())
                .append('\n')
        }
        return builder.toString()
    }

    companion object {
        const val MAX_SAMPLES = 7200

        private const val HEADER =
            "timestamp_ms,elapsed_s,speed_kmh,current_a,battery_percent,range_km,trip_km"

        private fun fmt(value: Double, decimals: Int): String =
            "%.${decimals}f".format(Locale.US, value)

        private fun ScooterStatus.toSample(ts: Long) =
            TripSample(ts, speedKmh, current, batteryPercent, rangeKm, tripKm)
    }
}
