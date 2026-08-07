package com.siroha.gamespace.data.monitoring

import com.siroha.gamespace.core.privilege.PrivilegeRepository
import com.siroha.gamespace.core.privilege.PrivilegedExecResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `/proc/stat` (the standard cumulative-jiffies CPU accounting file) has
 * been unreadable by regular, non-privileged apps since Android 8 —
 * confirmed against current reports while building this phase, not
 * assumed. A shell running as root or through Shizuku isn't sandboxed the
 * same way, so it can still read it; this is exactly the kind of feature
 * `PrivilegeRepository.execPrivileged` exists for.
 *
 * Usage % comes from two samples of the same cumulative counters a fixed
 * interval apart, not a single reading — CPU-jiffy counters are a running
 * total since boot, not an instantaneous load value.
 */
@Singleton
class CpuSource @Inject constructor(
    private val privilegeRepository: PrivilegeRepository
) {
    private var previousSample: ProcStatSample? = null

    suspend fun snapshot(): CpuSnapshot {
        if (!privilegeRepository.state.value.hasElevatedAccess) {
            previousSample = null
            return CpuSnapshot.Unavailable
        }

        val execResult = privilegeRepository.execPrivileged("cat /proc/stat")
        val cpuLine = (execResult as? PrivilegedExecResult.Success)
            ?.output
            ?.firstOrNull { it.startsWith("cpu ") }
            ?: return CpuSnapshot.Unavailable

        val sample = parseProcStatLine(cpuLine) ?: return CpuSnapshot.Unavailable
        val previous = previousSample
        previousSample = sample

        // First sample after (re)gaining access — no delta to compute yet.
        if (previous == null) return CpuSnapshot.Unavailable

        val totalDelta = sample.total - previous.total
        val idleDelta = sample.idle - previous.idle
        if (totalDelta <= 0) return CpuSnapshot.Unavailable

        val usedPercent = (((totalDelta - idleDelta) * 100) / totalDelta).toInt().coerceIn(0, 100)
        return CpuSnapshot.Percent(usedPercent)
    }

    private data class ProcStatSample(val total: Long, val idle: Long)

    /** Format: "cpu  user nice system idle iowait irq softirq steal guest guest_nice" —
     *  field 4 (0-indexed after the "cpu" label) is idle; total is the sum
     *  of every field on the line. */
    private fun parseProcStatLine(line: String): ProcStatSample? {
        val fields = line.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
        if (fields.size < 4) return null
        return ProcStatSample(total = fields.sum(), idle = fields[3])
    }
}
