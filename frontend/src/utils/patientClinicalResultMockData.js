/**
 * NCL-14-CN-009: Dữ liệu mẫu (Mock Demo Data) cho Cổng bệnh nhân - Kết quả cận lâm sàng
 * Sử dụng khi tài khoản bệnh nhân chưa có dữ liệu khám thực tế hoặc để trình diễn giao diện đầy đủ.
 */

export const MOCK_CLINICAL_VISITS = [
  {
    visitId: 'b1000000-0000-4000-8000-000000000001',
    visitAt: '2026-09-24T08:30:00Z',
    doctorName: 'BS. CKII Nguyễn Văn Hùng',
    specialtyName: 'Nội tổng quát & Nội tiết',
    diagnosisSummary: 'E11 - Đái tháo đường type 2, I10 - Tăng huyết áp vô căn',
  },
  {
    visitId: 'b1000000-0000-4000-8000-000000000002',
    visitAt: '2026-08-15T14:15:00Z',
    doctorName: 'BS. CKI Trần Thị Mai',
    specialtyName: 'Tai Mũi Họng',
    diagnosisSummary: 'J01.0 - Viêm xoang hàm cấp mủ',
  },
  {
    visitId: 'b1000000-0000-4000-8000-000000000003',
    visitAt: '2026-05-20T09:00:00Z',
    doctorName: 'ThS. BS Lê Hoàng Nam',
    specialtyName: 'Tiêu hóa - Gan mật',
    diagnosisSummary: 'K29.7 - Viêm dạ dày không đặc hiệu',
  },
]

export const MOCK_CLINICAL_RESULTS_MAP = {
  // Lượt khám 1: Khám Nội tổng quát & Nội tiết (4 chỉ định phong phú)
  'b1000000-0000-4000-8000-000000000001': [
    {
      clinicalResultId: 'c1000000-0000-4000-8000-000000000101',
      visitId: 'b1000000-0000-4000-8000-000000000001',
      visitAt: '2026-09-24T08:30:00Z',
      serviceCode: 'XN-SH-01',
      serviceName: 'Xét nghiệm Sinh hóa máu (Glucose & HbA1c)',
      resultType: 'NUMBER',
      numericValue: 7.8,
      unit: 'mmol/L',
      referenceRange: '3.9 - 6.4 mmol/L',
      lowerBound: 3.9,
      upperBound: 6.4,
      abnormalFlag: 'HIGH',
      status: 'FINAL',
      enteredAt: '2026-09-24T09:15:00Z',
      doctorName: 'BS. CKII Nguyễn Văn Hùng',
      orderingDoctorName: 'BS. CKII Nguyễn Văn Hùng',
      performingDoctorName: 'KTV. Trần Minh Long',
      specialtyName: 'Khoa Xét nghiệm Sinh hóa',
      conclusion: 'Chỉ số Glucose máu lúc đói tăng nhẹ so với ngưỡng chuẩn. Cần duy trì chế độ ăn kiểm soát carbohydrate và dùng thuốc đúng chỉ định.',
      doctorComment: 'Bệnh nhân tuân thủ phác đồ điều trị và tái khám định kỳ sau 1 tháng.',
      parameters: [
        {
          parameterCode: 'GLU',
          parameterName: 'Glucose máu lúc đói',
          resultType: 'NUMBER',
          numericValue: 7.8,
          unit: 'mmol/L',
          referenceRange: '3.9 - 6.4',
          lowerBound: 3.9,
          upperBound: 6.4,
          abnormalFlag: 'HIGH',
        },
        {
          parameterCode: 'HBA1C',
          parameterName: 'Định lượng HbA1c',
          resultType: 'NUMBER',
          numericValue: 7.2,
          unit: '%',
          referenceRange: '4.0 - 6.0',
          lowerBound: 4.0,
          upperBound: 6.0,
          abnormalFlag: 'HIGH',
        },
        {
          parameterCode: 'CRE',
          parameterName: 'Creatinine huyết thanh',
          resultType: 'NUMBER',
          numericValue: 82,
          unit: 'µmol/L',
          referenceRange: '53 - 106',
          lowerBound: 53,
          upperBound: 106,
          abnormalFlag: 'NORMAL',
        },
        {
          parameterCode: 'AST',
          parameterName: 'Men gan AST (GOT)',
          resultType: 'NUMBER',
          numericValue: 24,
          unit: 'U/L',
          referenceRange: '0 - 35',
          lowerBound: 0,
          upperBound: 35,
          abnormalFlag: 'NORMAL',
        },
        {
          parameterCode: 'ALT',
          parameterName: 'Men gan ALT (GPT)',
          resultType: 'NUMBER',
          numericValue: 28,
          unit: 'U/L',
          referenceRange: '0 - 35',
          lowerBound: 0,
          upperBound: 35,
          abnormalFlag: 'NORMAL',
        },
      ],
    },
    {
      clinicalResultId: 'c1000000-0000-4000-8000-000000000102',
      visitId: 'b1000000-0000-4000-8000-000000000001',
      visitAt: '2026-09-24T08:30:00Z',
      serviceCode: 'XN-HH-01',
      serviceName: 'Tổng phân tích tế bào máu ngoại vi (18 thông số)',
      resultType: 'NUMBER',
      numericValue: 4.68,
      unit: 'T/L',
      referenceRange: '4.0 - 5.5 T/L',
      lowerBound: 4.0,
      upperBound: 5.5,
      abnormalFlag: 'NORMAL',
      status: 'FINAL',
      enteredAt: '2026-09-24T09:20:00Z',
      doctorName: 'BS. Lê Thị Phương Thảo',
      orderingDoctorName: 'BS. CKII Nguyễn Văn Hùng',
      performingDoctorName: 'BS. Lê Thị Phương Thảo',
      specialtyName: 'Khoa Huyết học',
      conclusion: 'Các dòng tế bào máu (hồng cầu, bạch cầu, tiểu cầu) hoàn toàn trong giới hạn bình thường. Không có biểu hiện thiếu máu hoặc nhiễm khuẩn cấp.',
      doctorComment: 'Huyết học ổn định.',
      parameters: [
        {
          parameterCode: 'RBC',
          parameterName: 'Số lượng Hồng cầu (RBC)',
          resultType: 'NUMBER',
          numericValue: 4.68,
          unit: 'T/L',
          referenceRange: '4.0 - 5.5',
          lowerBound: 4.0,
          upperBound: 5.5,
          abnormalFlag: 'NORMAL',
        },
        {
          parameterCode: 'HGB',
          parameterName: 'Huyết sắc tố (Hb)',
          resultType: 'NUMBER',
          numericValue: 142,
          unit: 'g/L',
          referenceRange: '120 - 165',
          lowerBound: 120,
          upperBound: 165,
          abnormalFlag: 'NORMAL',
        },
        {
          parameterCode: 'HCT',
          parameterName: 'Thể tích khối hồng cầu (HCT)',
          resultType: 'NUMBER',
          numericValue: 42.5,
          unit: '%',
          referenceRange: '35 - 47',
          lowerBound: 35,
          upperBound: 47,
          abnormalFlag: 'NORMAL',
        },
        {
          parameterCode: 'WBC',
          parameterName: 'Số lượng Bạch cầu (WBC)',
          resultType: 'NUMBER',
          numericValue: 7.2,
          unit: 'G/L',
          referenceRange: '4.0 - 10.0',
          lowerBound: 4.0,
          upperBound: 10.0,
          abnormalFlag: 'NORMAL',
        },
        {
          parameterCode: 'PLT',
          parameterName: 'Số lượng Tiểu cầu (PLT)',
          resultType: 'NUMBER',
          numericValue: 245,
          unit: 'G/L',
          referenceRange: '150 - 450',
          lowerBound: 150,
          upperBound: 450,
          abnormalFlag: 'NORMAL',
        },
      ],
    },
    {
      clinicalResultId: 'c1000000-0000-4000-8000-000000000103',
      visitId: 'b1000000-0000-4000-8000-000000000001',
      visitAt: '2026-09-24T08:30:00Z',
      serviceCode: 'XN-NT-01',
      serviceName: 'Tổng phân tích nước tiểu (10 thông số)',
      resultType: 'TEXT',
      textValue: 'Glucose niệu (+), Các thông số khác bình thường',
      abnormalFlag: 'HIGH',
      status: 'FINAL',
      enteredAt: '2026-09-24T10:00:00Z',
      doctorName: 'KTV. Hoàng Minh Tuấn',
      orderingDoctorName: 'BS. CKII Nguyễn Văn Hùng',
      performingDoctorName: 'KTV. Hoàng Minh Tuấn',
      specialtyName: 'Khoa Xét nghiệm',
      conclusion: 'Phát hiện Glucose trong nước tiểu, tương ứng với đường huyết tăng nhẹ. Không có Protein niệu, không có tế bào mủ.',
      doctorComment: 'Cần kiểm soát tốt đường huyết.',
      parameters: [
        { parameterCode: 'GLU_U', parameterName: 'Glucose niệu', resultType: 'TEXT', textValue: 'Dương tính (+)', referenceRange: 'Âm tính (-)', abnormalFlag: 'HIGH' },
        { parameterCode: 'PRO_U', parameterName: 'Protein niệu', resultType: 'TEXT', textValue: 'Âm tính (-)', referenceRange: 'Âm tính (-)', abnormalFlag: 'NORMAL' },
        { parameterCode: 'LEU_U', parameterName: 'Bạch cầu niệu', resultType: 'TEXT', textValue: 'Âm tính (-)', referenceRange: 'Âm tính (-)', abnormalFlag: 'NORMAL' },
        { parameterCode: 'NIT_U', parameterName: 'Nitrite niệu', resultType: 'TEXT', textValue: 'Âm tính (-)', referenceRange: 'Âm tính (-)', abnormalFlag: 'NORMAL' },
        { parameterCode: 'KET_U', parameterName: 'Ketone niệu', resultType: 'TEXT', textValue: 'Âm tính (-)', referenceRange: 'Âm tính (-)', abnormalFlag: 'NORMAL' },
      ],
    },
    {
      clinicalResultId: 'c1000000-0000-4000-8000-000000000104',
      visitId: 'b1000000-0000-4000-8000-000000000001',
      visitAt: '2026-09-24T08:30:00Z',
      serviceCode: 'CDHA-XQ-01',
      serviceName: 'Chụp X-quang tim phổi thẳng kỹ thuật số (DR)',
      resultType: 'TEXT',
      textValue: 'Hai phế trường sáng đều, bóng tim bình thường',
      abnormalFlag: 'NORMAL',
      status: 'FINAL',
      enteredAt: '2026-09-24T10:15:00Z',
      doctorName: 'BS. CKII Phạm Quốc Trung',
      orderingDoctorName: 'BS. CKII Nguyễn Văn Hùng',
      performingDoctorName: 'BS. CKII Phạm Quốc Trung',
      specialtyName: 'Khoa Chẩn đoán hình ảnh',
      conclusion: 'Hai phế trường sáng đều, không thấy tổn thương thâm nhiễm nhu mô phổi khu trú. Bóng tim trong giới hạn bình thường (chỉ số tim - lồng ngực < 0.5). Góc tâm hoành và sườn hoành hai bên sáng nhọn.',
      doctorComment: 'Không phát hiện bất thường tim phổi cấp tính.',
      attachments: [
        {
          attachmentId: 'att-1',
          fileName: 'XQuang-Nguc-Thang-PA.jpg',
          fileType: 'image/jpeg',
          fileSize: 1845200,
        },
      ],
    },
  ],

  // Lượt khám 2: Khám Tai Mũi Họng (2 chỉ định)
  'b1000000-0000-4000-8000-000000000002': [
    {
      clinicalResultId: 'c1000000-0000-4000-8000-000000000201',
      visitId: 'b1000000-0000-4000-8000-000000000002',
      visitAt: '2026-08-15T14:15:00Z',
      serviceCode: 'TDCN-NS-01',
      serviceName: 'Nội soi Tai Mũi Họng ống mềm có ghi hình',
      resultType: 'TEXT',
      textValue: 'Cuốn mũi phù nề xung huyết, khe giữa hai bên đọng dịch mủ vàng đặc',
      abnormalFlag: 'HIGH',
      status: 'FINAL',
      enteredAt: '2026-08-15T14:45:00Z',
      doctorName: 'BS. CKI Trần Thị Mai',
      orderingDoctorName: 'BS. CKI Trần Thị Mai',
      performingDoctorName: 'BS. CKI Trần Thị Mai',
      specialtyName: 'Khoa Tai Mũi Họng',
      conclusion: 'Viêm xoang hàm hai bên cấp mủ. Niêm mạc mũi họng sung huyết nhẹ, họng và thanh quản không có u sùi bất thường.',
      doctorComment: 'Đã thực hiện hút rửa xoang, chỉ định dùng thuốc kháng sinh và kháng viêm theo toa.',
    },
    {
      clinicalResultId: 'c1000000-0000-4000-8000-000000000202',
      visitId: 'b1000000-0000-4000-8000-000000000002',
      visitAt: '2026-08-15T14:15:00Z',
      serviceCode: 'XN-SH-05',
      serviceName: 'Định lượng CRP (C-Reactive Protein) huyết thanh',
      resultType: 'NUMBER',
      numericValue: 12.8,
      unit: 'mg/L',
      referenceRange: '0 - 5.0 mg/L',
      lowerBound: 0,
      upperBound: 5.0,
      abnormalFlag: 'HIGH',
      status: 'FINAL',
      enteredAt: '2026-08-15T15:00:00Z',
      doctorName: 'BS. Lê Thị Phương Thảo',
      orderingDoctorName: 'BS. CKI Trần Thị Mai',
      performingDoctorName: 'BS. Lê Thị Phương Thảo',
      specialtyName: 'Khoa Xét nghiệm',
      conclusion: 'Chỉ số phản ứng viêm tăng mức độ nhẹ, phù hợp với đợt viêm xoang nhiễm trùng cấp tính.',
      doctorComment: 'Theo dõi đáp ứng sau 5 ngày điều trị.',
    },
  ],

  // Lượt khám 3: Khám Tiêu hóa (1 chỉ định)
  'b1000000-0000-4000-8000-000000000003': [
    {
      clinicalResultId: 'c1000000-0000-4000-8000-000000000301',
      visitId: 'b1000000-0000-4000-8000-000000000003',
      visitAt: '2026-05-20T09:00:00Z',
      serviceCode: 'TDCN-HP-01',
      serviceName: 'Test hơi thở tìm vi khuẩn Helicobacter Pylori (C13/C14)',
      resultType: 'NUMBER',
      numericValue: 14.5,
      unit: 'DPM',
      referenceRange: '< 50 DPM (Âm tính)',
      lowerBound: 0,
      upperBound: 50,
      abnormalFlag: 'NORMAL',
      status: 'FINAL',
      enteredAt: '2026-05-20T09:40:00Z',
      doctorName: 'ThS. BS Lê Hoàng Nam',
      orderingDoctorName: 'ThS. BS Lê Hoàng Nam',
      performingDoctorName: 'ThS. BS Lê Hoàng Nam',
      specialtyName: 'Khoa Tiêu hóa',
      conclusion: 'Âm tính với Helicobacter Pylori. Bệnh nhân không nhiễm khuẩn HP trong dạ dày.',
      doctorComment: 'Duy trì thuốc ức chế tiết acid và chế độ ăn đúng giờ, tránh cay nóng.',
    },
  ],
}

/**
 * Sinh tệp PDF giả lập tải về trực tiếp từ trình duyệt cho dữ liệu mẫu
 */
export const createMockPdfBlob = (title, meta = {}) => {
  const content = `%PDF-1.4
1 0 obj
<< /Title (${title}) /Author (He thong Benh An So) >>
endobj
------------------------------------------------------------
BỆNH ÁN SỐ - CỔNG THÔNG TIN BỆNH NHÂN
PHÒNG KHÁM ĐA KHOA QUỐC TẾ
------------------------------------------------------------
${title}
Thời gian xuất phiếu: ${new Date().toLocaleString('vi-VN')}
Bệnh nhân: ${meta.patientName || '0966069024'}
Bác sĩ phụ trách: ${meta.doctorName || 'BS chuyên khoa'}
Chuyên khoa: ${meta.specialtyName || 'Đa khoa'}
Chẩn đoán: ${meta.diagnosis || 'Theo dõi định kỳ'}
------------------------------------------------------------
KẾT QUẢ CẬN LÂM SÀNG:
${meta.details || 'Đã ghi nhận kết quả chính thức (Status: FINAL)'}
------------------------------------------------------------
Kết luận: ${meta.conclusion || 'Bình thường'}
Ghi chú: Phiếu kết quả đã được số hóa và lưu trữ an toàn trong Hồ sơ bệnh án điện tử.
`
  return new Blob([content], { type: 'application/pdf' })
}
