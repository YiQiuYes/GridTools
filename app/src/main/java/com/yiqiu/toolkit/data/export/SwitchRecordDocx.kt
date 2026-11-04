package com.yiqiu.toolkit.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import org.apache.poi.xwpf.usermodel.BreakType
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
 * exactly. When the preset data rows of one page are exhausted, a new page is
 * started with a verbatim copy of the template (title, info line, table and
 * notes) inside the same document. Apache POI is the third-party library doing
 * the OOXML read/modify/write; Aalto supplies the STAX implementation Android's
 * runtime lacks.
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
                val filled = fillDocument(doc, data)
                val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
                val file = File(dir, "现场电气操作记录票_${stamp()}.docx")
                FileOutputStream(file).use { filled.write(it) }
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

    /**
     * Testable core: fills an already-opened (pristine) template document with
     * the record data, spilling onto fresh template pages when the preset rows
     * of one page run out. Returns the document holding the final content —
     * it may be a re-opened instance, not the one passed in.
     */
    internal fun fillDocument(doc: XWPFDocument, data: SwitchRecordExportData): XWPFDocument {
        // Snapshot of the untouched template body, used to stamp out new pages.
        val pristine = reopen(doc)
        val scratch = XWPFDocument().apply {
            // Reusable page-break paragraph source for appendTemplatePage.
            createParagraph().createRun().addBreak(BreakType.PAGE)
        }
        try {
            var current = doc
            var remaining = data.entries
            while (true) {
                val table = current.tables.last()
                val capacity = (table.numberOfRows - 1).coerceAtLeast(1)
                fillHeader(current, data)
                fillTableRows(table, remaining.take(capacity))
                remaining = remaining.drop(capacity)
                if (remaining.isEmpty()) break
                appendTemplatePage(current, pristine, scratch)
                // Raw-XML copies are invisible to POI's wrappers; re-open the
                // document so the new table/paragraphs are tracked.
                current = reopen(current)
            }
            return current
        } finally {
            pristine.close()
            scratch.close()
        }
    }

    /** Serializes and re-opens a document, rebuilding POI's object model. */
    private fun reopen(doc: XWPFDocument): XWPFDocument {
        val bytes = ByteArrayOutputStream().also { doc.write(it) }.toByteArray()
        return XWPFDocument(ByteArrayInputStream(bytes))
    }

    // -----------------------------------------------------------------------
    // Header: 操作事由编号 + 操作日期 live in the same paragraph. 操作事由 is an
    // *equal-length replacement*: the value is written right after its label
    // and exactly that many padding characters are removed from the blanks
    // following it, so the line width — and therefore the position of 操作日期
    // — never changes. The 年/月/日 placeholder runs after the date label are
    // replaced by the formatted date (line-end, nothing after it shifts).
    // Both fields skip runs that are already filled, so the refill triggered
    // by multi-page pagination cannot corrupt earlier pages.
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
            if (dateIdx >= 0) fillDate(para, dateIdx, dateText)
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
        val labelAt = original.indexOf(REASON_LABEL)
        if (labelAt < 0) return
        val afterLabel = original.getOrNull(labelAt + REASON_LABEL.length)
        if (afterLabel != null && afterLabel != ' ') return  // already filled
        val trailing = original.length - original.trimEnd(' ').length
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

    /** Writes the formatted date over the 年/月/日 placeholder runs (line-end). */
    private fun fillDate(para: XWPFParagraph, dateIdx: Int, dateText: String) {
        val original = para.runs[dateIdx].getText(0) ?: ""
        val colonAt = original.indexOf("操作日期：")
        if (colonAt < 0) return
        val afterColon = original.getOrNull(colonAt + "操作日期：".length)
        if (afterColon != null && afterColon != ' ') return  // already filled
        para.runs[dateIdx].replaceText("操作日期：$dateText")
        // Drop every run after the date run (the old 年/月/日 + spacing).
        for (i in para.runs.size - 1 downTo dateIdx + 1) para.removeRun(i)
    }

    /**
     * Starts a new page: a page break, then a verbatim copy of the pristine
     * template body (title, info line, table and notes), so overflow entries
     * continue on a fresh preset form inside the same document.
     */
    private fun appendTemplatePage(doc: XWPFDocument, pristine: XWPFDocument, scratch: XWPFDocument) {
        val body = doc.document.body
        val dst = body.sectPr?.newCursor()
            ?: body.newCursor().also { cursor -> cursor.toEndToken() }
        try {
            // Copy the page break, then every template body element except
            // sectPr. The destination cursor stays put, so each copy lands
            // right after the previous one (and before the body's sectPr).
            scratch.paragraphs[0].ctp.newCursor().use { breakSrc ->
                breakSrc.copyXml(dst)
            }
            val src = pristine.document.body.newCursor()
            try {
                if (src.toFirstChild()) {
                    do {
                        if (src.getName()?.localPart == "sectPr") continue
                        src.copyXml(dst)
                    } while (src.toNextSibling())
                }
            } finally {
                src.dispose()
            }
        } finally {
            dst.dispose()
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
    // data rows. Each page receives at most its preset number of entries —
    // overflow goes to a fresh template page instead of cloned rows.
    // Every data row — filled or spare — is normalized to vertical centering
    // (the template ships its data cells with vAlign=top); every column is
    // horizontally centered except 操作任务 (column 3), which stays left-aligned.
    // -----------------------------------------------------------------------

    /** Index of the 操作任务 column — the only left-aligned one. */
    private const val TASK_COLUMN = 3

    private fun fillTableRows(table: XWPFTable, entries: List<ExportEntry>) {
        // Spare (unfilled) rows inherit the template's top alignment; normalize
        // them too so the whole table looks uniform if it is ever edited.
        for (r in 1 until table.numberOfRows) {
            val row = table.getRow(r) ?: continue
            for (c in 0 until row.getTableCells().size) {
                alignCell(
                    row.getCell(c),
                    if (c == TASK_COLUMN) ParagraphAlignment.LEFT else ParagraphAlignment.CENTER,
                )
            }
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
                setCellText(
                    cell,
                    value,
                    if (col == TASK_COLUMN) ParagraphAlignment.LEFT else ParagraphAlignment.CENTER,
                )
            }
        }
    }

    /**
     * Centers a cell vertically and sets its horizontal paragraph alignment,
     * keeping the rest of the cell formatting. The template cells ship their
     * own vAlign; POI's setVerticalAlignment unconditionally *appends* a second
     * w:vAlign instead of overwriting, so drop the existing one first or reads
     * (and renderers) keep seeing the old value.
     */
    private fun alignCell(cell: XWPFTableCell, horizontal: ParagraphAlignment) {
        cell.paragraphs.firstOrNull()?.alignment = horizontal
        val tcPr = cell.ctTc.tcPr
        if (tcPr != null && tcPr.isSetVAlign) tcPr.unsetVAlign()
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER)
    }

    /** Replace a cell's text with the given alignment, keeping the cell formatting. */
    private fun setCellText(cell: XWPFTableCell, text: String, horizontal: ParagraphAlignment) {
        for (paragraph in cell.paragraphs) {
            val count = paragraph.runs.size
            for (i in count - 1 downTo 0) paragraph.removeRun(i)
        }
        alignCell(cell, horizontal)
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
