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
    patientCode: 'BN-2026001',
    fullName: 'Nguyễn Văn An',
    dateOfBirth: '1988-05-12',
    gender: 'MALE',
    phoneNumber: '0908123456',
    email: 'an.nguyen@email.com',
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
    patientCode: 'BN-2026003',
    fullName: 'Lê Minh Huy',
    dateOfBirth: '1975-01-08',
    gender: 'MALE',
    phoneNumber: '0934567890',
    email: 'huy.le@email.com',
    address: '88 Nguyễn Huệ, Quận 5, TP.HCM',
    identityNumber: '011223344556',
    healthInsuranceCode: 'BH111222333',
    bloodType: 'B+',
    emergencyContact: 'Lê Thu Hà - 0931234567',
    medicalHistory: 'Viêm khớp',
    allergies: 'Sulfonamide',
  },
]

export const demoAppointments = [
  { id: 'a1', patientId: 'p1', patientName: 'Nguyễn Văn An', doctorId: 'd1', doctorName: 'BS. Phạm Hồng Anh', slot: '08:00', date: '2026-07-15', status: 'SCHEDULED' },
  { id: 'a2', patientId: 'p2', patientName: 'Trần Thị Bình', doctorId: 'd1', doctorName: 'BS. Phạm Hồng Anh', slot: '09:30', date: '2026-07-15', status: 'CHECKED_IN' },
  { id: 'a3', patientId: 'p3', patientName: 'Lê Minh Huy', doctorId: 'd2', doctorName: 'BS. Nguyễn Minh', slot: '10:00', date: '2026-07-15', status: 'NO_SHOW' },
]

export const demoMedicalRecords = [
  {
    id: 'm1',
    recordCode: 'BA-0001',
    patientId: 'p1',
    patientName: 'Nguyễn Văn An',
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
  { id: 'cls-1', code: 'XN-01', name: 'Tổng phân tích tế bào máu ngoại vi (24 chỉ số)', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 120000, room: 'Phòng 101 - Huyết học', description: 'Đánh giá số lượng hồng cầu, bạch cầu, tiểu cầu' },
  { id: 'cls-2', code: 'XN-02', name: 'Sinh hóa máu (Glucose, Ure, Creatinin, GOT, GPT)', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 250000, room: 'Phòng 102 - Sinh hóa', description: 'Khảo sát chức năng gan, thận, đường huyết' },
  { id: 'cls-3', code: 'XN-03', name: 'Đông máu cơ bản (PT, APTT, Fibrinogen)', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 180000, room: 'Phòng 101 - Huyết học', description: 'Đánh giá chức năng đông máu toàn bộ' },
  { id: 'cls-4', code: 'XN-04', name: 'Xét nghiệm nước tiểu 10 thông số', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 80000, room: 'Phòng 102 - Sinh hóa', description: 'Phân tích tổng quát tế bào nước tiểu' },
  { id: 'cls-5', code: 'XN-05', name: 'Định lượng HbA1c (Đường huyết trung bình 3 tháng)', category: 'LABORATORY', categoryName: 'Xét nghiệm', price: 190000, room: 'Phòng 102 - Sinh hóa', description: 'Theo dõi kiểm soát đường huyết bệnh tiểu đường' },
  { id: 'cls-6', code: 'CDHA-01', name: 'Chụp X-Quang ngực thẳng (1 tư thế)', category: 'IMAGING', categoryName: 'Chẩn đoán hình ảnh', price: 150000, room: 'Phòng X-Quang 01', description: 'Khảo sát nhu mô phổi, phế quản và khung xương ngực' },
  { id: 'cls-7', code: 'CDHA-02', name: 'Siêu âm ổ bụng tổng quát (2D/4D)', category: 'IMAGING', categoryName: 'Chẩn đoán hình ảnh', price: 220000, room: 'Phòng Siêu âm 02', description: 'Khảo sát gan, mật, tụy, lách, thận, bàng quang' },
  { id: 'cls-8', code: 'CDHA-03', name: 'Siêu âm Doppler màu tim và mạch máu', category: 'IMAGING', categoryName: 'Chẩn đoán hình ảnh', price: 350000, room: 'Phòng Siêu âm 01', description: 'Khảo sát chức năng van tim và dòng máu' },
  { id: 'cls-9', code: 'CDHA-04', name: 'Chụp CT-Scanner sọ não 128 dãy (Có thuốc cản quang)', category: 'IMAGING', categoryName: 'Chẩn đoán hình ảnh', price: 1200000, room: 'Phòng CT-Scanner', description: 'Chẩn đoán tổn thương mạch máu não, u não' },
  { id: 'cls-10', code: 'CDHA-05', name: 'Chụp Cộng hưởng từ MRI cột sống thắt lưng', category: 'IMAGING', categoryName: 'Chẩn đoán hình ảnh', price: 2200000, room: 'Phòng MRI', description: 'Khảo sát đĩa đệm, rễ thần kinh cột sống' },
  { id: 'cls-11', code: 'TDCN-01', name: 'Đo điện tâm đồ (ECG 12 chuyển đạo)', category: 'FUNCTIONAL', categoryName: 'Thăm dò chức năng', price: 100000, room: 'Phòng Điện tim', description: 'Phát hiện rối loạn nhịp tim, thiếu máu cơ tim' },
  { id: 'cls-12', code: 'TDCN-02', name: 'Nội soi dạ dày - thực quản bằng ống mềm (Không đau)', category: 'FUNCTIONAL', categoryName: 'Thăm dò chức năng', price: 850000, room: 'Phòng Nội soi 01', description: 'Quan sát trực tiếp niêm mạc dạ dày, tá tràng' },
  { id: 'cls-13', code: 'TDCN-03', name: 'Đo thông khí phổi (Chức năng hô hấp)', category: 'FUNCTIONAL', categoryName: 'Thăm dò chức năng', price: 200000, room: 'Phòng Thăm dò hô hấp', description: 'Đánh giá mức độ thông khí và tắc nghẽn phế quản' },
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
      { serviceId: 'cls-1', serviceCode: 'XN-01', serviceName: 'Tổng phân tích tế bào máu ngoại vi (24 chỉ số)', categoryName: 'Xét nghiệm', price: 120000, quantity: 1, note: 'Kiểm tra bạch cầu', status: 'COMPLETED' },
      { serviceId: 'cls-2', serviceCode: 'XN-02', serviceName: 'Sinh hóa máu (Glucose, Ure, Creatinin, GOT, GPT)', categoryName: 'Xét nghiệm', price: 250000, quantity: 1, note: 'Xét nghiệm lúc đói', status: 'IN_PROGRESS' },
      { serviceId: 'cls-6', serviceCode: 'CDHA-01', serviceName: 'Chụp X-Quang ngực thẳng (1 tư thế)', categoryName: 'Chẩn đoán hình ảnh', price: 150000, quantity: 1, note: 'Chụp đứng', status: 'PENDING' },
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
      { serviceId: 'cls-7', serviceCode: 'CDHA-02', serviceName: 'Siêu âm ổ bụng tổng quát (2D/4D)', categoryName: 'Chẩn đoán hình ảnh', price: 220000, quantity: 1, note: 'Khảo sát túi mật', status: 'PENDING' },
      { serviceId: 'cls-11', serviceCode: 'TDCN-01', serviceName: 'Đo điện tâm đồ (ECG 12 chuyển đạo)', categoryName: 'Thăm dò chức năng', price: 100000, quantity: 1, note: 'Đo lúc nghỉ', status: 'PENDING' },
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
      { serviceId: 'cls-2', serviceCode: 'XN-02', serviceName: 'Sinh hóa máu (Glucose, Ure, Creatinin, GOT, GPT)', categoryName: 'Xét nghiệm', price: 250000, quantity: 1, note: '', status: 'COMPLETED' },
      { serviceId: 'cls-5', serviceCode: 'XN-05', serviceName: 'Định lượng HbA1c (Đường huyết trung bình 3 tháng)', categoryName: 'Xét nghiệm', price: 190000, quantity: 1, note: '', status: 'COMPLETED' },
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

