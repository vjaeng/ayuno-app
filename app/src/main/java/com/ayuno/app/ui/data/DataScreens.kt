package com.ayuno.app.ui.data

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayuno.app.R
import com.ayuno.app.di.AppContainer
import com.ayuno.app.exchange.ExportFiles
import com.ayuno.app.ui.components.AyunoTextField
import com.ayuno.app.ui.components.CheckRow
import com.ayuno.app.ui.components.FieldError
import com.ayuno.app.ui.components.ListRow
import com.ayuno.app.ui.components.LoadingState
import com.ayuno.app.ui.components.PrimaryButton
import com.ayuno.app.ui.components.ScreenScaffold
import com.ayuno.app.ui.components.SecondaryButton
import com.ayuno.app.ui.components.SectionTitle
import com.ayuno.app.ui.components.SwitchRow
import com.ayuno.app.ui.components.message
import com.ayuno.app.ui.containerFactory
import com.ayuno.domain.model.ExportedFile
import com.ayuno.domain.model.ImportCheck
import com.ayuno.domain.model.ImportPreview
import com.ayuno.domain.model.ImportProblem
import com.ayuno.domain.model.ImportResult
import com.ayuno.domain.model.PersonProfile
import com.ayuno.domain.model.Settings
import com.ayuno.domain.usecase.ExportNames
import com.ayuno.domain.usecase.Outcome
import com.ayuno.domain.usecase.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Archivos listos para la hoja de compartir: el JSON y, si se pidió, las tablas (H-037). */
data class ExportReady(val file: ExportedFile, val tables: ByteArray?)

sealed interface ImportState {
    data object Idle : ImportState
    data object Working : ImportState
    data class Rejected(val problem: ImportProblem) : ImportState
    data class Preview(val name: String, val preview: ImportPreview, val content: ByteArray) : ImportState
    data class Done(val result: ImportResult) : ImportState
}

/** H-036 a H-040: exportar (un ayuno o todo) e importar con resumen previo. */
class DataViewModel(private val container: AppContainer) : ViewModel() {
    val hasFasts: StateFlow<Boolean?> = container.fasts.observeFasts().map { it.isNotEmpty() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val settings: StateFlow<Settings?> = container.settings.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _export = MutableStateFlow<ExportReady?>(null)
    val export: StateFlow<ExportReady?> = _export.asStateFlow()
    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()
    private val _import = MutableStateFlow<ImportState>(ImportState.Idle)
    val import: StateFlow<ImportState> = _import.asStateFlow()

    fun export(fastId: String?, withTables: Boolean) = viewModelScope.launch {
        when (val r = container.exportData(fastId)) {
            is Outcome.Ok -> { _errors.value = emptyList(); _export.value = ExportReady(r.value, if (withTables) container.exportTables(r.value.json) else null) }
            is Outcome.Invalid -> _errors.value = r.errors
        }
    }

    fun exported() { _export.value = null }

    fun fileName(fastId: String): StateFlow<String?> = container.fasts.observeFasts()
        .map { fasts -> fasts.firstOrNull { it.id == fastId }?.let(ExportNames::forFast) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** [content] nulo: el archivo pasa del límite y ni se ha leído entero. */
    fun preview(name: String, content: ByteArray?) = viewModelScope.launch {
        if (content == null) { _import.value = ImportState.Rejected(ImportProblem.TooLarge); return@launch }
        _import.value = ImportState.Working
        _import.value = when (val r = container.previewImport(content)) {
            is ImportCheck.Ok -> ImportState.Preview(name, r.value, content)
            is ImportCheck.Rejected -> ImportState.Rejected(r.problem)
        }
    }

    fun confirmImport() = viewModelScope.launch {
        val preview = _import.value as? ImportState.Preview ?: return@launch
        _import.value = ImportState.Working
        _import.value = when (val r = container.importData(preview.content)) {
            is ImportCheck.Ok -> { container.reminders.reschedule(); ImportState.Done(r.value) }
            is ImportCheck.Rejected -> ImportState.Rejected(r.problem)
        }
    }

    fun cancelImport() { _import.value = ImportState.Idle }
}

/** Escribe los archivos en la caché privada y abre la hoja de compartir; la app no envía nada por su cuenta. */
private fun share(context: Context, ready: ExportReady) {
    val uris = buildList {
        add(ExportFiles.write(context, ready.file.fileName, ready.file.json.toByteArray(Charsets.UTF_8)))
        ready.tables?.let { add(ExportFiles.write(context, ExportNames.tablesOf(ready.file.fileName), it)) }
    }
    context.startActivity(ExportFiles.shareIntent(uris, context.getString(R.string.export_share_title)))
}

@Composable
private fun ShareWhenReady(viewModel: DataViewModel) {
    val context = LocalContext.current
    val ready by viewModel.export.collectAsStateWithLifecycle()
    LaunchedEffect(ready) { ready?.let { share(context, it); viewModel.exported() } }
}

@Composable
fun DataScreen(onBack: () -> Unit, onPersonalData: () -> Unit) {
    val viewModel: DataViewModel = viewModel(factory = containerFactory { DataViewModel(it) })
    val hasFasts by viewModel.hasFasts.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val importState by viewModel.import.collectAsStateWithLifecycle()
    var withTables by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.preview(ExportFiles.displayName(context, it), ExportFiles.readLimited(context, it)) }
    }
    ShareWhenReady(viewModel)

    ScreenScaffold(stringResource(R.string.data_title), onBack) {
        if (importState !is ImportState.Idle) {
            ImportSection(importState, viewModel)
            return@ScreenScaffold
        }
        val any = hasFasts ?: run { LoadingState(); return@ScreenScaffold }
        SectionTitle(stringResource(R.string.data_export))
        PrimaryButton(stringResource(R.string.data_export_all), enabled = any, onClick = { viewModel.export(null, withTables) })
        if (!any) Text(stringResource(R.string.data_nothing_to_export), style = MaterialTheme.typography.bodyLarge)
        CheckRow(stringResource(R.string.data_with_tables), withTables) { withTables = it }
        ListRow(personLabel(settings), null, onClick = onPersonalData)
        errors.forEach { FieldError(stringResource(it.message())) }
        SectionTitle(stringResource(R.string.data_import))
        SecondaryButton(stringResource(R.string.data_import_file), onClick = { picker.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) })
        Text(stringResource(R.string.data_import_hint), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun personLabel(settings: Settings?): String =
    stringResource(if (settings?.exportPersonEnabled == true) R.string.data_person_included else R.string.data_person_not_included)

@Composable
private fun ImportSection(state: ImportState, viewModel: DataViewModel) {
    when (state) {
        ImportState.Idle -> Unit
        ImportState.Working -> LoadingState()
        is ImportState.Rejected -> {
            FieldError(problemText(state.problem))
            SecondaryButton(stringResource(R.string.action_back), onClick = viewModel::cancelImport)
        }
        is ImportState.Preview -> {
            Text(stringResource(R.string.import_title, state.name), style = MaterialTheme.typography.titleLarge)
            val c = state.preview.counts
            fun total(vararg keys: String) = keys.sumOf { c[it]?.total ?: 0 }
            fun new(vararg keys: String) = keys.sumOf { c[it]?.new ?: 0 }
            val records = arrayOf("measurements", "blood_pressure_readings", "symptoms", "intakes", "product_logs", "alerts", "notes", "refeed_checks", "meals")
            Text(stringResource(R.string.import_contains, total("fasts"), total("templates"), total("products"), total(*records)), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.import_new, new("fasts"), new("templates"), new("products"), new(*records)), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.import_existing), style = MaterialTheme.typography.bodyLarge)
            if (state.preview.ignoredFields.isNotEmpty()) {
                Text(stringResource(R.string.import_ignored, state.preview.ignoredFields.size, state.preview.ignoredFields.joinToString(", ")), style = MaterialTheme.typography.bodyLarge)
            }
            PrimaryButton(stringResource(R.string.import_confirm), onClick = viewModel::confirmImport)
            SecondaryButton(stringResource(R.string.action_cancel), onClick = viewModel::cancelImport)
        }
        is ImportState.Done -> {
            Text(stringResource(R.string.import_done, state.result.created, state.result.updated, state.result.keptLocal), style = MaterialTheme.typography.bodyLarge)
            if (state.result.ignoredFields.isNotEmpty()) {
                Text(stringResource(R.string.import_ignored, state.result.ignoredFields.size, state.result.ignoredFields.joinToString(", ")), style = MaterialTheme.typography.bodyLarge)
            }
            PrimaryButton(stringResource(R.string.action_back), onClick = viewModel::cancelImport)
        }
    }
}

@Composable
private fun problemText(problem: ImportProblem): String = when (problem) {
    ImportProblem.TooLarge -> stringResource(R.string.import_too_large)
    ImportProblem.NotOurFile -> stringResource(R.string.import_not_ours)
    ImportProblem.Empty -> stringResource(R.string.import_empty)
    is ImportProblem.NewerVersion -> stringResource(R.string.import_newer, problem.version)
    is ImportProblem.InvalidRow -> stringResource(R.string.import_invalid_row, problem.list, problem.index, problem.field)
}

/** H-036: exportar un ayuno desde su detalle. */
@Composable
fun ExportFastScreen(fastId: String, onBack: () -> Unit) {
    val viewModel: DataViewModel = viewModel(key = "export-$fastId", factory = containerFactory { DataViewModel(it) })
    val name by remember(fastId) { viewModel.fileName(fastId) }.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    var withTables by remember { mutableStateOf(false) }
    ShareWhenReady(viewModel)
    ScreenScaffold(stringResource(R.string.export_fast_title), onBack) {
        val fileName = name ?: run { LoadingState(); return@ScreenScaffold }
        Text(stringResource(R.string.export_file_name, fileName), style = MaterialTheme.typography.bodyLarge)
        CheckRow(stringResource(R.string.data_with_tables), withTables) { withTables = it }
        Text(personLabel(settings), style = MaterialTheme.typography.bodyLarge)
        errors.forEach { FieldError(stringResource(it.message())) }
        PrimaryButton(stringResource(R.string.export_action), onClick = { viewModel.export(fastId, withTables) })
    }
}

/** H-038: datos personales y si viajan en el archivo exportado. */
class PersonalDataViewModel(private val container: AppContainer) : ViewModel() {
    private val _loaded = MutableStateFlow<Pair<PersonProfile, Boolean>?>(null)
    val loaded: StateFlow<Pair<PersonProfile, Boolean>?> = _loaded.asStateFlow()

    init {
        viewModelScope.launch { _loaded.value = container.personProfile.get() to container.settings.get().exportPersonEnabled }
    }

    fun save(profile: PersonProfile, include: Boolean, onDone: () -> Unit) = viewModelScope.launch {
        if (container.savePersonProfile(profile, include) is Outcome.Ok) onDone()
    }
}

@Composable
fun PersonalDataScreen(onBack: () -> Unit) {
    val viewModel: PersonalDataViewModel = viewModel(factory = containerFactory { PersonalDataViewModel(it) })
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.person_title), onBack) {
        val (profile, included) = loaded ?: run { LoadingState(); return@ScreenScaffold }
        var include by remember { mutableStateOf(included) }
        var name by remember { mutableStateOf(profile.fullName.orEmpty()) }
        var idNumber by remember { mutableStateOf(profile.idNumber.orEmpty()) }
        var birth by remember { mutableStateOf(profile.birthDate?.toString().orEmpty()) }
        var birthError by remember { mutableStateOf(false) }
        Text(stringResource(R.string.person_hint), style = MaterialTheme.typography.bodyLarge)
        SwitchRow(stringResource(R.string.person_include), include, { include = it })
        AyunoTextField(stringResource(R.string.person_full_name), name, { name = it })
        AyunoTextField(stringResource(R.string.person_id_number), idNumber, { idNumber = it }, masked = true)
        AyunoTextField(stringResource(R.string.person_birth_date), birth, { birth = it }, error = stringResource(R.string.person_birth_date_error).takeIf { birthError })
        PrimaryButton(stringResource(R.string.action_save), onClick = {
            val date = birth.trim().takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            birthError = birth.isNotBlank() && date == null
            if (!birthError) viewModel.save(PersonProfile(name, idNumber, date), include, onBack)
        })
    }
}

