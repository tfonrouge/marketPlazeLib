import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.state.SimpleState
import com.fonrouge.commonLib.model.Marketplace
import com.fonrouge.commonLib.sync.IMarketplaceSync
import com.fonrouge.commonLib.sync.SyncScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class SyncSchedulerTest {

    private class CountingSyncEngine : IMarketplaceSync<BaseDoc<*>> {
        var syncCount = 0
        override suspend fun syncToLocal(marketplace: Marketplace): SimpleState {
            syncCount++
            return SimpleState(isOk = true, "Sync #$syncCount")
        }
        override suspend fun syncToRemote(marketplace: Marketplace): SimpleState =
            SimpleState(isOk = false, "not implemented")
    }

    private class FailingSyncEngine : IMarketplaceSync<BaseDoc<*>> {
        override suspend fun syncToLocal(marketplace: Marketplace): SimpleState {
            throw RuntimeException("API connection failed")
        }
        override suspend fun syncToRemote(marketplace: Marketplace): SimpleState =
            SimpleState(isOk = false, "not implemented")
    }

    private val testMarketplace = Marketplace(
        name = "Test Store",
        type = "Test",
    )

    @Test
    fun `schedule runs sync task`() = runBlocking {
        val scheduler = SyncScheduler()
        val engine = CountingSyncEngine()

        scheduler.schedule(SyncScheduler.SyncTask(
            marketplace = testMarketplace,
            syncEngine = engine,
            interval = 50.milliseconds,
        ))

        delay(130.milliseconds)
        scheduler.shutdown()
        assertTrue(engine.syncCount >= 2, "Should have synced at least twice, got ${engine.syncCount}")
    }

    @Test
    fun `schedule returns task ID`() {
        val scheduler = SyncScheduler()
        val engine = CountingSyncEngine()

        val taskId = scheduler.schedule(SyncScheduler.SyncTask(
            marketplace = testMarketplace,
            syncEngine = engine,
            interval = 1000.milliseconds,
        ))

        assertTrue(taskId.contains(testMarketplace._id.id), "Task ID should contain marketplace ID: $taskId")
        assertTrue(taskId.contains("CountingSyncEngine"), "Task ID should contain engine class name: $taskId")
        scheduler.shutdown()
    }

    @Test
    fun `isRunning returns true for active task`() = runBlocking {
        val scheduler = SyncScheduler()
        val engine = CountingSyncEngine()

        val taskId = scheduler.schedule(SyncScheduler.SyncTask(
            marketplace = testMarketplace,
            syncEngine = engine,
            interval = 100.milliseconds,
        ))

        delay(10.milliseconds)
        assertTrue(scheduler.isRunning(taskId))

        scheduler.shutdown()
    }

    @Test
    fun `cancel stops a task`() = runBlocking {
        val scheduler = SyncScheduler()
        val engine = CountingSyncEngine()

        val taskId = scheduler.schedule(SyncScheduler.SyncTask(
            marketplace = testMarketplace,
            syncEngine = engine,
            interval = 50.milliseconds,
        ))

        delay(30.milliseconds)
        scheduler.cancel(taskId)
        delay(10.milliseconds)

        assertFalse(scheduler.isRunning(taskId))
        scheduler.shutdown()
    }

    @Test
    fun `cancelAll stops all tasks`() = runBlocking {
        val scheduler = SyncScheduler()

        val id1 = scheduler.schedule(SyncScheduler.SyncTask(
            marketplace = testMarketplace,
            syncEngine = CountingSyncEngine(),
            interval = 100.milliseconds,
        ))
        val id2 = scheduler.schedule(SyncScheduler.SyncTask(
            marketplace = Marketplace(name = "Store 2", type = "Test"),
            syncEngine = CountingSyncEngine(),
            interval = 100.milliseconds,
        ))

        delay(10.milliseconds)
        assertEquals(2, scheduler.activeTaskIds().size)

        scheduler.cancelAll()
        delay(10.milliseconds)

        assertFalse(scheduler.isRunning(id1))
        assertFalse(scheduler.isRunning(id2))
        assertTrue(scheduler.activeTaskIds().isEmpty())

        scheduler.shutdown()
    }

    @Test
    fun `onResult callback receives sync results`() = runBlocking {
        val scheduler = SyncScheduler()
        val engine = CountingSyncEngine()
        val results = mutableListOf<SimpleState>()

        scheduler.schedule(SyncScheduler.SyncTask(
            marketplace = testMarketplace,
            syncEngine = engine,
            interval = 50.milliseconds,
            onResult = { results.add(it) },
        ))

        delay(130.milliseconds)
        scheduler.shutdown()

        assertTrue(results.size >= 2, "Should have at least 2 results, got ${results.size}")
        assertTrue(results.all { it.state == com.fonrouge.base.state.State.Ok })
    }

    @Test
    fun `failing sync engine does not crash scheduler`() = runBlocking {
        val scheduler = SyncScheduler()
        val failingEngine = FailingSyncEngine()
        val results = mutableListOf<SimpleState>()

        val taskId = scheduler.schedule(SyncScheduler.SyncTask(
            marketplace = testMarketplace,
            syncEngine = failingEngine,
            interval = 50.milliseconds,
            onResult = { results.add(it) },
        ))

        delay(130.milliseconds)
        assertTrue(scheduler.isRunning(taskId), "Scheduler should keep running despite failures")
        assertTrue(results.any { it.hasError }, "Should have captured error results")

        scheduler.shutdown()
    }

    @Test
    fun `reschedule replaces existing task`() = runBlocking {
        val scheduler = SyncScheduler()
        val engine1 = CountingSyncEngine()
        val engine2 = CountingSyncEngine()

        val task = SyncScheduler.SyncTask(
            marketplace = testMarketplace,
            syncEngine = engine1,
            interval = 50.milliseconds,
        )
        val taskId1 = scheduler.schedule(task)

        delay(70.milliseconds)
        val countAfterFirst = engine1.syncCount

        // Reschedule with same marketplace — should cancel first
        val task2 = task.copy(syncEngine = engine2)
        val taskId2 = scheduler.schedule(task2)

        assertEquals(taskId1, taskId2, "Same task ID since same marketplace + engine class")
        delay(70.milliseconds)

        // engine1 should have stopped incrementing
        assertEquals(countAfterFirst, engine1.syncCount, "First engine should have stopped")
        assertTrue(engine2.syncCount >= 1, "Second engine should have started")

        scheduler.shutdown()
    }
}
