package com.benhsoan.port.outbound.spreadsheet;

import java.io.InputStream;
import java.util.List;

import com.benhsoan.port.dto.spreadsheet.RawPatientRowDto;

public interface PatientSpreadsheetParserPort {
    List<RawPatientRowDto> parse(InputStream inputStream);
}
