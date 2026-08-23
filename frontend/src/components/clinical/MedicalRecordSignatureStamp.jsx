import React from 'react'
import { Card, Tag, Typography, Space } from 'antd'
import {
  SafetyCertificateFilled,
  CheckCircleFilled,
  LockFilled,
  FileProtectOutlined,
} from '@ant-design/icons'
import { parseSignatureData } from '../../utils/medicalRecordSignHelpers'

const { Text } = Typography

/**
 * Component hiển thị Con dấu Ký số Điện tử Bệnh án (Medical Record E-Signature Stamp)
 */
export default function MedicalRecordSignatureStamp({
  signatureData,
  signedAt,
  signedBy,
  doctorName,
  status = 'SIGNED',
  style = {},
  compact = false,
}) {
  const sigInfo = parseSignatureData(signatureData, signedAt, doctorName)

  if (compact) {
    return (
      <div
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 8,
          background: 'linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%)',
          border: '1px solid #86efac',
          borderRadius: 6,
          padding: '6px 12px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
          ...style,
        }}
      >
        <SafetyCertificateFilled style={{ color: '#16a34a', fontSize: 18 }} />
        <div>
          <div style={{ fontSize: 12, fontWeight: 700, color: '#166534', lineHeight: 1.2 }}>
            ĐÃ KÝ XÁC NHẬN: {sigInfo.doctorName}
          </div>
          <div style={{ fontSize: 11, color: '#15803d' }}>
            {sigInfo.signedAt || 'Đã xác thực điện tử'} {sigInfo.certHash ? `• [${sigInfo.certHash}]` : ''}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div
      style={{
        background: 'linear-gradient(135deg, #f8fafc 0%, #f0fdf4 100%)',
        border: '2px dashed #22c55e',
        borderRadius: 12,
        padding: '16px 20px',
        position: 'relative',
        overflow: 'hidden',
        boxShadow: '0 4px 12px rgba(34, 197, 94, 0.08)',
        ...style,
      }}
    >
      {/* Background Watermark */}
      <div
        style={{
          position: 'absolute',
          right: -15,
          bottom: -20,
          opacity: 0.08,
          pointerEvents: 'none',
        }}
      >
        <FileProtectOutlined style={{ fontSize: 140, color: '#16a34a' }} />
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <div
            style={{
              width: 48,
              height: 48,
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #22c55e 0%, #15803d 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 10px rgba(22, 163, 74, 0.3)',
              color: '#fff',
              fontSize: 24,
            }}
          >
            <SafetyCertificateFilled />
          </div>

          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ fontSize: 15, fontWeight: 800, color: '#166534', letterSpacing: 0.5 }}>
                BỆNH ÁN ĐÃ KÝ XÁC NHẬN ĐIỆN TỬ
              </span>
              <Tag color="success" icon={<CheckCircleFilled />}>
                HỢP LỆ & KHÓA NỘI DUNG
              </Tag>
            </div>
            <div style={{ fontSize: 13, color: '#334155', marginTop: 2 }}>
              Người ký: <Text strong style={{ color: '#0f172a' }}>{sigInfo.doctorName}</Text> (Bác sĩ phụ trách)
            </div>
            <div style={{ fontSize: 12, color: '#64748b', marginTop: 1 }}>
              Thời gian ký: <Text strong style={{ color: '#166534' }}>{sigInfo.signedAt || '—'}</Text>
              {sigInfo.certHash && (
                <span style={{ marginLeft: 8 }}>
                  • Mã chứng thực: <Text code style={{ fontSize: 11 }}>{sigInfo.certHash}</Text>
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Chữ ký tay nếu có hoặc Seal Badge */}
        {sigInfo.drawing ? (
          <div style={{ textAlign: 'center', minWidth: 140 }}>
            <div style={{ fontSize: 11, color: '#64748b', marginBottom: 2 }}>Chữ ký số bác sĩ:</div>
            <div
              style={{
                background: '#fff',
                padding: '4px 8px',
                borderRadius: 6,
                border: '1px solid #cbd5e1',
                display: 'inline-block',
              }}
            >
              <img
                src={sigInfo.drawing}
                alt="Chữ ký bác sĩ"
                style={{ height: 44, maxWidth: 160, objectFit: 'contain' }}
              />
            </div>
          </div>
        ) : (
          <div
            style={{
              border: '2px solid #16a34a',
              borderRadius: 8,
              padding: '6px 14px',
              textAlign: 'center',
              backgroundColor: 'rgba(255,255,255,0.85)',
              transform: 'rotate(-2deg)',
              boxShadow: '0 2px 6px rgba(0,0,0,0.06)',
            }}
          >
            <div style={{ fontSize: 10, fontWeight: 700, color: '#166534', letterSpacing: 1 }}>
              PHÒNG KHÁM ĐA KHOA
            </div>
            <div style={{ fontSize: 13, fontWeight: 800, color: '#15803d' }}>
              ✓ ĐÃ XÁC THỰC KÝ SỐ
            </div>
            <div style={{ fontSize: 10, color: '#166534', fontWeight: 600 }}>
              BS. {sigInfo.doctorName}
            </div>
          </div>
        )}
      </div>

      <div
        style={{
          marginTop: 10,
          paddingTop: 8,
          borderTop: '1px solid rgba(34, 197, 94, 0.2)',
          fontSize: 12,
          color: '#15803d',
          display: 'flex',
          alignItems: 'center',
          gap: 6,
        }}
      >
        <LockFilled />
        <span>
          Hồ sơ đã được mã hóa và khóa an toàn. Mọi thay đổi đều được ghi lại trong nhật ký kiểm toán hệ thống.
        </span>
      </div>
    </div>
  )
}
