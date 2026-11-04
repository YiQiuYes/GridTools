package com.yiqiu.toolkit.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Exports a 现场电气操作记录票 by filling the user-supplied Word template
 * (`switch_record_template.docx`) in place.
 *
 * The template is loaded unmodified and only text inside existing runs / table
 * cells is replaced, so every bit of the original formatting — fonts, table
 * borders, column widths, spacing and the explanatory notes — is preserved
 * exactly. Apache POI is the third-party library doing the OOXML read/modify/
 * write; Aalto supplies the STAX implementation Android's runtime lacks.
 */

data class SwitchRecordExportData(
    val recordTime: String,
    val reasonNo: String,
    val entries: List<ExportEntry>,
)

data class ExportEntry(
    val issuer: String,
    val receiver: String,
    val task: String,
    val completedAt: String,
    val operator: String,
    val supervisor: String,
    val note: String,
)

object SwitchRecordDocx {

    private const val TEMPLATE = "switch_record_template.docx"
    private const val AUTHORITY_SUFFIX = ".fileprovider"
    private const val EXPORT_DIR = "exports"

    init {
        // Android ships no javax.xml.stream implementation, so point POI at Aalto.
        runCatching {
            System.setProperty(
                "org.apache.poi.javax.xml.stream.XMLInputFactory",
                "com.fasterxml.aalto.stax.InputFactoryImpl",
            )
            System.setProperty(
                "org.apache.poi.javax.xml.stream.XMLOutputFactory",
                "com.fasterxml.aalto.stax.OutputFactoryImpl",
            )
            System.setProperty(
                "org.apache.poi.javax.xml.stream.XMLEventFactory",
                "com.fasterxml.aalto.stax.EventFactoryImpl",
            )
        }
    }

    /** Opens the bundled template, fills it, and writes a shareable docx to the cache. */
    fun exportToCache(context: Context, data: SwitchRecordExportData): Uri? = try {
        context.assets.open(TEMPLATE).use { input ->
            XWPFDocument(input).use { doc ->
                fillDocument(doc, data)
                val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
                val file = File(dir, "现场电气操作记录票_${stamp()}.docx")
                FileOutputStream(file).use { doc.write(it) }
                FileProvider.getUriForFile(
                    context,
                    context.packageName + AUTHORITY_SUFFIX,
                    file,
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    /** Testable core: mutates an already-opened document with the record data. */
    internal fun fillDocument(doc: XWPFDocument, data: SwitchRecordExportData) {
        fillHeader(doc, data)
        fillTable(doc, data.entries)
    }

    // -----------------------------------------------------------------------
    // Header: 操作事由编号 + 操作日期 live in the same paragraph. We rewrite the
    // single run that carries each label and then *remove* the trailing runs that
    // originally held the 年/月/日 + spacing. The big gap between the two labels
    // (runs that sit before 操作日期) is left untouched, so its layout survives.
    // -----------------------------------------------------------------------
    private fun fillHeader(doc: XWPFDocument, data: SwitchRecordExportData) {
        val dateText = formatDate(data.recordTime)
        for (para in doc.paragraphs) {
            val runs = para.runs
            if (runs.isEmpty()) continue
            val reasonIdx = runs.indexOfFirst { (it.getText(0) ?: "").contains("操作事由（填写编号") }
            if (reasonIdx >= 0) {
                runs[reasonIdx].setText("操作事由（填写编号)：${data.reasonNo}")
            }
            val dateIdx = runs.indexOfFirst { (it.getText(0) ?: "").contains("操作日期") }
            if (dateIdx >= 0) {
                runs[dateIdx].setText("操作日期：$dateText")
                // Drop every run after the date run (the old 年/月/日 + spacing).
                for (i in runs.size - 1 downTo dateIdx + 1) para.removeRun(i)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Table: row 0 is the header, rows 1..N are the (already present, empty)
    // data rows. We set cell text in place; if there are more entries than the
    // template's preset rows we clone the last data row. POI's addRow() only
    // updates the in-memory model and is dropped on write, so we clone via
    // createRow() (which syncs the XML) and copy the source cells across.
    // -----------------------------------------------------------------------
    private fun fillTable(doc: XWPFDocument, entries: List<ExportEntry>) {
        val table = doc.tables.firstOrNull() ?: return
        while (table.numberOfRows - 1 < entries.size) {
            cloneRow(table)
        }
        entries.forEachIndexed { index, entry ->
            val row = table.getRow(index + 1) ?: return@forEachIndexed
            val values = listOf(
                (index + 1).toString(),
                entry.issuer,
                entry.receiver,
                entry.task,
                entry.completedAt,
                entry.operator,
                entry.supervisor,
                entry.note,
            )
            values.forEachIndexed { col, value ->
                val cell = row.getCell(col) ?: return@forEachIndexed
                setCellText(cell, value)
            }
        }
    }

    /** Append a copy of the last data row (cells + formatting) and sync it to XML. */
    private fun cloneRow(table: XWPFTable) {
        val src = table.getRow(table.numberOfRows - 1) ?: return
        val newRow = table.createRow()
        val srcCtRow = src.ctRow
        val newCtRow = newRow.ctRow
        val count = srcCtRow.sizeOfTcArray()
        for (i in 0 until count) {
            val tc = srcCtRow.getTcArray(i)
            newCtRow.addNewTc().set(tc.copy())
        }
    }

    /** Replace a cell's text while keeping its paragraph / table-cell formatting. */
    private fun setCellText(cell: XWPFTableCell, text: String) {
        for (paragraph in cell.paragraphs) {
            val count = paragraph.runs.size
            for (i in count - 1 downTo 0) paragraph.removeRun(i)
        }
        val target = cell.paragraphs.firstOrNull() ?: cell.addParagraph()
        target.createRun().setText(text)
    }

    private fun formatDate(yyyyMmDd: String): String {
        if (yyyyMmDd.isBlank()) return ""
        return runCatching {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(yyyyMmDd)
                ?: return ""
            val cal = Calendar.getInstance().apply { time = parsed }
            String.format(
                Locale.getDefault(),
                "%04d 年 %02d 月 %02d 日",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH),
            )
        }.getOrDefault("")
    }

    private fun stamp(): String {
        val c = Calendar.getInstance()
        return "%04d%02d%02d_%02d%02d%02d".format(
            Locale.getDefault(),
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            c.get(Calendar.SECOND),
        )
    }
}
