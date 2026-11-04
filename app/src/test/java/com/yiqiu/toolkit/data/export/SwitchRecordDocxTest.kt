package com.yiqiu.toolkit.data.export

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

    private fun sample(entries: List<ExportEntry>) = SwitchRecordExportData(
        recordTime = "2026-11-05",
        reasonNo = "a 配网事故紧急处理",
        entries = entries,
    )

    private fun writeFilled(data: SwitchRecordExportData): File {
        assertTrue("template asset must exist", template.exists())
        val out = File.createTempFile("record_filled_", ".docx")
        FileInputStream(template).use { input ->
            XWPFDocument(input).use { doc ->
                SwitchRecordDocx.fillDocument(doc, data)
                FileOutputStream(out).use { doc.write(it) }
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

                // Injected header content.
                assertTrue("reason filled", body.contains("操作事由（填写编号)：a 配网事故紧急处理"))
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

                // The remaining preset rows stay blank, not corrupted.
                assertEquals("", cellText(table.getRow(4).getCell(1)).trim())
            }
        }
    }

    @Test
    fun clonesRowsWhenEntriesExceedPresetRows() {
        val many = List(10) { i ->
            ExportEntry("发令人$i", "受令人$i", "任务$i", "10:${"%02d".format(i)}", "操作$i", "监护$i", "备注$i")
        }
        val out = writeFilled(sample(many))
        FileInputStream(out).use { input ->
            XWPFDocument(input).use { doc ->
                val table = doc.tables.first()
                // 1 header + 10 data rows.
                assertEquals(11, table.numberOfRows)
                val last = table.getRow(10)
                assertEquals("10", cellText(last.getCell(0)))
                assertEquals("发令人9", cellText(last.getCell(1)))
                assertEquals("任务9", cellText(last.getCell(3)))
            }
        }
    }
}
