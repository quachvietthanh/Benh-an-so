package com.benhsoan.infrastructure.spreadsheet;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExcelPatientTemplateGeneratorTest {

    private final ExcelPatientTemplateGenerator generator = new ExcelPatientTemplateGenerator();

    @Test
    @DisplayName("Should generate valid Excel patient import template with headers and sample rows")
    void shouldGenerateValidTemplate() throws Exception {
        byte[] bytes = generator.generateTemplate();

        assertThat(bytes).isNotNull().isNotEmpty();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Danh_sach_benh_nhan");

            // Verify header
            Row headerRow = sheet.getRow(0);
            assertThat(headerRow).isNotNull();
            assertThat(headerRow.getCell(0).getStringCellValue()).contains("Họ và tên");
            assertThat(headerRow.getCell(1).getStringCellValue()).contains("Ngày sinh");
            assertThat(headerRow.getCell(2).getStringCellValue()).contains("Giới tính");
            assertThat(headerRow.getCell(12).getStringCellValue()).contains("người giám hộ");

            // Verify sample adult row
            Row adultRow = sheet.getRow(1);
            assertThat(adultRow.getCell(0).getStringCellValue()).isEqualTo("Nguyễn Văn An");
            assertThat(adultRow.getCell(2).getStringCellValue()).isEqualTo("Nam");

            // Verify sample minor row
            Row minorRow = sheet.getRow(2);
            assertThat(minorRow.getCell(0).getStringCellValue()).isEqualTo("Nguyễn Minh Khang");
            assertThat(minorRow.getCell(12).getStringCellValue()).isEqualTo("Nguyễn Văn An"); // Guardian name
        }
    }
}
