package com.fonrouge.mercadoLibreLib.model

import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.types.StringId
import com.fonrouge.commonLib.model.MarketplaceId
import com.fonrouge.commonLib.model.Money
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/*
 * Gasto de Mercado Ads (Product Ads) por día, campaña, ad group y publicación — PublicidadMercadoAds(BRIDGE) en
 * mppErsaPack, B1 v2 y B2 aprobados (L-026). Todo lo de este archivo es puro: no hace I/O, no conoce el token y no
 * llama a ML. El anfitrión pone las respuestas y la escritura. Oráculo: la especificación ejecutable del bridge
 * (`especificacion/captura.py`), cuyos casos se portan en `MercadoLibreAdsTest`.
 */

@Serializable
enum class MercadoLibreAdsProduct { PADS, DISPLAY, BADS }

/** Procedencia de un día. El orden importa: una fuente menor nunca sobrescribe a una mayor (B1 R22). */
@Serializable
enum class MercadoLibreAdsSource(val rank: Int) { PANEL_PERIODO(0), PANEL_DIARIO(1), API(2) }

@Serializable
enum class MercadoLibreAdsDayStatus { PROVISIONAL, CERRADO }

/** Una fila del día: la publicación sale del anuncio, nunca de `ad_group_external_id` (B1 R5). `itemId` es `null` sólo
 * en datos del panel sin cruce probado, y entonces el costo es 0 (B1 R24). */
@Serializable
data class MercadoLibreAdsRow(
    val campaignId: Long?,
    val adGroupId: Long?,
    val adGroupType: String?,
    val itemId: String?,
    val logisticType: String? = null,
    val userProductId: String? = null,
    val cost: Money,
)

/** Una versión reemplazada de un día: nada se pierde al releer (B1 R7). */
@Serializable
data class MercadoLibreAdsRevision(
    val revision: Int,
    val source: MercadoLibreAdsSource,
    val total: Money,
    val contentKey: String,
    val valuesKey: String,
    val lastReadAt: String,
    val rows: List<MercadoLibreAdsRow>,
)

/** Un día leído completo y con sus controles aprobados, todavía sin guardar. */
data class MercadoLibreAdsDayRead(
    val date: String,
    val source: MercadoLibreAdsSource,
    val total: Money,
    val adGroupsTotal: Int,
    val rows: List<MercadoLibreAdsRow>,
) {
    /** Todo el contenido: sirve para saber si una relectura de la misma fuente cambió algo. */
    val contentKey: String
        get() = "total=${total.cents};" + rows.joinToString(";") {
            "${it.campaignId}|${it.adGroupId}|${it.adGroupType}|${it.itemId}|${it.logisticType}|${it.userProductId}|${it.cost.cents}"
        }

    /** Lo que se compara entre fuentes: costo por publicación con costo, y total. Los ceros son informativos. */
    val valuesKey: String
        get() = "total=${total.cents};" + rows.filter { it.itemId != null && it.cost.cents != 0L }
            .groupBy { it.itemId!! }.mapValues { (_, v) -> v.sumOf { it.cost.cents } }
            .entries.sortedBy { it.key }.joinToString(";") { "${it.key}=${it.value}" }
}

/** Identidad de un día guardado: cuenta, producto y anunciante. */
data class MercadoLibreAdsIdentity(
    val marketplaceId: MarketplaceId,
    val product: MercadoLibreAdsProduct,
    val advertiserId: Long,
    val siteId: String,
    val currencyId: String,
)

/**
 * Un día de Mercado Ads de un anunciante y un producto (colección `mercadoLibreAdsDay`), completo o nada (B1 R1).
 * `date` es el día del clic, tal como lo da ML (B1 R10).
 */
@Serializable
data class MercadoLibreAdsDay(
    override val _id: StringId<MercadoLibreAdsDay>,
    val marketplaceId: MarketplaceId,
    val product: MercadoLibreAdsProduct,
    val advertiserId: Long,
    val siteId: String,
    val currencyId: String,
    val date: String,
    val rows: List<MercadoLibreAdsRow>,
    val total: Money,
    val adGroupsTotal: Int,
    val status: MercadoLibreAdsDayStatus,
    val source: MercadoLibreAdsSource,
    val revision: Int,
    val history: List<MercadoLibreAdsRevision>,
    val firstReadAt: String,
    val lastReadAt: String,
    val contentKey: String,
    val valuesKey: String,
) : BaseDoc<StringId<MercadoLibreAdsDay>> {
    companion object {
        fun idOf(identity: MercadoLibreAdsIdentity, date: String): String =
            "${identity.marketplaceId.id}|${identity.product}|${identity.advertiserId}|$date"
    }
}

/** Resultado de armar un día: o el día completo, o el motivo por el que no se guarda. */
sealed interface MercadoLibreAdsAssembly {
    data class Ok(val read: MercadoLibreAdsDayRead) : MercadoLibreAdsAssembly
    data class Rejected(val reason: String) : MercadoLibreAdsAssembly
}

/**
 * Arma un día con las respuestas de la API, o lo rechaza (B1 R2–R6):
 * - [pages]: `ad_groups/search` del día, en orden de `offset` (R2: contiguas y completas);
 * - [campaignsDaily]: `campaigns/search` con `aggregation_type=DAILY` del mismo día, total del anunciante (C-1);
 * - [adsByAdGroup]: `ad_groups/{id}/ads` del día para cada ad group con costo (C-2 y la publicación).
 */
fun assembleAdsDay(
    date: String,
    pages: List<JsonObject>,
    campaignsDaily: JsonObject,
    adsByAdGroup: Map<Long, JsonObject>,
    source: MercadoLibreAdsSource = MercadoLibreAdsSource.API,
): MercadoLibreAdsAssembly {
    fun rejected(reason: String) = MercadoLibreAdsAssembly.Rejected(reason)
    if (pages.isEmpty()) return rejected("sin páginas de ad groups")
    val total = pages[0].paging("total") ?: return rejected("paginación: falta paging.total")
    val groups = mutableListOf<JsonObject>()
    var expected = 0
    for (page in pages) {
        val offset = page.paging("offset") ?: return rejected("paginación: falta paging.offset")
        if (offset != expected) return rejected("paginación: offset $offset en lugar de $expected")
        val results = page.results()
        groups += results
        expected += results.size
    }
    if (groups.size != total) return rejected("paginación incompleta: ${groups.size} de $total ad groups") // R2

    val daily = campaignsDaily.results().filter { (it["date"] as? JsonPrimitive)?.content == date }
    if (daily.size != 1) return rejected("C-1: el total diario de campañas no trae exactamente un renglón del $date")
    val dayTotal = daily[0]["cost"].exactCentsOrNull() ?: return rejected("C-1: costo del día ilegible")

    val rows = mutableListOf<MercadoLibreAdsRow>()
    val keys = mutableSetOf<Triple<Long?, Long, String>>()
    for (group in groups) {
        val groupId = (group["id"] as? JsonPrimitive)?.longOrNull ?: return rejected("ad group sin id")
        val groupCost = group.costCents() ?: return rejected("C-2: costo ilegible en el ad group $groupId")
        if (groupCost == 0L) continue // cero a nivel de ad group: no hace falta desglose
        val ads = adsByAdGroup[groupId]
            ?: return rejected("falta el desglose por publicación del ad group $groupId con costo ${groupCost.asMoneyText()}") // R4
        var sum = 0L
        for (ad in ads.results()) {
            val itemId = (ad["item_id"] as? JsonPrimitive)?.content ?: return rejected("anuncio sin item_id en el ad group $groupId")
            val cost = ad.costCents() ?: return rejected("costo ilegible de $itemId en el ad group $groupId")
            val campaignId = (ad["campaign_id"] as? JsonPrimitive)?.longOrNull
            val key = Triple(campaignId, groupId, itemId)
            if (!keys.add(key)) return rejected("C-3: fila repetida $key") // R6
            sum += cost
            rows += MercadoLibreAdsRow(
                campaignId = campaignId,
                adGroupId = groupId,
                adGroupType = (group["ad_group_type"] as? JsonPrimitive)?.content,
                itemId = itemId, // R5: del anuncio
                logisticType = (ad["logistic_type"] as? JsonPrimitive)?.content,
                userProductId = (ad["user_product_id"] as? JsonPrimitive)?.content,
                cost = Money(cost),
            )
        }
        if (sum != groupCost) {
            return rejected("C-2: ad group $groupId cuesta ${groupCost.asMoneyText()} y sus publicaciones suman ${sum.asMoneyText()}")
        }
    }
    val sumRows = rows.sumOf { it.cost.cents }
    if (sumRows != dayTotal) return rejected("C-1: las filas suman ${sumRows.asMoneyText()} y el total del día es ${dayTotal.asMoneyText()}")
    return MercadoLibreAdsAssembly.Ok(
        MercadoLibreAdsDayRead(date, source, Money(dayTotal), total, rows.sortedWith(compareBy({ it.itemId }, { it.adGroupId })))
    )
}

enum class MercadoLibreAdsReadAction {
    CREATED, UNCHANGED, REVISED, SOURCE_CHANGED, SOURCE_CHANGED_WITH_DISCREPANCY, IGNORED_LOWER_SOURCE
}

data class MercadoLibreAdsApplied(val day: MercadoLibreAdsDay, val action: MercadoLibreAdsReadAction)

/**
 * Qué guardar después de leer un día (B1 R7, R22). Nunca pierde una versión; una fuente menor nunca sobrescribe a una
 * mayor; si cambia la fuente, compara los valores por publicación y marca la discrepancia.
 */
fun applyAdsRead(
    saved: MercadoLibreAdsDay?,
    read: MercadoLibreAdsDayRead,
    now: String,
    identity: MercadoLibreAdsIdentity,
): MercadoLibreAdsApplied {
    if (saved == null) {
        return MercadoLibreAdsApplied(newDay(read, now, identity, revision = 1, history = emptyList(), firstReadAt = now),
            MercadoLibreAdsReadAction.CREATED)
    }
    if (read.source.rank < saved.source.rank) return MercadoLibreAdsApplied(saved, MercadoLibreAdsReadAction.IGNORED_LOWER_SOURCE)
    if (read.source == saved.source && read.contentKey == saved.contentKey) {
        return MercadoLibreAdsApplied(saved.copy(lastReadAt = now), MercadoLibreAdsReadAction.UNCHANGED)
    }
    val previous = MercadoLibreAdsRevision(saved.revision, saved.source, saved.total, saved.contentKey, saved.valuesKey,
        saved.lastReadAt, saved.rows)
    val action = when {
        read.source == saved.source -> MercadoLibreAdsReadAction.REVISED
        read.valuesKey == saved.valuesKey -> MercadoLibreAdsReadAction.SOURCE_CHANGED
        else -> MercadoLibreAdsReadAction.SOURCE_CHANGED_WITH_DISCREPANCY
    }
    return MercadoLibreAdsApplied(
        newDay(read, now, identity, saved.revision + 1, saved.history + previous, saved.firstReadAt), action)
}

private fun newDay(
    read: MercadoLibreAdsDayRead, now: String, identity: MercadoLibreAdsIdentity, revision: Int,
    history: List<MercadoLibreAdsRevision>, firstReadAt: String,
) = MercadoLibreAdsDay(
    _id = StringId(MercadoLibreAdsDay.idOf(identity, read.date)),
    marketplaceId = identity.marketplaceId, product = identity.product, advertiserId = identity.advertiserId,
    siteId = identity.siteId, currencyId = identity.currencyId, date = read.date, rows = read.rows, total = read.total,
    adGroupsTotal = read.adGroupsTotal, status = MercadoLibreAdsDayStatus.PROVISIONAL, source = read.source,
    revision = revision, history = history, firstReadAt = firstReadAt, lastReadAt = now,
    contentKey = read.contentKey, valuesKey = read.valuesKey,
)

/** Lo que el plan necesita de un día guardado. */
data class MercadoLibreAdsSavedState(
    val status: MercadoLibreAdsDayStatus,
    val source: MercadoLibreAdsSource,
    val lastReadDate: String,
)

data class MercadoLibreAdsPlan(
    /** Días por leer, en orden de prioridad. El cupo se aplica en el ciclo, en solicitudes ([MercadoLibreAdsCycle]). */
    val reads: List<String>,
    /** Días `PROVISIONAL` de la API que cumplieron la ventana de relectura: pasan a `CERRADO` sin leer. */
    val close: List<String>,
    /** Por qué no se lee nada, si es el caso. */
    val skippedBy: String? = null,
)

/**
 * Qué leer en este ciclo (B1 R8, R9, R16; B2 S4). Puro.
 * - Con [enabledFrom] `null` (interruptor apagado) no se lee nada: cero llamadas.
 * - Ventana: de hoy − [windowDays] a ayer; «ayer» sólo desde las 07:00 de Ciudad de México, cuando ML ya actualizó.
 * - Orden: días sin documento (del más viejo, el que está por salir de la ventana), días `PROVISIONAL` de la API que
 *   todavía no se releyeron hoy y no cumplen [rereadDays], y días del panel que pasan a API.
 * - Sin recorte: el cupo se mide en solicitudes HTTP y lo aplica [MercadoLibreAdsCycle] sobre este orden, porque un día
 *   cuesta un número de solicitudes que no se conoce antes de leerlo (páginas y desglose por grupo).
 */
fun planAdsReads(
    today: LocalDate,
    hourMexicoCity: Int,
    enabledFrom: String?,
    saved: Map<String, MercadoLibreAdsSavedState>,
    windowDays: Int = 90,
    rereadDays: Int,
): MercadoLibreAdsPlan {
    if (enabledFrom == null) return MercadoLibreAdsPlan(emptyList(), emptyList(), "interruptor apagado")
    val oldest = today.minus(DatePeriod(days = windowDays))
    val newest = today.minus(DatePeriod(days = if (hourMexicoCity >= 7) 1 else 2))
    val window = generateSequence(oldest) { it.plus(DatePeriod(days = 1)) }.takeWhile { it <= newest }.map { it.toString() }.toList()
    fun ageDays(date: String) = (today.toEpochDays() - LocalDate.parse(date).toEpochDays()).toInt()
    val todayText = today.toString()
    val missing = window.filter { it !in saved }
    val apiProvisional = saved.filter { (_, s) -> s.source == MercadoLibreAdsSource.API && s.status == MercadoLibreAdsDayStatus.PROVISIONAL }
    val close = apiProvisional.keys.filter { ageDays(it) >= rereadDays }.sorted()
    val reread = apiProvisional.filter { (d, s) -> ageDays(d) < rereadDays && s.lastReadDate < todayText && d in window }.keys.sorted()
    val upgrade = window.filter { d -> saved[d]?.let { it.source != MercadoLibreAdsSource.API } == true }
    return MercadoLibreAdsPlan((missing + reread + upgrade).distinct(), close)
}

/**
 * Una solicitud HTTP a Mercado Ads para leer un día (B2 S1). Cada una cuenta 1 contra el cupo del ciclo (S4, L-025): las
 * páginas de ad groups, el total diario y cada consulta de desglose por grupo, con sus páginas.
 */
sealed interface MercadoLibreAdsRequest {
    val date: String

    /** `ad_groups/search` del día desde [offset] (R2). */
    data class AdGroupsPage(override val date: String, val offset: Int) : MercadoLibreAdsRequest

    /** `campaigns/search` con `aggregation_type=DAILY`: total del anunciante (C-1). */
    data class CampaignsDaily(override val date: String) : MercadoLibreAdsRequest

    /** `ad_groups/{id}/ads` del día desde [offset]: publicación y costo (C-2, R5). */
    data class AdGroupAds(override val date: String, val adGroupId: Long, val offset: Int) : MercadoLibreAdsRequest
}

/** Lo que devolvió una solicitud (B2 S1). El anfitrión traduce el estado HTTP; esta lógica decide qué hacer. */
sealed interface MercadoLibreAdsResponse {
    data class Ok(val body: JsonObject) : MercadoLibreAdsResponse

    /** 401, 403, 429, 400 u otro 4xx, o sin token: el ciclo se detiene sin escribir nada más (R14, MarketPlace D42). */
    data class Cut(val status: Int, val reason: String) : MercadoLibreAdsResponse

    /** 5xx, tiempo agotado o cuerpo ilegible: se salta el día y el ciclo sigue. */
    data class Retryable(val reason: String) : MercadoLibreAdsResponse

    /** 404 en `advertisers`: el producto no está habilitado para la cuenta (medido: DISPLAY y BADS). */
    data object NotEnabled : MercadoLibreAdsResponse
}

/**
 * Lee un día solicitud por solicitud, sin I/O: [next] dice cuál sigue y [accept] recibe su cuerpo. Cuando [next] da
 * `null`, [assemble] arma el día con [assembleAdsDay]. Orden: páginas de ad groups hasta `paging.total`, total diario y
 * desglose de cada ad group con costo, con sus páginas. Si la paginación de ad groups ya no cuadra, no pide nada más: el
 * día se rechazará por R2 y no tiene caso gastar cupo.
 */
class MercadoLibreAdsDayReader(val date: String) {
    private val pages = mutableListOf<JsonObject>()
    private var pagesDone = false
    private var pagesValid = false
    private var daily: JsonObject? = null
    private var groups: List<Long> = emptyList()
    private var groupIndex = 0
    private val ads = linkedMapOf<Long, MutableList<JsonElement>>()
    private var adsOffset = 0
    private var pending: MercadoLibreAdsRequest? = null

    /** La solicitud que sigue, o `null` si el día ya se puede armar. */
    fun next(): MercadoLibreAdsRequest? {
        pending = when {
            !pagesDone -> MercadoLibreAdsRequest.AdGroupsPage(date, pages.sumOf { it.results().size })
            !pagesValid -> null
            daily == null -> MercadoLibreAdsRequest.CampaignsDaily(date)
            groupIndex < groups.size -> MercadoLibreAdsRequest.AdGroupAds(date, groups[groupIndex], adsOffset)
            else -> null
        }
        return pending
    }

    /** El cuerpo de la solicitud que dio [next]. */
    fun accept(body: JsonObject) {
        when (val request = pending ?: error("accept sin una solicitud pendiente del $date")) {
            is MercadoLibreAdsRequest.AdGroupsPage -> {
                pages += body
                val total = pages[0].paging("total")
                val loaded = pages.sumOf { it.results().size }
                if (total == null || body.results().isEmpty() || loaded >= total) {
                    pagesDone = true
                    var offset = 0
                    pagesValid = total != null && loaded == total && pages.all { page ->
                        (page.paging("offset") == offset).also { offset += page.results().size }
                    }
                    groups = pages.flatMap { it.results() }
                        .filter { (it.costCents() ?: 0L) != 0L }
                        .mapNotNull { (it["id"] as? JsonPrimitive)?.longOrNull }
                }
            }
            is MercadoLibreAdsRequest.CampaignsDaily -> daily = body
            is MercadoLibreAdsRequest.AdGroupAds -> {
                val results = body["results"] as? JsonArray ?: JsonArray(emptyList())
                ads.getOrPut(request.adGroupId) { mutableListOf() }.addAll(results)
                adsOffset += results.size
                val total = body.paging("total")
                if (total == null || results.isEmpty() || adsOffset >= total) {
                    groupIndex++
                    adsOffset = 0
                }
            }
        }
        pending = null
    }

    /** Arma el día con lo recibido. Una paginación que no cuadra se rechaza por R2 antes de mirar lo demás. */
    fun assemble(): MercadoLibreAdsAssembly = assembleAdsDay(
        date, pages, daily ?: JsonObject(emptyMap()),
        ads.mapValues { (_, results) -> JsonObject(mapOf("results" to JsonArray(results))) }, MercadoLibreAdsSource.API,
    )
}

data class MercadoLibreAdsCycleReport(
    val actions: Map<String, MercadoLibreAdsReadAction> = emptyMap(),
    val rejected: Map<String, String> = emptyMap(),
    val retryable: List<String> = emptyList(),
    /** Día y estado HTTP que cortaron el ciclo (401, 403, 429, 400…). */
    val cut: Pair<String, Int>? = null,
    /** Día que quedó a medias porque se acabó el cupo: no se escribió nada de él y el próximo ciclo lo retoma. */
    val quotaExhaustedAt: String? = null,
    /** Día que no cabe ni en el cupo completo de un ciclo: con este cupo nunca se termina (alerta, L-025). */
    val exceedsQuota: String? = null,
    /** Días del plan que ni se empezaron, por corte o por cupo. */
    val notStarted: Int = 0,
    /** Solicitudes HTTP hechas. Nunca más que el cupo. */
    val requests: Int = 0,
    /** Por qué no se intentó nada (interruptor apagado). */
    val skippedBy: String? = null,
    /** Días `PROVISIONAL` de la API que cumplieron la ventana de relectura y pasaron a `CERRADO` sin leer (R8). */
    val closed: List<String> = emptyList(),
    /**
     * Días que no se escribieron porque otro escritor cambió el documento entre la lectura y la escritura (escritura
     * condicionada a la revisión leída). Lo de ese otro escritor queda intacto; el siguiente ciclo vuelve a leer el día.
     */
    val conflicts: List<String> = emptyList(),
)

/**
 * Un ciclo sobre [dates] (ya ordenados por [planAdsReads]) con un cupo de [quotaRequests] solicitudes HTTP. Sin I/O: el
 * anfitrión pide cada [Step.Request] y guarda cada [Step.DayReady] ([runAdsCycle]).
 * - Un día se escribe sólo completo y con sus controles (R1); un día a medias no escribe nada, así que lo guardado antes
 *   (la última revisión válida) queda intacto.
 * - Un corte (401, 403, 429, 400) detiene todo; un reintentable o un rechazo sólo saltan ese día.
 * - El cupo nunca se rebasa: si se acaba a mitad de un día, el día queda sin escribir ([MercadoLibreAdsCycleReport.quotaExhaustedAt])
 *   y el siguiente ciclo lo retoma, porque el plan lo vuelve a poner. No se empieza un día con menos de
 *   [MIN_REQUESTS_PER_DAY] solicitudes disponibles.
 */
class MercadoLibreAdsCycle(private val dates: List<String>, private val quotaRequests: Int) {
    sealed interface Step {
        data class Request(val request: MercadoLibreAdsRequest) : Step
        data class DayReady(val read: MercadoLibreAdsDayRead) : Step
        data object Done : Step
    }

    private var index = 0
    private var reader: MercadoLibreAdsDayReader? = null
    private var requestsAtDayStart = 0
    private var stopped = false

    var report: MercadoLibreAdsCycleReport = MercadoLibreAdsCycleReport()
        private set

    fun next(): Step {
        while (!stopped) {
            val current = reader ?: startNextDay() ?: break
            val request = current.next()
            if (request == null) {
                reader = null
                when (val assembly = current.assemble()) {
                    is MercadoLibreAdsAssembly.Ok -> return Step.DayReady(assembly.read)
                    is MercadoLibreAdsAssembly.Rejected -> report = report.copy(rejected = report.rejected + (current.date to assembly.reason))
                }
                continue
            }
            if (report.requests >= quotaRequests) {
                stop(partial = current.date)
                break
            }
            report = report.copy(requests = report.requests + 1)
            return Step.Request(request)
        }
        return Step.Done
    }

    fun accept(response: MercadoLibreAdsResponse) {
        val current = reader ?: error("accept sin un día en curso")
        when (response) {
            is MercadoLibreAdsResponse.Ok -> current.accept(response.body)
            is MercadoLibreAdsResponse.Cut -> {
                reader = null
                report = report.copy(cut = current.date to response.status)
                stop(partial = null)
            }
            MercadoLibreAdsResponse.NotEnabled -> {
                reader = null
                report = report.copy(cut = current.date to 404)
                stop(partial = null)
            }
            is MercadoLibreAdsResponse.Retryable -> {
                reader = null
                report = report.copy(retryable = report.retryable + current.date)
            }
        }
    }

    fun recorded(date: String, action: MercadoLibreAdsReadAction) {
        report = report.copy(actions = report.actions + (date to action))
    }

    fun conflicted(date: String) {
        report = report.copy(conflicts = report.conflicts + date)
    }

    private fun startNextDay(): MercadoLibreAdsDayReader? {
        if (index >= dates.size) return null
        if (quotaRequests - report.requests < MIN_REQUESTS_PER_DAY) {
            stop(partial = null)
            return null
        }
        requestsAtDayStart = report.requests
        return MercadoLibreAdsDayReader(dates[index++]).also { reader = it }
    }

    private fun stop(partial: String?) {
        stopped = true
        reader = null
        report = report.copy(
            quotaExhaustedAt = partial ?: report.quotaExhaustedAt,
            exceedsQuota = if (partial != null && requestsAtDayStart == 0) partial else report.exceedsQuota,
            notStarted = dates.size - index,
        )
    }

    companion object {
        /** Lo mínimo que cuesta un día: una página de ad groups y el total diario. */
        const val MIN_REQUESTS_PER_DAY = 2
    }
}

/**
 * Corre [cycle]: pide cada solicitud con [get] y guarda cada día completo con [applyAdsRead]. `inline` para que el
 * anfitrión pase lambdas que suspenden (HTTP y Mongo) sin que esta lógica dependa de corrutinas. Lo guardado antes nunca
 * se borra. [store] escribe **condicionado**: recibe el día nuevo y el que se leyó (`null` si no había) y devuelve
 * `false` si el documento ya no es el leído (otro escritor llegó antes); ese día queda en
 * [MercadoLibreAdsCycleReport.conflicts] y no en las acciones. El ciclo sólo lee la API, la fuente mayor, así que [MercadoLibreAdsReadAction.IGNORED_LOWER_SOURCE] no ocurre
 * aquí; la guarda queda por si un día se le pasa otra fuente.
 */
inline fun runAdsCycle(
    cycle: MercadoLibreAdsCycle,
    now: String,
    identity: MercadoLibreAdsIdentity,
    get: (MercadoLibreAdsRequest) -> MercadoLibreAdsResponse,
    load: (String) -> MercadoLibreAdsDay?,
    store: (day: MercadoLibreAdsDay, expected: MercadoLibreAdsDay?) -> Boolean,
): MercadoLibreAdsCycleReport {
    while (true) {
        when (val step = cycle.next()) {
            is MercadoLibreAdsCycle.Step.Request -> cycle.accept(get(step.request))
            is MercadoLibreAdsCycle.Step.DayReady -> {
                val previous = load(step.read.date)
                val applied = applyAdsRead(previous, step.read, now, identity)
                if (applied.action == MercadoLibreAdsReadAction.IGNORED_LOWER_SOURCE || store(applied.day, previous)) {
                    cycle.recorded(step.read.date, applied.action)
                } else {
                    cycle.conflicted(step.read.date)
                }
            }
            MercadoLibreAdsCycle.Step.Done -> return cycle.report
        }
    }
}

/** Un reporte de origen de un periodo: nombre del archivo y su SHA-256 (B1 R20). */
@Serializable
data class MercadoLibreAdsEvidence(
    val file: String,
    val digest: String,
) {
    init {
        require(file.isNotBlank()) { "evidencia sin archivo" }
        require(digest.length == 64 && digest.all { it in '0'..'9' || it in 'a'..'f' }) { "huella SHA-256 inválida para $file" }
    }
}

/**
 * Gasto de Mercado Ads de un intervalo sin granularidad diaria (colección `mercadoLibreAdsPeriod`, B1 R20 y R21): un reporte
 * del panel por mes o por periodo. Se concilia completo contra los CFDI de sus días de clic; nunca se reparte en días ni
 * cruza un mes. Lo escribe sólo el importador del historial (B4-12), una vez, con la huella del archivo de origen.
 */
@Serializable
data class MercadoLibreAdsPeriod(
    override val _id: StringId<MercadoLibreAdsPeriod>,
    val marketplaceId: MarketplaceId,
    val product: MercadoLibreAdsProduct,
    val advertiserId: Long,
    val siteId: String,
    val currencyId: String,
    /** Primer y último día del clic, inclusive (`AAAA-MM-DD`). */
    val from: String,
    val to: String,
    /** `mensual` o `periodo`, como lo trae el reporte del panel. */
    val granularity: String,
    val rows: List<MercadoLibreAdsRow>,
    val total: Money,
    val source: MercadoLibreAdsSource,
    /**
     * Los reportes de origen, cada uno con su SHA-256 (procedencia, R20). Un periodo puede venir de más de un reporte del
     * panel (por ejemplo, uno mensual y uno total que lo controla); se guardan todos, nunca unidos en un texto.
     */
    val evidence: List<MercadoLibreAdsEvidence>,
    val importedAt: String,
) : BaseDoc<StringId<MercadoLibreAdsPeriod>> {
    init {
        require(from <= to) { "periodo invertido: $from > $to" }
        require(from.take(7) == to.take(7)) { "un periodo no cruza un mes (R21): $from a $to" }
        require(evidence.isNotEmpty()) { "un periodo necesita al menos un reporte de origen (R20): $from a $to" }
    }

    companion object {
        fun idOf(identity: MercadoLibreAdsIdentity, from: String, to: String): String =
            "${identity.marketplaceId.id}|${identity.product}|${identity.advertiserId}|$from|$to"
    }
}

private fun JsonObject.paging(key: String): Int? =
    ((this["paging"] as? JsonObject)?.get(key) as? JsonPrimitive)?.longOrNull?.toInt()

private fun JsonObject.results(): List<JsonObject> =
    (this["results"] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

/** Costo en centavos exactos: sin `metrics` o sin `cost` es 0; un costo presente e ilegible es `null` (rechazo). */
private fun JsonObject.costCents(): Long? {
    val metrics = this["metrics"] as? JsonObject ?: return 0L
    val cost = metrics["cost"] ?: return 0L
    return cost.exactCentsOrNull()
}

private fun Long.asMoneyText(): String = "${this / 100}.${(this % 100).toString().padStart(2, '0')}"
