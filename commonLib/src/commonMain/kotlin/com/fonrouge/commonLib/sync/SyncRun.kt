package com.fonrouge.commonLib.sync

import com.fonrouge.commonLib.model.MarketplaceId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
 * guarda una copia de sólo lectura de la lista antes de construir; y `copy` es privado
 * ([ConsistentCopyVisibility]) porque un `copy(errors = lista)` recibiria la lista tal cual y
 * abriria el mismo agujero por otra puerta. Para variar una corrida se construye otra.
 *
 * La copia es de sólo lectura **de verdad**, no un `toList()`: en JVM `List` y `MutableList` son la
 * misma clase de Java, asi que un `ArrayList` copiado seguiria siendo vaciable con un cast desde la
 * propia propiedad [errors]. La lista guardada implementa `List` y nada mas; las operaciones
 * mutadoras fallan en JVM y en JS. La deserializacion pasa por el mismo envoltorio, para que una
 * corrida leida de la base no sea mutable por la via que se cerro para las construidas.
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
     * Es una instantanea de sólo lectura propia de la corrida: ni mutar la lista que se paso al
     * construirla ni hacer un cast de esta propiedad la cambian. Quien llame al constructor privado
     * debe entregar una lista de sólo lectura que nadie mas referencie.
     */
    @Serializable(with = SnapshotErrorsSerializer::class)
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
         * Unica forma publica de construir una corrida; se llama como un constructor. Guarda una
         * copia de sólo lectura de [errors]: la lista de la corrida es suya, no la del llamador, y
         * no se puede mutar.
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
            errors = SnapshotList(errors),
            skippedReason = skippedReason,
        )
    }
}

/**
 * Lista de sólo lectura **de verdad**: implementa `List` y nada mas, y copia la entrada.
 *
 * `toList()` no basta. En JVM devuelve un `ArrayList` a partir de dos elementos, y `List` y
 * `MutableList` son la misma clase de Java, asi que `(errors as MutableList).clear()` vaciaria la
 * corrida —y el historial, que devuelve el mismo objeto— sin pasar por ningun constructor. Con esta
 * clase, en JVM las operaciones mutadoras heredadas de `java.util.List` lanzan
 * `UnsupportedOperationException` (el compilador las genera asi para las colecciones de sólo
 * lectura) y en JS el cast a `MutableList` falla.
 */
private class SnapshotList<T>(source: List<T>) : AbstractList<T>() {
    private val items: List<T> = source.toList()
    override val size: Int get() = items.size
    override fun get(index: Int): T = items[index]
}

/**
 * Serializa como una lista normal y **deserializa a [SnapshotList]**: sin esto el decodificador
 * entregaria un `ArrayList` al constructor privado, y una corrida leida de la base seria mutable
 * por la misma via que se cerro para las construidas.
 */
private object SnapshotErrorsSerializer : KSerializer<List<SyncRunError>> {
    private val delegate = ListSerializer(SyncRunError.serializer())
    override val descriptor: SerialDescriptor get() = delegate.descriptor
    override fun serialize(encoder: Encoder, value: List<SyncRunError>) = delegate.serialize(encoder, value)
    override fun deserialize(decoder: Decoder): List<SyncRunError> = SnapshotList(delegate.deserialize(decoder))
}
