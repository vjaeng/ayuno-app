package com.ayuno.app.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayuno.app.MainActivity
import com.ayuno.app.reminders.ReminderReceiver
import com.ayuno.domain.id.Ids
import com.ayuno.domain.model.ChecklistItem
import com.ayuno.domain.model.DuringFast
import com.ayuno.domain.model.IntakeGoal
import com.ayuno.domain.model.PlanProduct
import com.ayuno.domain.model.ProductKind
import com.ayuno.domain.model.ProductLogStatus
import com.ayuno.domain.model.RuleConditionKind
import com.ayuno.domain.model.StageKind
import com.ayuno.domain.model.StageNotice
import com.ayuno.domain.model.UserChoice
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.SaveProduct
import com.ayuno.domain.usecase.SaveSymptomType
import com.ayuno.domain.usecase.SaveTemplate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime

/** H-002, H-006, H-010, H-011, H-012, H-013, H-014, H-015, H-021, H-022, H-023, H-035 desde la interfaz. */
@RunWith(AndroidJUnit4::class)
class Batch2aE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** Añade al plan de la plantilla sembrada lo que haga falta, con el caso de uso real. */
    private fun templateWith(
        metrics: List<String> = listOf("glucose", "blood_pressure"),
        transform: (com.ayuno.domain.model.Plan) -> com.ayuno.domain.model.Plan,
    ): com.ayuno.domain.model.TemplateWithPlan {
        val base = seedTemplate(metrics = metrics)
        return runBlocking { (app.container.saveTemplate(base.copy(plan = transform(base.plan))) as Outcome.Ok).value }
    }

    @Test
    fun `H010 H021 un sintoma de alarma dispara su regla y uno esperable no`() {
        runBlocking { app.container.saveSymptomType(SaveSymptomType.Input(null, "Visión borrosa", "Alarma", true)) }
        val rule = glucoseBelow55For20Min().copy(conditionKind = RuleConditionKind.ALARM_SYMPTOM, metricTypeCode = null, threshold = null, sustainedMinutes = null, message = "Toma 15 g de azúcar ya.")
        val id = seedStartedFast(templateWith { it.copy(rules = listOf(rule)) })
        compose.activityRule.scenario.recreate()
        compose.tap("Anotar"); compose.tap("Síntoma"); compose.tap("Dolor de cabeza"); compose.tap("Guardar")
        compose.eventually { assertEquals(1, runBlocking { app.container.records.symptoms(id) }.size) }
        assertTrue(runBlocking { app.container.alerts.alerts(id) }.isEmpty())
        compose.tap("Anotar"); compose.tap("Síntoma"); compose.tap("⚠ Visión borrosa"); compose.tap("Guardar")
        compose.assertShown("Toma 15 g de azúcar ya.")
        compose.assertShown("Si marcas un síntoma de alarma: alerta roja y ofrecer terminar el ayuno.")
    }

    @Test
    fun `H012 H006 H002 sal con sodio calculado, total frente a la meta en Inicio y aviso de la etapa`() {
        val id = seedStartedFast(templateWith {
            it.copy(
                goals = listOf(IntakeGoal(Ids.newId(), "salt", 4.0, "g")),
                notices = listOf(StageNotice(Ids.newId(), "No sacarse sangre durante el ayuno.", listOf(StageKind.FAST))),
            )
        })
        app.clock.advanceMinutes(10 * 60) // viernes 08:10
        compose.activityRule.scenario.recreate()
        compose.assertShown("No sacarse sangre durante el ayuno.")
        compose.tap("Anotar"); compose.tap("Líquido"); compose.tap("Sal (g)")
        compose.type("Cantidad", "5")
        compose.assertShown("Sodio: 1,97 g")
        compose.tap("Guardar")
        compose.assertShown("Sal: llevas 5 de 4 g · sodio 1,97 g")
        assertEquals(5.0, runBlocking { app.container.records.intakes(id) }.single().amount, 0.0)
    }

    @Test
    fun `H012 anotar caldo durante el ayuno pregunta si se esta terminando y lleva al fin con esa hora`() {
        seedStartedFast(seedTemplate())
        app.clock.advanceMinutes(60)
        compose.activityRule.scenario.recreate()
        compose.tap("Anotar"); compose.tap("Líquido"); compose.tap("Caldo (mL)")
        compose.type("Cantidad", "250")
        compose.tap("Guardar")
        compose.assertShown("Anotaste Caldo. ¿Estás terminando el ayuno?")
        compose.tap("Sí, terminar")
        compose.assertShown("¿Por qué terminas?")
        compose.assertShown("jueves 22 de octubre, 23:10:00", substring = true)
    }

    @Test
    fun `H012 si se responde que no, la ingesta queda y el ayuno sigue`() {
        val id = seedStartedFast(seedTemplate())
        compose.activityRule.scenario.recreate()
        compose.tap("Anotar"); compose.tap("Líquido"); compose.tap("Caldo (mL)"); compose.type("Cantidad", "250"); compose.tap("Guardar")
        compose.tap("No, sigo")
        compose.assertShown("Anotar")
        assertEquals(1, runBlocking { app.container.records.intakes(id) }.size)
    }

    @Test
    fun `H013 H014 H015 la toma pendiente aparece en Inicio y se marca tomada y los que salen se avisan`() {
        runBlocking {
            app.container.saveProduct(SaveProduct.Input(null, null, "Vitamina C", ProductKind.SUPPLEMENT, 1.0, "g", false, listOf(LocalTime.of(8, 30) to 1.0)))
            app.container.saveProduct(SaveProduct.Input(null, null, "Omega 3", ProductKind.SUPPLEMENT, 2.0, "cápsulas", false, listOf(LocalTime.of(8, 30) to 2.0)))
        }
        val id = seedStartedFast(templateWith(metrics = emptyList()) {
            it.copy(products = listOf(
                PlanProduct(Ids.newId(), "vitamina_c", DuringFast.CONTINUES, false, null),
                PlanProduct(Ids.newId(), "omega_3", DuringFast.STOPS, false, "Aceite"),
            ))
        })
        app.clock.now = Instant.parse("2026-10-23T13:25:00Z") // viernes 08:25
        compose.activityRule.scenario.recreate()
        compose.assertShown("Vitamina C · 1 g")
        compose.tap("Anotar"); compose.tap("Producto")
        compose.assertShown("Este producto no se toma durante el ayuno.")
        compose.tap("Vitamina C"); compose.tap("Guardar")
        compose.eventually {
            val log = runBlocking { app.container.records.productLogs(id) }.single()
            assertEquals(ProductLogStatus.TAKEN, log.status)
            assertEquals(LocalDateTime.of(2026, 10, 23, 8, 30), log.scheduledAt)
        }
    }

    @Test
    fun `H013 un producto a evitar avisa antes de anotarlo`() {
        runBlocking { app.container.saveProduct(SaveProduct.Input(null, null, "Ibuprofeno", ProductKind.MEDICATION, 400.0, "mg", true, emptyList())) }
        seedStartedFast(seedTemplate())
        compose.activityRule.scenario.recreate()
        compose.tap("Anotar"); compose.tap("Producto"); compose.tap("Ibuprofeno")
        compose.assertShown("Marcaste Ibuprofeno como a evitar. Puedes anotarlo igual.")
        compose.tap("Anotar igual")
        compose.assertShown("Dosis prevista: 400 mg")
    }

    @Test
    fun `H023 algo no va bien sin regla definida muestra la alerta generica y queda registrada`() {
        val id = seedStartedFast(seedTemplate())
        compose.activityRule.scenario.recreate()
        compose.tap("Seguridad"); compose.tap("Algo no va bien")
        compose.assertShown("Detente y valora si terminar el ayuno.")
        compose.type("Si sigues, escribe por qué", "Era el hambre")
        compose.tap("Sigo")
        compose.eventually { assertEquals(UserChoice.CONTINUE, runBlocking { app.container.alerts.alerts(id) }.single().userChoice) }
    }

    @Test
    fun `H022 a la hora indicada, con el sodio bajo la meta, salta el aviso`() {
        val rule = glucoseBelow55For20Min().copy(
            conditionKind = RuleConditionKind.INTAKE_BELOW_GOAL, metricTypeCode = null, threshold = null, sustainedMinutes = null,
            intakeTypeCode = "salt", checkTime = LocalTime.of(14, 0), level = com.ayuno.domain.model.RuleLevel.NOTICE, message = "Toma sal en agua.",
        )
        val id = seedStartedFast(templateWith { it.copy(rules = listOf(rule), goals = listOf(IntakeGoal(Ids.newId(), "salt", 4.0, "g"))) })
        app.clock.now = Instant.parse("2026-10-23T19:00:00Z") // viernes 14:00
        runBlocking { ReminderReceiver.handle(app, LocalDateTime.of(2026, 10, 23, 14, 0)) }
        assertEquals(1, runBlocking { app.container.alerts.alerts(id) }.size)
        compose.activityRule.scenario.recreate()
        compose.assertShown("Toma sal en agua.")
    }

    @Test
    fun `H035 nota libre con hora`() {
        val id = seedStartedFast(seedTemplate())
        compose.activityRule.scenario.recreate()
        compose.tap("Anotar"); compose.tap("Nota"); compose.type("Texto", "Leve temblor"); compose.tap("Guardar")
        compose.assertShown("Anotar")
        compose.eventually { assertEquals("Leve temblor", runBlocking { app.container.records.observeNotes(id).first() }.single().text) }
    }

    @Test
    fun `H002 la lista previa se marca en Inicio y no impide empezar`() {
        seedPlannedFast(templateWith { it.copy(checklist = listOf(ChecklistItem(Ids.newId(), "Comprar sal", false), ChecklistItem(Ids.newId(), "Cargar el tensiómetro", false))) })
        compose.activityRule.scenario.recreate()
        compose.assertShown("Lista previa: marcados 0 de 2")
        compose.tap("Comprar sal")
        compose.assertShown("Lista previa: marcados 1 de 2")
        compose.tap("Empieza el ayuno ahora")
        compose.assertShown("Seguridad")
    }

    @Test
    fun `H011 crear un tipo de ingesta desde el catalogo`() {
        compose.tap("Más"); compose.tap("Catálogos"); compose.tap("Líquidos y electrolitos"); compose.tap("Añadir un tipo")
        compose.type("Nombre", "Infusión de menta"); compose.type("Unidad", "mL"); compose.tap("Guardar")
        compose.assertShown("Añadir un tipo") // de vuelta en la lista
        compose.assertShown("Infusión de menta")
        compose.eventually { assertTrue(runBlocking { app.container.catalog.intakeTypes() }.any { it.code == "infusion_de_menta" }) }
    }

    @Test
    fun `H013 crear un producto desde el catalogo con codigo generado`() {
        compose.tap("Más"); compose.tap("Catálogos"); compose.tap("Suplementos y medicamentos"); compose.tap("Añadir un producto")
        compose.type("Nombre", "Magnesio"); compose.type("Dosis habitual", "400"); compose.type("Unidad", "mg"); compose.tap("Guardar")
        compose.assertShown("Magnesio")
        compose.eventually { assertEquals("mg", runBlocking { app.container.catalog.product("magnesio") }!!.unit) }
    }
}
