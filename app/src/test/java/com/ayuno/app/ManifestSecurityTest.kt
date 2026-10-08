package com.ayuno.app

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** ADR-SEG-004 (sin red) y ADR-SEG-001 (sin copia de seguridad del sistema), sobre el manifiesto fusionado. */
@RunWith(AndroidJUnit4::class)
class ManifestSecurityTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test
    fun `el manifiesto final no pide el permiso INTERNET`() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions.orEmpty().toList()
        assertFalse("Permisos pedidos: $requested", Manifest.permission.INTERNET in requested)
    }

    @Test
    fun `la copia de seguridad del sistema esta desactivada`() {
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }
}
