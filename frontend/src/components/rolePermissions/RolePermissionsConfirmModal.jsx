import React from 'react'
import { Alert, Button, Modal, Space, Tag, Typography } from 'antd'
import { ExclamationCircleOutlined, SaveOutlined } from '@ant-design/icons'
import { getRoleDisplayName } from './rolePermissionsConstants'

const { Text, Paragraph } = Typography

function RolePermissionsConfirmModal({
  confirmModalOpen,
  targetRoleForSave,
  roleDiff,
  savingRoleId,
  onClose,
  onConfirm,
}) {
  return (
    <Modal
      title={
        <Space>
          <ExclamationCircleOutlined style={{ color: '#f59e0b' }} />
          <span>Xác nhận cập nhật phân quyền</span>
        </Space>
      }
      open={confirmModalOpen}
      onCancel={onClose}
      footer={[
        <Button key="cancel" onClick={onClose} disabled={savingRoleId !== null}>
          Hủy bỏ
        </Button>,
        <Button
          key="submit"
          type="primary"
          icon={<SaveOutlined />}
          loading={savingRoleId !== null}
          onClick={onConfirm}
          style={{ backgroundColor: '#2563eb', borderColor: '#2563eb' }}
        >
          Xác nhận lưu vào Backend
        </Button>,
      ]}
    >
      {targetRoleForSave && roleDiff && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Paragraph style={{ margin: 0 }}>
            Bạn đang lưu cấu hình quyền mới cho vai trò:{' '}
            <Text strong style={{ color: '#1d4ed8' }}>
              {getRoleDisplayName(targetRoleForSave)} ({targetRoleForSave.name})
            </Text>
            .
          </Paragraph>

          {roleDiff.addedCodes.length > 0 && (
            <div>
              <div style={{ fontSize: 12, fontWeight: 600, color: '#15803d', marginBottom: 4 }}>
                + Cấp thêm {roleDiff.addedCodes.length} quyền mới:
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, maxHeight: 110, overflowY: 'auto' }}>
                {roleDiff.addedCodes.map((c) => (
                  <Tag key={c} color="green" style={{ fontSize: 11, margin: 0 }}>
                    +{c}
                  </Tag>
                ))}
              </div>
            </div>
          )}

          {roleDiff.removedCodes.length > 0 && (
            <div>
              <div style={{ fontSize: 12, fontWeight: 600, color: '#b91c1c', marginBottom: 4 }}>
                - Thu hồi {roleDiff.removedCodes.length} quyền:
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, maxHeight: 110, overflowY: 'auto' }}>
                {roleDiff.removedCodes.map((c) => (
                  <Tag key={c} color="red" style={{ fontSize: 11, margin: 0 }}>
                    -{c}
                  </Tag>
                ))}
              </div>
            </div>
          )}

          <Alert
            type="info"
            showIcon
            message="Lưu ý sau khi cập nhật"
            description="Người dùng thuộc vai trò này cần đăng nhập lại (hoặc làm mới phiên) để Token nhận danh sách quyền mới từ Backend."
            style={{ fontSize: 12, borderRadius: 6 }}
          />
        </div>
      )}
    </Modal>
  )
}

export default RolePermissionsConfirmModal
