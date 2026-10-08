package com.ayuno.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** `:app → :data` solo desde la raíz de composición `di/` (specs/ARQUITECTURA.md §1). */
class ArchitectureTest {

    @Test
    fun `solo el paquete di importa el modulo data`() {
        val sources = File("src/main/java/com/ayuno/app")
        assertTrue("No se encontraron fuentes en ${sources.absolutePath}", sources.isDirectory)
        val violations = sources.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.relativeTo(sources).path.startsWith("di") }
            .filter { file -> file.readLines().any { it.trim().startsWith("import com.ayuno.data") } }
            .map { it.relativeTo(sources).path }
            .toList()
        assertTrue("Importan com.ayuno.data fuera de di/: $violations", violations.isEmpty())
    }
}
