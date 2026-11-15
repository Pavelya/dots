package io.github.pavelya.dots.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Android's backup takes the campaign progress and nothing else: never the saved games or the test
 * log (A-04). Read from the files the app is built with; unit tests run in the app's folder.
 */
class BackupRulesTest {
    private val progressFile = "datastore/${ProgressRepository.NAME}.preferences_pb"

    private fun xml(path: String): Element =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path)).documentElement

    /** Each rule under [section] as "include file datastore/…". */
    private fun rules(section: Element): List<String> {
        val nodes = section.childNodes
        return (0 until nodes.length).map { nodes.item(it) }.filterIsInstance<Element>()
            .map { "${it.tagName} ${it.getAttribute("domain")} ${it.getAttribute("path")}" }
    }

    @Test
    fun `backup and device transfer take only the progress file`() {
        val root = xml("src/main/res/xml/data_extraction_rules.xml")
        for (name in listOf("cloud-backup", "device-transfer")) {
            val section = root.getElementsByTagName(name).item(0) as Element
            assertEquals(name, listOf("include file $progressFile"), rules(section))
        }
    }

    @Test
    fun `Android 11 and older back up only the progress file`() {
        assertEquals(listOf("include file $progressFile"), rules(xml("src/main/res/xml/backup_rules.xml")))
    }

    @Test
    fun `backup is on`() {
        val application = xml("src/main/AndroidManifest.xml").getElementsByTagName("application").item(0) as Element
        assertEquals("true", application.getAttribute("android:allowBackup"))
    }
}
