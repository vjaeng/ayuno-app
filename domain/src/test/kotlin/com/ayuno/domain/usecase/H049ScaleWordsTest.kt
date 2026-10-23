package com.ayuno.domain.usecase

import com.ayuno.domain.fakes.TestWorld
import com.ayuno.domain.model.EndReason
import com.ayuno.domain.model.Measurement
import com.ayuno.domain.model.MetricType
import com.ayuno.domain.model.RuleConditionKind
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/** EV-005 · H-049 (D-21) · TRX guardar-entrada y corregir-entrada con palabra. */
class H049ScaleWordsTest {

    private val at = LocalDateTime.of(2026, 10, 23, 12, 1, 40)

    private val words = listOf("Nada" to 0.0, "Poca" to 3.0, "Moderada" to 5.0, "Mucha" to 7.0, "Intensa" to 10.0)

    private fun drafts(list: List<Pair<String, Double>> = words) = list.map { (label, value) -> SaveMetricType.OptionDraft(null, label, value) }

    private suspend fun TestWorld.saveHunger(options: List<SaveMetricType.OptionDraft>?, min: Double? = 0.0, max: Double? = 10.0) =
        saveMetricType(SaveMetricType.Input("hunger", "Hambre", "0-10", true, min, max, options))

    private suspend fun TestWorld.hungerWithWords(): MetricType = ((saveHunger(drafts()) as Outcome.Ok).value)

    private suspend fun TestWorld.choose(fastId: String, type: MetricType, label: String): Outcome<RecordResult<Measurement>> =
        recordMeasurement(RecordMeasurement.Input(fastId, "hunger", at, metricOptionId = type.options.first { it.label == label }.id))

    // ---------- Palabras del catálogo ----------

    @Test
    fun `H-049 las palabras se guardan en orden con su numero y se leen por el numero guardado`() = runTest {
        val w = TestWorld()
        val hunger = w.hungerWithWords()
        assertEquals(words, hunger.options.map { it.label to it.value })
        assertEquals(listOf(0, 1, 2, 3, 4), hunger.options.map { it.position })
        assertEquals("Mucha", hunger.optionFor(7.0)?.label)
        assertNull(hunger.optionFor(4.0))
        assertNull(hunger.optionFor(null))
    }

    @Test
    fun `H-049 editar el resto de la medicion conserva sus palabras`() = runTest {
        val w = TestWorld()
        val hunger = w.hungerWithWords()
        w.saveHunger(options = null)
        w.setMetricTypeActive("hunger", false)
        assertEquals(hunger.options, w.catalog.metricType("hunger")!!.options)
    }

    @Test
    fun `H-049 textos o numeros repetidos, vacios, fuera de rango o demasiadas palabras no se guardan`() = runTest {
        val w = TestWorld()
        fun errors(o: Outcome<MetricType>) = (o as Outcome.Invalid).errors
        assertEquals(listOf(ValidationError.OPTION_LABEL_DUPLICATED), errors(w.saveHunger(drafts(listOf("Poca" to 3.0, " poca " to 4.0)))))
        assertEquals(listOf(ValidationError.OPTION_VALUE_DUPLICATED), errors(w.saveHunger(drafts(listOf("Poca" to 3.0, "Algo" to 3.0)))))
        assertEquals(listOf(ValidationError.OPTION_LABEL_REQUIRED), errors(w.saveHunger(drafts(listOf("  " to 3.0)))))
        assertEquals(listOf(ValidationError.OPTION_VALUE_OUT_OF_RANGE), errors(w.saveHunger(drafts(listOf("Feroz" to 11.0)))))
        assertEquals(listOf(ValidationError.OPTION_VALUE_REQUIRED), errors(w.saveHunger(drafts(listOf("Poca" to Double.NaN)))))
        assertEquals(listOf(ValidationError.OPTIONS_TOO_MANY), errors(w.saveHunger(drafts((0..9).map { "P$it" to it.toDouble() }))))
        assertTrue(w.catalog.metricType("hunger")!!.options.isEmpty())
    }

    @Test
    fun `H-049 cambiar el rango dejando palabras fuera no se permite`() = runTest {
        val w = TestWorld()
        w.hungerWithWords()
        assertEquals(listOf(ValidationError.OPTION_VALUE_OUT_OF_RANGE), (w.saveHunger(options = null, min = 1.0, max = 10.0) as Outcome.Invalid).errors)
        assertEquals(0.0, w.catalog.metricType("hunger")!!.minValid)
    }

    @Test
    fun `H-049 una medicion propia no numerica no admite palabras`() = runTest {
        val w = TestWorld()
        val result = w.saveMetricType(SaveMetricType.Input(null, "Estado", "texto", false, null, null, drafts(listOf("Bien" to 1.0))))
        assertEquals(listOf(ValidationError.OPTIONS_NOT_NUMERIC), (result as Outcome.Invalid).errors)
    }

    @Test
    fun `H-049 quitar y renombrar palabras no altera lo ya anotado`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val hunger = w.hungerWithWords()
        w.choose(f.id, hunger, "Mucha")
        val kept = hunger.options.filterNot { it.label == "Mucha" }.map { SaveMetricType.OptionDraft(it.id, if (it.label == "Poca") "Algo" else it.label, it.value) }
        w.saveHunger(kept)
        val after = w.catalog.metricType("hunger")!!
        assertEquals(listOf("Nada", "Algo", "Moderada", "Intensa"), after.options.map { it.label })
        assertEquals(hunger.options.first { it.label == "Poca" }.id, after.options[1].id)
        val m = w.records.measurements.value.single()
        assertEquals(7.0, m.valueNum)
        assertNull(after.optionFor(m.valueNum))
    }

    // ---------- Anotar y corregir con una palabra ----------

    @Test
    fun `H-049 elegir una palabra guarda su numero en la unidad del formulario`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val hunger = w.hungerWithWords()
        val saved = ((w.choose(f.id, hunger, "Mucha") as Outcome.Ok).value as RecordResult.Saved).entry
        assertEquals(7.0, saved.valueEntered)
        assertEquals(7.0, saved.valueNum)
        assertEquals("0-10", saved.unitEntered)
        assertEquals("Mucha", hunger.optionFor(saved.valueNum)?.label)
    }

    @Test
    fun `H-049 una regla sobre la escala salta al elegir la palabra cuyo numero la cumple`() = runTest {
        val w = TestWorld()
        val rule = w.glucoseBelow(6.0).copy(name = "Hambre alta", metricTypeCode = "hunger", conditionKind = RuleConditionKind.METRIC_ABOVE)
        val f = w.startedFast(w.plan57h(listOf(rule)))
        val hunger = w.hungerWithWords()
        assertTrue(((w.choose(f.id, hunger, "Moderada") as Outcome.Ok).value as RecordResult.Saved).alerts.isEmpty())
        val fired = ((w.choose(f.id, hunger, "Mucha") as Outcome.Ok).value as RecordResult.Saved).alerts
        assertEquals(listOf("Hambre alta"), fired.map { a -> w.fasts.get(f.id)!!.plan.rules.first { it.id == a.ruleId }.name })
    }

    @Test
    fun `H-049 una palabra que ya no existe no se guarda`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        w.hungerWithWords()
        val result = w.recordMeasurement(RecordMeasurement.Input(f.id, "hunger", at, metricOptionId = "quitada"))
        assertEquals(listOf(ValidationError.OPTION_NOT_FOUND), (result as Outcome.Invalid).errors)
        // Una palabra de otra medición tampoco vale.
        val glucoseOption = w.hungerWithWords().options.first().id
        val other = w.recordMeasurement(RecordMeasurement.Input(f.id, "glucose", at, metricOptionId = glucoseOption))
        assertEquals(listOf(ValidationError.OPTION_NOT_FOUND), (other as Outcome.Invalid).errors)
        assertTrue(w.records.measurements.value.isEmpty())
    }

    @Test
    fun `H-049 corregir con una palabra deja en la bitacora los numeros anterior y nuevo`() = runTest {
        val w = TestWorld(); val f = w.startedFast()
        val hunger = w.hungerWithWords()
        val m = ((w.choose(f.id, hunger, "Poca") as Outcome.Ok).value as RecordResult.Saved).entry
        w.endFast(EndFast.Input(f.id, at.plusHours(1), EndReason.GOAL_MET, null, null))
        val id = hunger.options.first { it.label == "Mucha" }.id
        w.correctMeasurement(CorrectMeasurement.Input(m.id, m.takenAt, metricOptionId = id, reason = "me equivoqué"))
        val after = w.records.measurements.value.single { it.id == m.id }
        assertEquals(7.0, after.valueNum)
        val log = w.audit.entriesFor(m.id).associate { it.field to (it.oldValue to it.newValue) }
        assertEquals(mapOf("value_entered" to ("3" to "7"), "value_num" to ("3" to "7")), log)
        val missing = w.correctMeasurement(CorrectMeasurement.Input(m.id, m.takenAt, metricOptionId = "quitada"))
        assertEquals(listOf(ValidationError.OPTION_NOT_FOUND), (missing as Outcome.Invalid).errors)
    }
}
