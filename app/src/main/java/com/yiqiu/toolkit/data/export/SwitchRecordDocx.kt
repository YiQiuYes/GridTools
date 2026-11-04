package com.yiqiu.toolkit.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
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

    /** The 事由 label as it appears in the template run (half-width `)`, full-width `：`). */
    private const val REASON_LABEL = "操作事由（填写编号)："

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
    // Header: 操作事由编号 + 操作日期 live in the same paragraph. 操作事由 is an
    // *equal-length replacement*: the value is written right after its label
    // and exactly that many padding characters are removed from the blanks
    // following it, so the line width — and therefore the position of 操作日期
    // — never changes. The 年/月/日 placeholder runs after the date label are
    // replaced by the formatted date (line-end, nothing after it shifts).
    // -----------------------------------------------------------------------
    private fun fillHeader(doc: XWPFDocument, data: SwitchRecordExportData) {
        val dateText = formatDate(data.recordTime)
        val reasonNo = data.reasonNo.trim().split(" ").firstOrNull().orEmpty()
        for (para in doc.paragraphs) {
            val runs = para.runs
            if (runs.isEmpty()) continue
            val reasonIdx = runs.indexOfFirst { (it.getText(0) ?: "").contains(REASON_LABEL) }
            if (reasonIdx >= 0) fillReasonNo(para, reasonIdx, reasonNo)
            // Re-read the runs: fillReasonNo may have removed trailing runs.
            val dateIdx = para.runs.indexOfFirst { (it.getText(0) ?: "").contains("操作日期") }
            if (dateIdx >= 0) {
                para.runs[dateIdx].replaceText("操作日期：$dateText")
                // Drop every run after the date run (the old 年/月/日 + spacing).
                for (i in para.runs.size - 1 downTo dateIdx + 1) para.removeRun(i)
            }
        }
    }

    /**
     * Equal-length replacement: writes [value] right after [REASON_LABEL] and
     * removes exactly `value.length` characters from the padding that follows
     * it — first the spaces trailing the label inside this run, then whole
     * following space-only runs — so the rest of the line does not shift.
     */
    private fun fillReasonNo(para: XWPFParagraph, reasonIdx: Int, value: String) {
        val run = para.runs[reasonIdx]
        val original = run.getText(0) ?: ""
        val trailing = original.length - original.trimEnd(' ').length
        if (original.length <= trailing) return  // no label in this run — bail out
        val keep = (trailing - value.length).coerceAtLeast(0)
        run.replaceText(original.dropLast(trailing) + value + " ".repeat(keep))
        var overflow = (value.length - trailing).coerceAtLeast(0)
        var i = reasonIdx + 1
        while (overflow > 0 && i < para.runs.size) {
            val text = para.runs[i].getText(0) ?: ""
            if (text.any { it != ' ' }) break  // reached real content (操作日期…)
            if (text.length <= overflow) {
                overflow -= text.length
                para.removeRun(i)  // the next run shifts into index i
            } else {
                para.runs[i].replaceText(text.substring(overflow))
                overflow = 0
            }
        }
    }

    /**
     * POI's [XWPFRun.setText] *appends* a new w:t node instead of replacing the
     * run's text, which silently duplicates content and widens the line. Drop
     * every w:t beyond the first, then write through position 0 (which also
     * keeps POI's xml:space="preserve" handling for trailing spaces intact).
     */
    private fun XWPFRun.replaceText(text: String) {
        val ctr = ctr
        while (ctr.sizeOfTArray() > 1) ctr.removeT(ctr.sizeOfTArray() - 1)
        setText(text, 0)
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
