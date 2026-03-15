package com.fonrouge.samples

import com.fonrouge.samples.collections.SampleCollections
import com.fonrouge.samples.services.ISampleItemService
import com.fonrouge.samples.services.ISampleListService
import com.fonrouge.samples.services.SampleItemService
import com.fonrouge.samples.services.SampleListService
import dev.kilua.rpc.applyRoutes
import dev.kilua.rpc.getAllServiceManagers
import dev.kilua.rpc.initRpc
import dev.kilua.rpc.registerService
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.routing.*
import io.kvision.remote.registerRemoteTypes

@Suppress("unused")
fun Application.main() {
    registerRemoteTypes()
    install(Compression)

    // InMemoryRepository — no MongoDB required
    val collections = SampleCollections()
    SeedData.seed(collections)

    routing {
        getAllServiceManagers().forEach { applyRoutes(it) }
    }

    initRpc {
        registerService<ISampleListService> { SampleListService(it, collections) }
        registerService<ISampleItemService> { SampleItemService(it, collections) }
    }
}
