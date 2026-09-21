package com.benhsoan.infrastructure.spreadsheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class ExcelPatientSheetParserTest {

    private final ExcelPatientSheetParser parser = new ExcelPatientSheetParser();
    private final ExcelPatientTemplateGenerator generator = new ExcelPatientTemplateGenerator();

    @Test
    @DisplayName("Should successfully parse template spreadsheet with sample rows")
    void shouldParseSampleTemplate() {
        byte[] templateBytes = generator.generateTemplate();
        List<RawPatientRowDto> rows = parser.parse(new ByteArrayInputStream(templateBytes));

        assertThat(rows).hasSize(2);

        RawPatientRowDto adult = rows.get(0);
        assertThat(adult.getRowNumber()).isEqualTo(2);
        assertThat(adult.getFullName()).isEqualTo("Nguyễn Văn An");
        assertThat(adult.getDateOfBirth()).isEqualTo("15/05/1988");
        assertThat(adult.getGender()).isEqualTo("Nam");
        assertThat(adult.getPhone()).isEqualTo("0901234567");

        RawPatientRowDto minor = rows.get(1);
        assertThat(minor.getRowNumber()).isEqualTo(3);
        assertThat(minor.getFullName()).isEqualTo("Nguyễn Minh Khang");
        assertThat(minor.getGuardianName()).isEqualTo("Nguyễn Văn An");
        assertThat(minor.getGuardianRelationship()).isEqualTo("Bố");
        assertThat(minor.getGuardianPhone()).isEqualTo("0901234567");
    }

    @Test
    @DisplayName("Should throw ValidationException when sheet has only header and no data rows")
    void shouldThrowWhenNoDataRows() throws Exception {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("EmptyData");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Họ và tên");
            wb.write(out);

            byte[] bytes = out.toByteArray();
            assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(bytes)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("không có dữ liệu");
        }
    }

    @Test
    @DisplayName("Should throw ValidationException when spreadsheet has no sheets")
    void shouldThrowWhenNoSheets() throws Exception {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);
            byte[] bytes = out.toByteArray();
            assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(bytes)))
                    .isInstanceOf(ValidationException.class);
        }
    }
}
