import React from 'react'
import { Tag, Tooltip } from 'antd'
import {
  AlertOutlined,
  FireOutlined,
  WarningOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import { getSpecialControlMeta } from '../../utils/specialControlHelpers.js'

export default function SpecialControlBadge({
  group,
  note = '',
  showTooltip = true,
  short = false,
  style = {},
}) {
  const meta = getSpecialControlMeta(group)
  if (!meta) return null

  const label = short ? meta.shortLabel : meta.label

  let icon = <WarningOutlined style={{ fontSize: 12 }} />
  if (meta.code === 'NARCOTIC') {
    icon = <AlertOutlined style={{ fontSize: 12 }} />
  } else if (meta.code === 'PSYCHOTROPIC') {
    icon = <FireOutlined style={{ fontSize: 12 }} />
  } else if (meta.code === 'COMBINED') {
    icon = <InfoCircleOutlined style={{ fontSize: 12 }} />
  }

  const tagContent = (
    <Tag
      style={{
        margin: 0,
        fontWeight: 700,
        fontSize: 11,
        padding: '2px 8px',
        borderRadius: 4,
        display: 'inline-flex',
        alignItems: 'center',
        gap: 4,
        color: meta.textColor,
        backgroundColor: meta.bgColor,
        borderColor: meta.borderColor,
        lineHeight: '18px',
        cursor: note ? 'help' : 'default',
        ...style,
      }}
    >
      {icon}
      <span>{label}</span>
    </Tag>
  )

  if (!showTooltip || !note) {
    return tagContent
  }

  return (
    <Tooltip
      title={
        <div style={{ maxWidth: 280 }}>
          <div style={{ fontWeight: 700, marginBottom: 4 }}>{meta.label}</div>
          <div style={{ fontSize: 12, opacity: 0.9 }}>{note}</div>
        </div>
      }
    >
      {tagContent}
    </Tooltip>
  )
}
