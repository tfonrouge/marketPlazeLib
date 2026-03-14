package com.fonrouge.commonLib.sync

import com.fonrouge.base.state.SimpleState
import com.fonrouge.commonLib.model.Marketplace
import kotlinx.coroutines.*
import kotlin.time.Duration

class SyncScheduler(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) {
    data class SyncTask(
        val marketplace: Marketplace,
        val syncEngine: IMarketplaceSync<*>,
        val interval: Duration,
        val onResult: (suspend (SimpleState) -> Unit)? = null,
    )

    private val jobs = mutableMapOf<String, Job>()

    fun schedule(task: SyncTask): String {
        val taskId = "${task.marketplace._id.id}:${task.syncEngine::class.simpleName}"
        jobs[taskId]?.cancel()
        jobs[taskId] = scope.launch {
            while (isActive) {
                val result = try {
                    task.syncEngine.syncToLocal(task.marketplace)
                } catch (e: Exception) {
                    SimpleState(isOk = false, msgError = "Sync exception: ${e.message}")
                }
                task.onResult?.invoke(result)
                delay(task.interval)
            }
        }
        return taskId
    }

    fun cancel(taskId: String) {
        jobs.remove(taskId)?.cancel()
    }

    fun cancelAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    fun isRunning(taskId: String): Boolean {
        return jobs[taskId]?.isActive == true
    }

    fun activeTaskIds(): Set<String> {
        return jobs.filterValues { it.isActive }.keys
    }

    fun shutdown() {
        cancelAll()
        scope.cancel()
    }
}
