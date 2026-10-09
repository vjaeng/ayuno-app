package com.ayuno.app.ui.catalog

import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.CheckRow
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.OptionSelector
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.SwitchRow
import com.ayuno.app.ui.components.TimePickerDialog
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.app.ui.format.Formats
import com.ayuno.domain.model.IntakeType
import com.ayuno.domain.model.Product
import com.ayuno.domain.model.ProductKind
import com.ayuno.domain.model.SymptomType
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.SaveIntakeType
import com.ayuno.domain.usecase.SaveProduct
import com.ayuno.domain.usecase.SaveSymptomType
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class OtherCatalogsViewModel(private val container: AppContainer) : ViewModel() {
    val symptoms: StateFlow<List<SymptomType>?> = container.catalog.observeSymptomTypes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val intakes: StateFlow<List<IntakeType>?> = container.catalog.observeIntakeTypes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val products: StateFlow<List<Product>?> = container.catalog.observeProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    fun clearErrors() { _errors.value = emptyList() }
    fun setSymptomActive(code: String, active: Boolean) = viewModelScope.launch { container.setSymptomTypeActive(code, active) }
    fun setProductActive(code: String, active: Boolean) = viewModelScope.launch { container.setProductActive(code, active); container.reminders.reschedule() }

    fun <T> save(block: suspend () -> Outcome<T>, onDone: () -> Unit) = viewModelScope.launch {
        when (val r = block()) {
            is Outcome.Ok -> { _errors.value = emptyList(); container.reminders.reschedule(); onDone() }
            is Outcome.Invalid -> _errors.value = r.errors
        }
    }

    val container_ get() = container
}

@Composable
private fun otherCatalogs(): OtherCatalogsViewModel = viewModel(factory = containerFactory { OtherCatalogsViewModel(it) })

/** Más → Catálogos (UX-UI ajustes-catalogos). */
@Composable
fun CatalogsScreen(onBack: () -> Unit, onMetrics: () -> Unit, onSymptoms: () -> Unit, onIntakes: () -> Unit, onProducts: () -> Unit) {
    ScreenScaffold(stringResource(R.string.catalogs_title), onBack) {
        ListRow(stringResource(R.string.catalog_metrics_title), null, onMetrics)
        ListRow(stringResource(R.string.catalog_symptoms_title), null, onSymptoms)
        ListRow(stringResource(R.string.catalog_intakes_title), null, onIntakes)
        ListRow(stringResource(R.string.catalog_products_title), null, onProducts)
    }
}

/** H-010 */
@Composable
fun SymptomCatalogScreen(onBack: () -> Unit, onEdit: (String?) -> Unit) {
    val vm = otherCatalogs()
    val list by vm.symptoms.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.catalog_symptoms_title), onBack) {
        val items = list ?: run { LoadingState(); return@ScreenScaffold }
        items.forEach { t ->
            ListRow((if (t.isAlarm) "⚠ " else "") + t.name, t.groupName, { onEdit(t.code) }) { Switch(t.active, { vm.setSymptomActive(t.code, it) }) }
        }
        SecondaryButton(stringResource(R.string.catalog_add_symptom), onClick = { onEdit(null) })
    }
}

@Composable
fun SymptomEditorScreen(code: String?, onBack: () -> Unit) {
    val vm = otherCatalogs()
    val list by vm.symptoms.collectAsStateWithLifecycle()
    val errors by vm.errors.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.clearErrors() }
    val existing = code?.let { c -> list?.firstOrNull { it.code == c } }
    if (code != null && existing == null) { LoadingState(); return }
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var group by remember(existing) { mutableStateOf(existing?.groupName.orEmpty()) }
    var alarm by remember(existing) { mutableStateOf(existing?.isAlarm ?: false) }
    ScreenScaffold(stringResource(R.string.catalog_symptom), onBack) {
        AyunoTextField(stringResource(R.string.field_name), name, { name = it })
        AyunoTextField(stringResource(R.string.field_group), group, { group = it })
        SwitchRow(stringResource(R.string.field_is_alarm), alarm, { alarm = it }, stringResource(R.string.field_is_alarm_hint))
        errors.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_save), onClick = {
            vm.save({ vm.container_.saveSymptomType(SaveSymptomType.Input(code, name, group, alarm)) }, onBack)
        })
    }
}

/** H-011 */
@Composable
fun IntakeCatalogScreen(onBack: () -> Unit, onEdit: (String?) -> Unit) {
    val vm = otherCatalogs()
    val list by vm.intakes.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.catalog_intakes_title), onBack) {
        val items = list ?: run { LoadingState(); return@ScreenScaffold }
        items.forEach { t ->
            val supporting = listOfNotNull(t.unit, stringResource(R.string.intake_breaks_fast).takeIf { t.breaksFast }).joinToString(" · ")
            ListRow(t.name, supporting, { onEdit(t.code) })
        }
        SecondaryButton(stringResource(R.string.catalog_add_intake), onClick = { onEdit(null) })
    }
}

@Composable
fun IntakeEditorScreen(code: String?, onBack: () -> Unit) {
    val vm = otherCatalogs()
    val list by vm.intakes.collectAsStateWithLifecycle()
    val errors by vm.errors.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.clearErrors() }
    val existing = code?.let { c -> list?.firstOrNull { it.code == c } }
    if (code != null && existing == null) { LoadingState(); return }
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var unit by remember(existing) { mutableStateOf(existing?.unit.orEmpty()) }
    var breaks by remember(existing) { mutableStateOf(existing?.breaksFast ?: false) }
    var factor by remember(existing) { mutableStateOf(existing?.sodiumFactor?.let { Formats.number(it, 3) }.orEmpty()) }
    ScreenScaffold(stringResource(R.string.catalog_intake), onBack) {
        AyunoTextField(stringResource(R.string.field_name), name, { name = it })
        AyunoTextField(stringResource(R.string.field_unit), unit, { unit = it })
        SwitchRow(stringResource(R.string.intake_breaks_fast), breaks, { breaks = it })
        AyunoTextField(stringResource(R.string.field_sodium_factor), factor, { factor = it }, numeric = true)
        errors.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_save), onClick = {
            vm.save({ vm.container_.saveIntakeType(SaveIntakeType.Input(code, name, unit, breaks, Formats.parseNumber(factor))) }, onBack)
        })
    }
}

/** H-013 */
@Composable
fun ProductCatalogScreen(onBack: () -> Unit, onEdit: (String?) -> Unit) {
    val vm = otherCatalogs()
    val list by vm.products.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.catalog_products_title), onBack) {
        val items = list ?: run { LoadingState(); return@ScreenScaffold }
        if (items.isEmpty()) Text(stringResource(R.string.product_empty), style = MaterialTheme.typography.bodyLarge)
        items.forEach { p ->
            val supporting = listOfNotNull(
                "${Formats.number(p.defaultAmount)} ${p.unit}",
                p.schedules.joinToString(", ") { Formats.time(it.timeOfDay) }.ifEmpty { null },
                stringResource(R.string.product_avoid).takeIf { p.avoid },
            ).joinToString(" · ")
            ListRow(p.name, supporting, { onEdit(p.code) }) { Switch(p.active, { vm.setProductActive(p.code, it) }) }
        }
        SecondaryButton(stringResource(R.string.catalog_add_product), onClick = { onEdit(null) })
    }
}

@Composable
fun ProductEditorScreen(code: String?, onBack: () -> Unit) {
    val vm = otherCatalogs()
    val list by vm.products.collectAsStateWithLifecycle()
    val errors by vm.errors.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.clearErrors() }
    val existing = code?.let { c -> list?.firstOrNull { it.code == c } }
    if (code != null && existing == null) { LoadingState(); return }
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var productCode by remember(existing) { mutableStateOf(existing?.code.orEmpty()) }
    var kind by remember(existing) { mutableStateOf(existing?.kind ?: ProductKind.SUPPLEMENT) }
    var amount by remember(existing) { mutableStateOf(existing?.defaultAmount?.let { Formats.number(it, 2) }.orEmpty()) }
    var unit by remember(existing) { mutableStateOf(existing?.unit.orEmpty()) }
    var avoid by remember(existing) { mutableStateOf(existing?.avoid ?: false) }
    val schedules = remember(existing) { mutableStateListOf<LocalTime>().apply { addAll(existing?.schedules?.map { it.timeOfDay }.orEmpty()) } }
    var addingTime by remember { mutableStateOf(false) }

    ScreenScaffold(stringResource(R.string.catalog_product), onBack) {
        AyunoTextField(stringResource(R.string.field_name), name, { name = it })
        if (existing == null) {
            AyunoTextField(stringResource(R.string.field_code), productCode, { productCode = it })
            Text(stringResource(R.string.product_code_hint), style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(stringResource(R.string.product_code_fixed, existing.code), style = MaterialTheme.typography.bodyLarge)
        }
        OptionSelector(listOf(ProductKind.SUPPLEMENT to stringResource(R.string.kind_supplement), ProductKind.MEDICATION to stringResource(R.string.kind_medication)), kind, { kind = it })
        AyunoTextField(stringResource(R.string.field_dose), amount, { amount = it }, numeric = true)
        AyunoTextField(stringResource(R.string.field_unit), unit, { unit = it })
        SectionTitle(stringResource(R.string.product_schedules))
        schedules.sorted().forEach { t ->
            ListRow(Formats.time(t), null, onClick = {}) { TextButton(onClick = { schedules.remove(t) }) { Text(stringResource(R.string.action_remove)) } }
        }
        TextButton(onClick = { addingTime = true }) { Text(stringResource(R.string.template_add_time)) }
        CheckRow(stringResource(R.string.product_avoid), avoid) { avoid = it }
        errors.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.action_save), onClick = {
            val dose = Formats.parseNumber(amount)
            vm.save({
                vm.container_.saveProduct(
                    SaveProduct.Input(code, productCode.takeIf { existing == null }, name, kind, dose, unit, avoid, schedules.map { it to (dose ?: 0.0) }),
                )
            }, onBack)
        })
    }
    if (addingTime) TimePickerDialog(LocalTime.of(8, 0), onDismiss = { addingTime = false }, onConfirm = { if (it !in schedules) schedules += it; addingTime = false })
}
