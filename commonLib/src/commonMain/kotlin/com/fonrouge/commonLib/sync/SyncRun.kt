package com.fonrouge.commonLib.sync

import com.fonrouge.commonLib.model.MarketplaceId
import kotlinx.serialization.Serializable

/** Que se estaba haciendo cuando ocurrio la corrida. */
enum class SyncKind {
    /** Comparar remoto contra ERP sin escribir nada. */
    RECONCILE,

    /** Empujar altas y cambios de producto. */
    PUSH_PRODUCTS,

    /** Empujar existencias por ubicacion. */
    PUSH_INVENTORY,

    /** Empujar o refrescar medios. */
    PUSH_MEDIA,

    /** Colecciones. */
    COLLECTIONS,
}

/** Como termino. */
enum class SyncOutcome {
    /** Hizo lo que debia, sin errores. */
    OK,

    /** Hizo parte: hay resultados utiles **y** errores. Es el desenlace mas comun y el peor de ocultar. */
    PARTIAL,

    /** No hizo nada util. */
    FAILED,

    /** No se ejecuto porque faltaba una precondicion — configuracion, mapeos, credenciales. */
    SKIPPED,
}

/** Un error concreto, atribuido a algo. Sin `item` es un error de la corrida entera. */
@Serializable
data class SyncRunError(
    /** SKU, `externalId`, handle… lo que permita ir al elemento. */
    val item: String? = null,
    val message: String,
)

/**
 * Lo que paso en **una** ejecucion de sincronizacion, para poder responder despues.
 *
 * ## Por que existe
 *
 * Un consumidor guardaba el ultimo reporte en un `ConcurrentHashMap` que la tarea periodica ni
 * siquiera escribia: la interfaz podia no tener reporte aunque la conciliacion hubiera corrido, y
 * al reiniciar no quedaba rastro de nada. Sin historial no se puede contestar «¿esto lleva fallando
 * desde el jueves?», que es justo lo que se pregunta cuando algo va mal.
 *
 * ## Por que es agnostica del marketplace
 *
 * Vive en `commonLib` y no conoce `SyncReport` de shopifyLib: la direccion de dependencias es
 * shopifyLib → commonLib, y un historial que sólo sirviera para Shopify obligaria a inventar otro
 * para MercadoLibre y otro para Amazon. Cada integracion traduce su reporte a esta forma.
 *
 * [finishedAt] es epoch millis y no un tipo de fecha para no atar el puerto a una libreria de
 * tiempo en KMP; quien lo persista le dara el tipo que use su base.
 *
 * ## Por que el constructor es privado
 *
 * Una corrida es un registro historico: lo que dice al recuperarla debe ser lo que decia al
 * registrarla. Si [errors] guardara la misma lista que le paso quien la construyo, esa lista podria
 * seguir creciendo despues: una corrida registrada como `OK` con una `mutableListOf()` vacia
 * apareceria con errores al leerla del historial sin que `record` volviera a llamarse, y la
 * invariante «OK sin errores», que sólo se comprueba en `init`, quedaria burlada sin que nada lo
 * dijera. Por eso la unica entrada publica es [SyncRun.invoke] —misma sintaxis de llamada—, que
 * toma una instantanea de la lista antes de construir; y `copy` es privado
 * ([ConsistentCopyVisibility]) porque un `copy(errors = lista)` recibiria la lista tal cual y
 * abriria el mismo agujero por otra puerta. Para variar una corrida se construye otra.
 *
 * La deserializacion entra por el constructor privado con una lista recien creada por el
 * decodificador, que nadie mas referencia: tambien es una instantanea.
 */
@Serializable
@ConsistentCopyVisibility
data class SyncRun private constructor(
    val marketplaceId: MarketplaceId,
    val kind: SyncKind,
    val outcome: SyncOutcome,
    /** Fin de la corrida en epoch millis UTC. Lo pone quien la registra. */
    val finishedAt: Long,
    val durationMillis: Long = 0,
    /** Elementos examinados. */
    val examined: Int = 0,
    /** Elementos que la corrida cambio de verdad — no los que pensaba cambiar. */
    val changed: Int = 0,
    /**
     * Los errores **con su texto**, no un contador.
     *
     * Un consumidor imprimia `errors=1` y tiraba el mensaje; el fallo real —un campo obligatorio
     * ausente en un documento— tardo meses en diagnosticarse porque el motivo nunca se guardaba.
     *
     * Es una instantanea propia de la corrida: mutar la lista que se paso al construirla no la
     * cambia. Quien llame al constructor privado debe entregar una lista que nadie mas referencie.
     */
    val errors: List<SyncRunError> = emptyList(),
    /** Por que se omitio, cuando [outcome] es [SyncOutcome.SKIPPED]. Debe ser accionable. */
    val skippedReason: String? = null,
) {
    init {
        require(outcome != SyncOutcome.SKIPPED || !skippedReason.isNullOrBlank()) {
            "SKIPPED exige decir por que: una omisión sin motivo es el fallo silencioso otra vez"
        }
        require(outcome != SyncOutcome.OK || errors.isEmpty()) {
            "OK no admite errores; con errores y resultados útiles el desenlace es PARTIAL"
        }
    }

    companion object {
        /**
         * Unica forma publica de construir una corrida; se llama como un constructor. Toma una
         * instantanea de [errors]: la lista que guarda la corrida es suya, no la del llamador.
         */
        operator fun invoke(
            marketplaceId: MarketplaceId,
            kind: SyncKind,
            outcome: SyncOutcome,
            finishedAt: Long,
            durationMillis: Long = 0,
            examined: Int = 0,
            changed: Int = 0,
            errors: List<SyncRunError> = emptyList(),
            skippedReason: String? = null,
        ): SyncRun = SyncRun(
            marketplaceId = marketplaceId,
            kind = kind,
            outcome = outcome,
            finishedAt = finishedAt,
            durationMillis = durationMillis,
            examined = examined,
            changed = changed,
            errors = errors.toList(),
            skippedReason = skippedReason,
        )
    }
}
