package com.yiqiu.toolkit.data.export

import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private fun cellText(cell: XWPFTableCell): String =
    cell.paragraphs.joinToString("") { it.text ?: "" }

/**
 * Exercises the POI-based template fill on the JVM (no Android runtime needed):
 * it loads the exact template shipped in assets, fills it, writes a temp docx,
 * then re-opens that docx and asserts both the injected content and the
 * original formatting (title, notes, table shape) survived.
 */
class SwitchRecordDocxTest {

    private val template = File("src/main/assets/switch_record_template.docx")

    private fun sample(entries: List<ExportEntry>, reasonNo: String = "a 配网事故紧急处理") =
        SwitchRecordExportData(
            recordTime = "2026-11-05",
            reasonNo = reasonNo,
            entries = entries,
        )

    private fun writeFilled(data: SwitchRecordExportData): File {
        assertTrue("template asset must exist", template.exists())
        val out = File.createTempFile("record_filled_", ".docx")
        FileInputStream(template).use { input ->
            XWPFDocument(input).use { templateDoc ->
                val filled = SwitchRecordDocx.fillDocument(templateDoc, data)
                FileOutputStream(out).use { filled.write(it) }
            }
        }
        return out
    }

    @Test
    fun fillsHeaderAndTableWhileKeepingTitleAndNotes() {
        val entries = listOf(
            ExportEntry("张一", "李二", "断开 1 号主变 101 开关", "08:30", "王三", "赵四", "第一项"),
            ExportEntry("张一", "李二", "检查 101 开关在分位", "08:45", "王三", "赵四", ""),
            ExportEntry("张一", "李二", "合上 101 开关", "09:00", "王三", "赵四", "完毕"),
        )
        val out = writeFilled(sample(entries))
        assertTrue(out.exists() && out.length() > 0)

        FileInputStream(out).use { input ->
            XWPFDocument(input).use { doc ->
                val body = doc.paragraphs.joinToString("\n") { it.text ?: "" }

                // Original static content is preserved verbatim.
                assertTrue("title kept", body.contains("柳州供电局 现场电气操作记录票"))
                assertTrue("note 1 kept", body.contains("说明： 1、本表用于符合"))
                assertTrue("note 2 kept", body.contains("操作事由栏填写"))
                assertTrue("note 3 kept", body.contains("3、本表由操作人员在现场操作时及时记录。"))

                // Injected header content: only the code letter goes after the label.
                assertTrue("reason filled", body.contains("操作事由（填写编号)：a"))
                assertTrue("date filled", body.contains("操作日期：2026 年 11 月 05 日"))
                assertFalse("old year/month/day blanks gone", body.contains("年   月   日"))

                // Table shape: 1 header + 8 preset rows, unchanged for <=8 entries.
                val table = doc.tables.first()
                assertEquals(9, table.numberOfRows)

                val r1 = table.getRow(1)
                assertEquals("1", cellText(r1.getCell(0)))
                assertEquals("张一", cellText(r1.getCell(1)))
                assertEquals("李二", cellText(r1.getCell(2)))
                assertEquals("断开 1 号主变 101 开关", cellText(r1.getCell(3)))
                assertEquals("08:30", cellText(r1.getCell(4)))
                assertEquals("王三", cellText(r1.getCell(5)))
                assertEquals("赵四", cellText(r1.getCell(6)))
                assertEquals("第一项", cellText(r1.getCell(7)))

                // Filled cells are vertically centered; centered horizontally
                // except the 操作任务 column, which is left-aligned.
                assertEquals(
                    "data cell v-centered",
                    XWPFTableCell.XWPFVertAlign.CENTER,
                    r1.getCell(1).verticalAlignment,
                )
                assertEquals(
                    "receiver v-centered",
                    XWPFTableCell.XWPFVertAlign.CENTER,
                    r1.getCell(2).verticalAlignment,
                )
                assertEquals(
                    "issuer centered",
                    ParagraphAlignment.CENTER,
                    r1.getCell(1).paragraphs[0].alignment,
                )
                assertEquals(
                    "task left-aligned",
                    ParagraphAlignment.LEFT,
                    r1.getCell(3).paragraphs[0].alignment,
                )
                assertEquals(
                    "note centered",
                    ParagraphAlignment.CENTER,
                    r1.getCell(7).paragraphs[0].alignment,
                )

                // The remaining preset rows stay blank, not corrupted.
                assertEquals("", cellText(table.getRow(4).getCell(1)).trim())
            }
        }
    }

    @Test
    fun continuesOnNewTemplatePageWhenPresetRowsAreExhausted() {
        val many = List(10) { i ->
            ExportEntry("发令人$i", "受令人$i", "任务$i", "10:${"%02d".format(i)}", "操作$i", "监护$i", "备注$i")
        }
        val out = writeFilled(sample(many))
        FileInputStream(out).use { input ->
            XWPFDocument(input).use { doc ->
                // 10 entries > 8 preset rows: a second template page is appended.
                assertEquals(2, doc.tables.size)
                val page1 = doc.tables[0]
                val page2 = doc.tables[1]
                assertEquals(9, page1.numberOfRows)
                assertEquals(9, page2.numberOfRows)

                // Page 1 carries entries 1-8.
                assertEquals("发令人0", cellText(page1.getRow(1).getCell(1)))
                assertEquals("发令人7", cellText(page1.getRow(8).getCell(1)))

                // Page 2 is a fresh preset form carrying entries 9-10.
                assertEquals("发令人8", cellText(page2.getRow(1).getCell(1)))
                assertEquals("发令人9", cellText(page2.getRow(2).getCell(1)))
                assertEquals("", cellText(page2.getRow(3).getCell(1)).trim())

                // Even the spare rows on page 2 are vertically centered — the
                // template copy must not keep its original top alignment.
                assertEquals(
                    "page2 spare row v-centered",
                    XWPFTableCell.XWPFVertAlign.CENTER,
                    page2.getRow(3).getCell(1).verticalAlignment,
                )
                // Its header row matches the original table header.
                assertEquals(
                    cellText(page1.getRow(0).getCell(0)),
                    cellText(page2.getRow(0).getCell(0)),
                )

                // The template body (title + notes) is present on both pages.
                val titles = doc.paragraphs.count { (it.text ?: "").contains("柳州供电局 现场电气操作记录票") }
                assertEquals("title duplicated on new page", 2, titles)

                // A page break separates the two template copies.
                val hasPageBreak = doc.paragraphs.any { it.ctp.xmlText().contains("type=\"page\"") }
                assertTrue("page break inserted", hasPageBreak)
            }
        }
    }

    @Test
    fun reasonNoReplacementKeepsHeaderLineLength() {
        fun headerText(f: File): String = FileInputStream(f).use { input ->
            XWPFDocument(input).use { doc ->
                doc.paragraphs.map { it.text ?: "" }
                    .first { it.contains("操作事由（填写编号") }
            }
        }
        val before = headerText(template)

        // Short value ("a"): the trailing padding shrinks by exactly 1 char.
        val short = headerText(writeFilled(sample(emptyList())))
        assertEquals("line length kept for short value", before.length, short.length)
        assertTrue(short.startsWith("操作事由（填写编号)：a "))

        // Long value: eats into the following space runs; the total line length
        // is still unchanged and 操作日期 keeps its position at the same offset.
        val longReason = "b".repeat(30)
        val long = headerText(writeFilled(sample(emptyList(), longReason)))
        assertEquals("line length kept for long value", before.length, long.length)
        assertTrue(long.contains("操作事由（填写编号)：$longReason"))
        val dateAt = long.indexOf("操作日期：")
        assertEquals("操作日期 unmoved", before.indexOf("操作日期："), dateAt)
    }
}
