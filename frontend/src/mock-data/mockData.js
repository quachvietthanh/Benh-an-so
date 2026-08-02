export const demoUsers = [
  {
    id: 'u1',
    username: 'admin',
    password: 'admin',
    fullName: 'Nguyễn Thị Lan',
    email: 'admin@benhsoan.vn',
    roles: ['admin'],
  },
  {
    id: 'u2',
    username: 'manager',
    password: 'manager',
    fullName: 'Trần Văn Minh',
    email: 'manager@benhsoan.vn',
    roles: ['manager'],
  },
  {
    id: 'u3',
    username: 'doctor',
    password: 'doctor',
    fullName: 'BS. Phạm Hồng Anh',
    email: 'doctor@benhsoan.vn',
    roles: ['doctor'],
  },
  {
    id: 'u4',
    username: 'receptionist',
    password: 'receptionist',
    fullName: 'Lê Thị Hạnh',
    email: 'reception@benhsoan.vn',
    roles: ['receptionist'],
  },
  {
    id: 'u5',
    username: 'pharmacist',
    password: 'pharmacist',
    fullName: 'Đặng Thị Uyên',
    email: 'pharm@benhsoan.vn',
    roles: ['pharmacist'],
  },
]

export const demoPatients = [
  {
    id: 'p1',
    patientCode: 'BN000009',
    fullName: 'Nguyen Tuan Long',
    dateOfBirth: '1983-05-12',
    gender: 'MALE',
    phoneNumber: '0910000009',
    email: 'long.nguyen@email.com',
    address: '123 Lê Lợi, Quận 1, TP.HCM',
    identityNumber: '012345678910',
    healthInsuranceCode: 'BH123456789',
    bloodType: 'A+',
    emergencyContact: 'Nguyễn Thị Hoa - 0911123456',
    medicalHistory: 'Tăng huyết áp',
    allergies: 'Penicillin',
  },
  {
    id: 'p2',
    patientCode: 'BN-2026002',
    fullName: 'Trần Thị Bình',
    dateOfBirth: '1992-09-20',
    gender: 'FEMALE',
    phoneNumber: '0912345678',
    email: 'binh.tran@email.com',
    address: '45 Hai Bà Trưng, Quận 3, TP.HCM',
    identityNumber: '098765432109',
    healthInsuranceCode: 'BH987654321',
    bloodType: 'O+',
    emergencyContact: 'Trần Văn Cường - 0987654321',
    medicalHistory: 'Đái tháo đường',
    allergies: 'Không có',
  },
  {
    id: 'p3',
    patientCode: 'BN-774016',
    fullName: 'nguyễn công bằng',
    dateOfBirth: '2005-01-08',
    gender: 'MALE',
    phoneNumber: '0398318097',
    email: 'bang.nguyen@email.com',
    address: '88 Nguyễn Huệ, Quận 5, TP.HCM',
    identityNumber: '011223344556',
    healthInsuranceCode: 'BH111222333',
    bloodType: 'B+',
    emergencyContact: 'Lê Thu Hà - 0931234567',
    medicalHistory: 'Viêm phế quản',
    allergies: 'Không có',
  },
]

export const demoAppointments = [
  { id: 'a1', patientId: 'p1', patientName: 'Nguyen Tuan Long', doctorId: 'd1', doctorName: 'BS. Phạm Hồng Anh', slot: '08:00', date: '2026-07-15', status: 'SCHEDULED' },
  { id: 'a2', patientId: 'p2', patientName: 'Trần Thị Bình', doctorId: 'd1', doctorName: 'BS. Phạm Hồng Anh', slot: '09:30', date: '2026-07-15', status: 'CHECKED_IN' },
  { id: 'a3', patientId: 'p3', patientName: 'nguyễn công bằng', doctorId: 'd2', doctorName: 'BS. Nguyễn Minh', slot: '10:00', date: '2026-07-15', status: 'NO_SHOW' },
]

export const demoMedicalRecords = [
  {
    id: 'm1',
    recordCode: 'BA-0001',
    patientId: 'p1',
    patientName: 'Nguyen Tuan Long',
    doctorName: 'BS. Phạm Hồng Anh',
    diagnosis: 'Tăng huyết áp mức độ vừa',
    status: 'COMPLETED',
    createdAt: '2026-06-30T08:00:00',
  },
  {
    id: 'm2',
    recordCode: 'BA-0002',
    patientId: 'p2',
    patientName: 'Trần Thị Bình',
    doctorName: 'BS. Phạm Hồng Anh',
    diagnosis: 'Đái tháo đường type 2',
    status: 'IN_PROGRESS',
    createdAt: '2026-07-10T09:30:00',
  },
]

export const demoMedicines = [
  { id: 'med1', name: 'Amlodipine 5mg', category: 'Tim mạch', stock: 120, minStock: 20, expiryDate: '2027-10-01', lot: 'LOT-AM-001', price: 2000 },
  { id: 'med2', name: 'Metformin 500mg', category: 'Tiểu đường', stock: 8, minStock: 20, expiryDate: '2026-08-15', lot: 'LOT-MT-002', price: 1500 },
  { id: 'med3', name: 'Paracetamol 500mg', category: 'Giảm đau', stock: 300, minStock: 50, expiryDate: '2028-01-20', lot: 'LOT-PA-003', price: 1000 },
  { id: 'med4', name: 'Ibuprofen 400mg', category: 'Giảm đau, Kháng viêm', stock: 150, minStock: 30, expiryDate: '2027-05-10', lot: 'LOT-IB-004', price: 2500 },
  { id: 'med5', name: 'Aspirin 81mg', category: 'Tim mạch', stock: 200, minStock: 40, expiryDate: '2028-02-15', lot: 'LOT-AS-005', price: 1200 },
]

export const drugInteractions = [
  {
    drugs: ['med3', 'med4'], 
    severity: 'Cảnh báo (Vừa)',
    description: 'Dùng chung Paracetamol và Ibuprofen có thể tăng nguy cơ tác dụng phụ lên dạ dày và thận. Cần cân nhắc liều lượng.'
  },
  {
    drugs: ['med4', 'med5'], 
    severity: 'Nghiêm trọng (Cao)',
    description: 'Ibuprofen làm giảm tác dụng bảo vệ tim mạch của Aspirin liều thấp và tăng nguy cơ xuất huyết tiêu hóa. Chống chỉ định dùng chung.'
  }
]

export const demoPrescriptions = [
  { id: 'pre1', recordId: 'm2', medicines: [{ id: 'med1', quantity: 30, dosage: 'Sáng 1 viên' }, { id: 'med2', quantity: 60, dosage: 'Sáng 1 viên, Tối 1 viên' }] },
]

export const demoInvoices = [
  { id: 'inv1', invoiceCode: 'HD-0001', patientName: 'Nguyễn Văn An', amount: 250000, status: 'PAID', createdAt: '2026-06-30T09:00:00', adjustmentOf: null },
  { id: 'inv2', invoiceCode: 'HD-0002', patientName: 'Trần Thị Bình', amount: 450000, status: 'PENDING', createdAt: '2026-07-10T10:30:00', adjustmentOf: null },
]

export const demoAuditLogs = [
  { id: 'log1', user: 'BS. Phạm Hồng Anh', patient: 'Nguyễn Văn An', action: 'Xem hồ sơ bệnh án', time: '2026-07-12 08:10' },
  { id: 'log2', user: 'Lê Thị Hạnh', patient: 'Trần Thị Bình', action: 'Cập nhật hồ sơ hành chính', time: '2026-07-12 09:40' },
]

export const demoServices = [
  { id: 'svc1', name: 'Khám tổng quát', price: 150000, effectiveFrom: '2026-01-01', status: 'ACTIVE' },
  { id: 'svc2', name: 'Xét nghiệm máu cơ bản', price: 220000, effectiveFrom: '2026-01-01', status: 'ACTIVE' },
  { id: 'svc3', name: 'Siêu âm bụng', price: 320000, effectiveFrom: '2026-04-01', status: 'ACTIVE' },
]

export const demoClinicalCatalog = [
  // Hematology & Biochemistry (LAB_TEST)
  { id: 'f0000000-0000-0000-0000-000000000010', code: 'LAB-CBC', name: 'Công thức máu toàn bộ', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 120000, room: 'Phòng 101 - Huyết học', description: 'Đánh giá các chỉ số hồng cầu, bạch cầu và tiểu cầu.' },
  { id: 'f0000000-0000-0000-0000-000000000011', code: 'LAB-HGB', name: 'Định lượng Hemoglobin', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 80000, room: 'Phòng 101 - Huyết học', description: 'Đánh giá tình trạng thiếu máu.' },
  { id: 'f0000000-0000-0000-0000-000000000012', code: 'LAB-PLT', name: 'Đếm số lượng tiểu cầu', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 70000, room: 'Phòng 101 - Huyết học', description: 'Đánh giá số lượng tiểu cầu.' },
  { id: 'f0000000-0000-0000-0000-000000000013', code: 'LAB-ESR', name: 'Tốc độ máu lắng', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 60000, room: 'Phòng 101 - Huyết học', description: 'Chỉ dấu viêm không đặc hiệu.' },
  { id: 'f0000000-0000-0000-0000-000000000014', code: 'LAB-HBA1C', name: 'Định lượng HbA1c', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 190000, room: 'Phòng 102 - Sinh hóa', description: 'Theo dõi đường huyết trung bình trong 2-3 tháng.' },
  { id: 'f0000000-0000-0000-0000-000000000015', code: 'LAB-UREA', name: 'Định lượng Ure máu', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 60000, room: 'Phòng 102 - Sinh hóa', description: 'Đánh giá chức năng thận.' },
  { id: 'f0000000-0000-0000-0000-000000000016', code: 'LAB-CREA', name: 'Định lượng Creatinin máu', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 70000, room: 'Phòng 102 - Sinh hóa', description: 'Đánh giá chức năng thận.' },
  { id: 'f0000000-0000-0000-0000-000000000017', code: 'LAB-AST', name: 'Men gan AST (GOT)', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 65000, room: 'Phòng 102 - Sinh hóa', description: 'Đánh giá tổn thương tế bào gan.' },
  { id: 'f0000000-0000-0000-0000-000000000018', code: 'LAB-ALT', name: 'Men gan ALT (GPT)', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 65000, room: 'Phòng 102 - Sinh hóa', description: 'Đánh giá tổn thương tế bào gan.' },
  { id: 'f0000000-0000-0000-0000-000000000019', code: 'LAB-CHOL', name: 'Cholesterol toàn phần', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 60000, room: 'Phòng 102 - Sinh hóa', description: 'Đánh giá rối loạn mỡ máu.' },
  { id: 'f0000000-0000-0000-0000-000000000020', code: 'LAB-TG', name: 'Triglycerid', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 60000, room: 'Phòng 102 - Sinh hóa', description: 'Đánh giá rối loạn mỡ máu.' },
  { id: 'f0000000-0000-0000-0000-000000000021', code: 'LAB-LDL', name: 'Cholesterol LDL', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 75000, room: 'Phòng 102 - Sinh hóa', description: 'Đánh giá nguy cơ tim mạch.' },
  { id: 'f0000000-0000-0000-0000-000000000022', code: 'LAB-HDL', name: 'Cholesterol HDL', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 75000, room: 'Phòng 102 - Sinh hóa', description: 'Đánh giá nguy cơ tim mạch.' },
  { id: 'f0000000-0000-0000-0000-000000000023', code: 'LAB-URIC', name: 'Acid uric máu', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 80000, room: 'Phòng 102 - Sinh hóa', description: 'Hỗ trợ chẩn đoán bệnh gout.' },
  { id: 'f0000000-0000-0000-0000-000000000024', code: 'LAB-CRP', name: 'Protein C phản ứng định lượng (CRP)', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 150000, room: 'Phòng 102 - Sinh hóa', description: 'Đánh giá tình trạng viêm cấp.' },
  { id: 'f0000000-0000-0000-0000-000000000025', code: 'LAB-HBSAG', name: 'Xét nghiệm HBsAg', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 110000, room: 'Phòng Vi sinh', description: 'Sàng lọc viêm gan B.' },
  { id: 'f0000000-0000-0000-0000-000000000026', code: 'LAB-ANTI-HCV', name: 'Xét nghiệm kháng thể viêm gan C', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 130000, room: 'Phòng Vi sinh', description: 'Sàng lọc viêm gan C.' },
  { id: 'f0000000-0000-0000-0000-000000000027', code: 'LAB-HIV', name: 'Xét nghiệm HIV', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 120000, room: 'Phòng Vi sinh', description: 'Sàng lọc nhiễm HIV.' },
  { id: 'f0000000-0000-0000-0000-000000000028', code: 'LAB-URINE', name: 'Tổng phân tích nước tiểu (10 thông số)', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 80000, room: 'Phòng 102 - Sinh hóa', description: 'Đánh giá chỉ số hóa sinh và cặn lắng nước tiểu.' },
  { id: 'f0000000-0000-0000-0000-000000000029', code: 'LAB-STOOL', name: 'Xét nghiệm phân', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 70000, room: 'Phòng Vi sinh', description: 'Xét nghiệm ký sinh trùng, máu ẩn và các chỉ số phân.' },

  // Diagnostic Imaging (IMAGING)
  { id: 'f0000000-0000-0000-0000-000000000030', code: 'IMG-CXR', name: 'X-quang ngực thẳng', category: 'IMAGING', categoryName: 'Chẩn đoán hình ảnh', price: 150000, room: 'Phòng X-Quang 01', description: 'Chụp X-quang tim phổi tư thế thẳng.' },
  { id: 'f0000000-0000-0000-0000-000000000031', code: 'IMG-AXR', name: 'X-quang bụng không chuẩn bị', category: 'IMAGING', categoryName: 'Chẩn đoán hình ảnh', price: 160000, room: 'Phòng X-Quang 01', description: 'Khảo sát ổ bụng không dùng thuốc cản quang.' },
  { id: 'f0000000-0000-0000-0000-000000000032', code: 'IMG-US-ABD', name: 'Siêu âm bụng tổng quát', category: 'IMAGING', categoryName: 'Chẩn đoán hình ảnh', price: 220000, room: 'Phòng Siêu âm 02', description: 'Khảo sát gan mật tụy lách thận và ổ bụng.' },
  { id: 'f0000000-0000-0000-0000-000000000033', code: 'IMG-US-THY', name: 'Siêu âm tuyến giáp', category: 'IMAGING', categoryName: 'Chẩn đoán hình ảnh', price: 200000, room: 'Phòng Siêu âm 02', description: 'Khảo sát cấu trúc và nhân tuyến giáp.' },
  { id: 'f0000000-0000-0000-0000-000000000034', code: 'IMG-CT-ABD', name: 'CT scan bụng', category: 'IMAGING', categoryName: 'Chẩn đoán hình ảnh', price: 1200000, room: 'Phòng CT-Scanner', description: 'Chụp cắt lớp vi tính ổ bụng.' },
  { id: 'f0000000-0000-0000-0000-000000000035', code: 'IMG-MRI-BRAIN', name: 'MRI sọ não', category: 'IMAGING', categoryName: 'Chẩn đoán hình ảnh', price: 2200000, room: 'Phòng MRI', description: 'Chụp cộng hưởng từ sọ não.' },
  { id: 'f0000000-0000-0000-0000-000000000036', code: 'IMG-MAMMO', name: 'Chụp X-quang tuyến vú', category: 'IMAGING', categoryName: 'Chẩn đoán hình ảnh', price: 300000, room: 'Phòng X-Quang 02', description: 'Sàng lọc và chẩn đoán bệnh lý tuyến vú.' },

  // Functional Investigations (FUNCTIONAL)
  { id: 'f0000000-0000-0000-0000-000000000037', code: 'OTH-ECG', name: 'Điện tâm đồ (ECG 12 chuyển đạo)', category: 'FUNCTIONAL', categoryName: 'Thăm dò chức năng', price: 100000, room: 'Phòng Điện tim', description: 'Ghi nhận hoạt động điện học của tim.' },
  { id: 'f0000000-0000-0000-0000-000000000038', code: 'OTH-ECHO', name: 'Siêu âm tim Doppler', category: 'FUNCTIONAL', categoryName: 'Thăm dò chức năng', price: 350000, room: 'Phòng Siêu âm tim', description: 'Đánh giá cấu trúc và chức năng tim.' },
  { id: 'f0000000-0000-0000-0000-000000000039', code: 'OTH-SPIRO', name: 'Đo chức năng hô hấp', category: 'FUNCTIONAL', categoryName: 'Thăm dò chức năng', price: 200000, room: 'Phòng Thăm dò hô hấp', description: 'Đánh giá thông khí phổi bằng hô hấp ký.' },
  { id: 'f0000000-0000-0000-0000-000000000040', code: 'OTH-ENDO-GI', name: 'Nội soi dạ dày tá tràng', category: 'FUNCTIONAL', categoryName: 'Thăm dò chức năng', price: 850000, room: 'Phòng Nội soi 01', description: 'Nội soi khảo sát thực quản, dạ dày và tá tràng.' },
  { id: 'f0000000-0000-0000-0000-000000000041', code: 'OTH-ENDO-COLON', name: 'Nội soi đại tràng', category: 'FUNCTIONAL', categoryName: 'Thăm dò chức năng', price: 1100000, room: 'Phòng Nội soi 02', description: 'Nội soi khảo sát đại tràng.' },
  { id: 'f0000000-0000-0000-0000-000000000042', code: 'OTH-HOLTER', name: 'Holter điện tâm đồ 24 giờ', category: 'FUNCTIONAL', categoryName: 'Thăm dò chức năng', price: 600000, room: 'Phòng Điện tim', description: 'Theo dõi điện tâm đồ liên tục trong 24 giờ.' },
  { id: 'f0000000-0000-0000-0000-000000000043', code: 'OTH-ABPM', name: 'Đo huyết áp lưu động 24 giờ', category: 'FUNCTIONAL', categoryName: 'Thăm dò chức năng', price: 500000, room: 'Phòng Điện tim', description: 'Theo dõi huyết áp liên tục trong 24 giờ.' },
  { id: 'f0000000-0000-0000-0000-000000000044', code: 'OTH-DEXA', name: 'Đo mật độ xương DEXA', category: 'FUNCTIONAL', categoryName: 'Thăm dò chức năng', price: 400000, room: 'Phòng Đo mật độ xương', description: 'Đánh giá mật độ khoáng của xương.' },
]

export const demoClinicalOrders = [
  {
    id: 'ord-101',
    orderCode: 'CD-20260730-001',
    patientId: 'p1',
    patientCode: 'BN0001',
    patientName: 'Nguyễn Văn An',
    gender: 'Nam',
    age: 36,
    phone: '0912345678',
    doctorId: 'u3',
    doctorName: 'BS. Phạm Hồng Anh',
    department: 'Khoa Nội tổng quát',
    diagnosis: 'Tăng huyết áp vô căn, nghi ngờ đái tháo đường',
    priority: 'URGENT', // URGENT | NORMAL
    status: 'IN_PROGRESS', // PENDING | IN_PROGRESS | RESULTED | COMPLETED | CANCELLED
    items: [
      { serviceId: 'f0000000-0000-0000-0000-000000000010', serviceCode: 'LAB-CBC', serviceName: 'Công thức máu toàn bộ', categoryName: 'Xét nghiệm', price: 120000, quantity: 1, note: 'Kiểm tra bạch cầu', status: 'COMPLETED' },
      { serviceId: 'f0000000-0000-0000-0000-000000000015', serviceCode: 'LAB-UREA', serviceName: 'Định lượng Ure máu', categoryName: 'Xét nghiệm', price: 60000, quantity: 1, note: 'Xét nghiệm lúc đói', status: 'IN_PROGRESS' },
      { serviceId: 'f0000000-0000-0000-0000-000000000030', serviceCode: 'IMG-CXR', serviceName: 'X-quang ngực thẳng', categoryName: 'Chẩn đoán hình ảnh', price: 150000, quantity: 1, note: 'Chụp đứng', status: 'PENDING' },
    ],
    totalAmount: 520000,
    createdAt: '2026-07-30T07:30:00',
    updatedAt: '2026-07-30T08:00:00',
    resultSummary: 'Đã hoàn tất XN máu ngoại vi, bạch cầu trong giới hạn bình thường.',
  },
  {
    id: 'ord-102',
    orderCode: 'CD-20260730-002',
    patientId: 'p2',
    patientCode: 'BN0002',
    patientName: 'Trần Thị Bình',
    gender: 'Nữ',
    age: 48,
    phone: '0903112233',
    doctorId: 'u3',
    doctorName: 'BS. Phạm Hồng Anh',
    department: 'Khoa Tim mạch',
    diagnosis: 'Đau ngực trái chu kỳ, theo dõi bệnh mạch vành',
    priority: 'NORMAL',
    status: 'PENDING',
    items: [
      { serviceId: 'f0000000-0000-0000-0000-000000000032', serviceCode: 'IMG-US-ABD', serviceName: 'Siêu âm bụng tổng quát', categoryName: 'Chẩn đoán hình ảnh', price: 220000, quantity: 1, note: 'Khảo sát túi mật', status: 'PENDING' },
      { serviceId: 'f0000000-0000-0000-0000-000000000037', serviceCode: 'OTH-ECG', serviceName: 'Điện tâm đồ (ECG 12 chuyển đạo)', categoryName: 'Thăm dò chức năng', price: 100000, quantity: 1, note: 'Đo lúc nghỉ', status: 'PENDING' },
    ],
    totalAmount: 320000,
    createdAt: '2026-07-30T08:15:00',
    updatedAt: '2026-07-30T08:15:00',
    resultSummary: null,
  },
  {
    id: 'ord-103',
    orderCode: 'CD-20260729-008',
    patientId: 'p3',
    patientCode: 'BN0003',
    patientName: 'Lê Văn Cường',
    gender: 'Nam',
    age: 52,
    phone: '0988776655',
    doctorId: 'u3',
    doctorName: 'BS. Phạm Hồng Anh',
    department: 'Khoa Nội tiết',
    diagnosis: 'Theo dõi biến chứng đái tháo đường type 2',
    priority: 'NORMAL',
    status: 'RESULTED',
    items: [
      { serviceId: 'f0000000-0000-0000-0000-000000000016', serviceCode: 'LAB-CREA', serviceName: 'Định lượng Creatinin máu', categoryName: 'Xét nghiệm', price: 70000, quantity: 1, note: '', status: 'COMPLETED' },
      { serviceId: 'f0000000-0000-0000-0000-000000000014', serviceCode: 'LAB-HBA1C', serviceName: 'Định lượng HbA1c', categoryName: 'Xét nghiệm', price: 190000, quantity: 1, note: '', status: 'COMPLETED' },
    ],
    totalAmount: 440000,
    createdAt: '2026-07-29T14:20:00',
    updatedAt: '2026-07-29T16:00:00',
    resultSummary: 'HbA1c: 7.8% (Cao). Khuyến cáo điều chỉnh liều thuốc.',
  },
  {
    id: 'ord-104',
    orderCode: 'CD-20260728-003',
    patientId: 'p4',
    patientCode: 'BN0004',
    patientName: 'Phạm Minh Hoàng',
    gender: 'Nam',
    age: 29,
    phone: '0977114455',
    doctorId: 'u3',
    doctorName: 'BS. Phạm Hồng Anh',
    department: 'Khoa Ngoại thần kinh',
    diagnosis: 'Chấn thương sọ não kín sau TNGT, chấn thương phần mềm',
    priority: 'URGENT',
    status: 'COMPLETED',
    items: [
      { serviceId: 'cls-9', serviceCode: 'CDHA-04', serviceName: 'Chụp CT-Scanner sọ não 128 dãy (Có thuốc cản quang)', categoryName: 'Chẩn đoán hình ảnh', price: 1200000, quantity: 1, note: 'Khẩn cấp', status: 'COMPLETED' },
      { serviceId: 'cls-12', serviceCode: 'TDCN-02', serviceName: 'Nội soi dạ dày - thực quản bằng ống mềm (Không đau)', categoryName: 'Thăm dò chức năng', price: 850000, quantity: 1, note: 'Kiểm tra xuất huyết', status: 'COMPLETED' },
    ],
    totalAmount: 2050000,
    createdAt: '2026-07-28T10:00:00',
    updatedAt: '2026-07-28T11:45:00',
    resultSummary: 'CT Sọ não không phát hiện xuất huyết nội sọ. Nội soi niêm mạc dạ dày bình thường.',
  },
]

