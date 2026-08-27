import {
  MedicineBoxOutlined,
  HeartOutlined,
  ExperimentOutlined,
  SoundOutlined,
  HeartFilled,
  EyeOutlined,
  SmileOutlined,
} from '@ant-design/icons'

export const SPECIALTY_COLOR_PALETTE = {
  GENERAL: {
    bg: '#E6F1FB',
    text: '#0C447C',
    border: '#B5D7F8',
    IconComponent: MedicineBoxOutlined,
  },
  INTERNAL_MEDICINE: {
    bg: '#EEEDFE',
    text: '#3C3489',
    border: '#CECBFD',
    IconComponent: HeartOutlined,
  },
  SURGERY: {
    bg: '#FAECE7',
    text: '#712B13',
    border: '#F4C4B4',
    IconComponent: ExperimentOutlined,
  },
  ENT: {
    bg: '#E1F5EE',
    text: '#085041',
    border: '#A6E3D0',
    IconComponent: SoundOutlined,
  },
  CARDIOLOGY: {
    bg: '#FBEAF0',
    text: '#72243E',
    border: '#F4B8CD',
    IconComponent: HeartFilled,
  },
  OPHTHALMOLOGY: {
    bg: '#E0F2FE',
    text: '#0369A1',
    border: '#BAE6FD',
    IconComponent: EyeOutlined,
  },
  DERMATOLOGY: {
    bg: '#FEF3C7',
    text: '#92400E',
    border: '#FDE68A',
    IconComponent: SmileOutlined,
  },
}

export const FALLBACK_COLOR_PALETTES = [
  { bg: '#E6F1FB', text: '#0C447C', border: '#B5D7F8', IconComponent: MedicineBoxOutlined },
  { bg: '#EEEDFE', text: '#3C3489', border: '#CECBFD', IconComponent: HeartOutlined },
  { bg: '#FAECE7', text: '#712B13', border: '#F4C4B4', IconComponent: ExperimentOutlined },
  { bg: '#E1F5EE', text: '#085041', border: '#A6E3D0', IconComponent: SoundOutlined },
  { bg: '#FBEAF0', text: '#72243E', border: '#F4B8CD', IconComponent: HeartFilled },
]

export const getSpecialtyTheme = (code, index = 0) => {
  const normalized = String(code || '').toUpperCase()
  if (SPECIALTY_COLOR_PALETTE[normalized]) {
    return SPECIALTY_COLOR_PALETTE[normalized]
  }
  return FALLBACK_COLOR_PALETTES[index % FALLBACK_COLOR_PALETTES.length]
}
