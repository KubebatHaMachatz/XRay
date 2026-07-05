package com.demo.xray

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * PRD principle 1 / FR-8 / Phase 0 exit criterion: the release manifest must never request
 * `android.permission.INTERNET`. This checks the source manifest directly (Gradle's test working
 * directory is the module root), since no dependency in this project declares INTERNET either -
 * see PRD 15.1 and ADR-0001.
 */
class ManifestNoInternetPermissionTest {
    @Test
    fun `source manifest does not request INTERNET permission`() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("expected to find ${manifestFile.absolutePath}", manifestFile.exists())

        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile)
        val usesPermissionNodes = document.getElementsByTagName("uses-permission")

        val requestedPermissions =
            (0 until usesPermissionNodes.length).map { i ->
                usesPermissionNodes.item(i).attributes.getNamedItemNS("http://schemas.android.com/apk/res/android", "name")?.nodeValue
            }

        assertTrue(
            "manifest must not request android.permission.INTERNET, found: $requestedPermissions",
            requestedPermissions.none { it == "android.permission.INTERNET" },
        )
    }
}
