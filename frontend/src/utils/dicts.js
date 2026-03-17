const ORDER_TYPE_LABELS = {
  MACHINE: '上机预约',
  SAMPLE: '送样预约'
}

const ORDER_STATUS_LABELS = {
  PENDING_AUDIT: '待审核',
  WAITING_USE: '待使用',
  IN_USE: '使用中',
  WAITING_RECEIVE: '待接样',
  TESTING: '检测中',
  RESULT_UPLOADED: '结果已上传（待结算）',
  WAITING_SETTLEMENT: '待结算',
  SETTLING: '结算中',
  COMPLETED: '已完成',
  REJECTED: '已驳回',
  CANCELED: '已取消'
}

const ORDER_STATUS_TAGS = {
  PENDING_AUDIT: 'warning',
  WAITING_USE: 'info',
  IN_USE: 'primary',
  WAITING_RECEIVE: 'info',
  TESTING: 'primary',
  RESULT_UPLOADED: 'success',
  WAITING_SETTLEMENT: 'warning',
  SETTLING: 'warning',
  COMPLETED: 'success',
  REJECTED: 'danger',
  CANCELED: 'info'
}

const AUDIT_RESULT_LABELS = {
  PENDING: '待审核',
  PASS: '通过',
  REJECT: '驳回'
}

const SAMPLE_TESTING_STATUS_LABELS = {
  WAITING_RECEIVE: '待接样',
  TESTING: '检测中',
  RESULT_UPLOADED: '结果已上传',
  COMPLETED: '已完成'
}

const USER_ROLE_LABELS = {
  ADMIN: '平台管理员',
  INSTRUMENT_OWNER: '仪器负责人',
  DEPT_MANAGER: '部门管理员',
  INTERNAL_USER: '校内用户',
  EXTERNAL_USER: '校外用户'
}

const TXN_TYPE_LABELS = {
  RECHARGE: '充值',
  CONSUME: '消费',
  REFUND: '退款',
  SETTLEMENT: '结算'
}

const INOUT_TYPE_LABELS = {
  IN: '收入',
  OUT: '支出'
}

const RECHARGE_STATUS_LABELS = {
  PENDING: '待审核',
  REVIEW_PENDING: '待复核',
  PASS: '已通过',
  REJECT: '已驳回',
  REJECTED: '已驳回'
}

const RECHARGE_STATUS_TAGS = {
  PENDING: 'warning',
  REVIEW_PENDING: 'warning',
  PASS: 'success',
  REJECT: 'danger',
  REJECTED: 'danger'
}

const INSTRUMENT_STATUS_LABELS = {
  NORMAL: '正常',
  DISABLED: '禁用',
  MAINTENANCE: '维护中',
  FAULT: '故障'
}

const INSTRUMENT_STATUS_TAGS = {
  NORMAL: 'success',
  DISABLED: 'info',
  MAINTENANCE: 'warning',
  FAULT: 'danger'
}

const OPEN_MODE_LABELS = {
  MACHINE: '仅支持上机',
  SAMPLE: '仅支持送样',
  BOTH: '支持上机/送样'
}

const BOOKING_UNIT_LABELS = {
  HOUR: '小时',
  ITEM: '次',
  SAMPLE: '次',
  PROJECT: '项目'
}

const PAY_STATUS_LABELS = {
  UNPAID: '未支付',
  PAID: '已支付',
  REFUNDED: '已退款'
}

const BILL_TYPE_LABELS = {
  INTERNAL: '校内结算',
  EXTERNAL: '校外结算'
}

const SETTLEMENT_STATUS_LABELS = {
  PENDING: '待结算',
  CONFIRMED: '已结算',
  REFUND_PENDING: '退款待审批',
  REFUNDING: '退款处理中',
  REFUNDED: '已退款',
  VOID: '已作废'
}

function fromMap(mapping, value, fallback = '-') {
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  return mapping[value] || value
}

export function orderTypeLabel(value) {
  return fromMap(ORDER_TYPE_LABELS, value)
}

export function orderStatusLabel(value) {
  return fromMap(ORDER_STATUS_LABELS, value)
}

export function orderStatusTagType(value) {
  return ORDER_STATUS_TAGS[value] || 'info'
}

export function auditResultLabel(value) {
  return fromMap(AUDIT_RESULT_LABELS, value)
}

export function sampleTestingStatusLabel(value) {
  return fromMap(SAMPLE_TESTING_STATUS_LABELS, value)
}

export function userRoleLabel(value) {
  return fromMap(USER_ROLE_LABELS, value)
}

export function txnTypeLabel(value) {
  return fromMap(TXN_TYPE_LABELS, value)
}

export function inoutTypeLabel(value) {
  return fromMap(INOUT_TYPE_LABELS, value)
}

export function rechargeStatusLabel(value) {
  return fromMap(RECHARGE_STATUS_LABELS, value)
}

export function rechargeStatusTagType(value) {
  return RECHARGE_STATUS_TAGS[value] || 'info'
}

export function instrumentStatusLabel(value) {
  return fromMap(INSTRUMENT_STATUS_LABELS, value)
}

export function instrumentStatusTagType(value) {
  return INSTRUMENT_STATUS_TAGS[value] || 'info'
}

export function openModeLabel(value) {
  return fromMap(OPEN_MODE_LABELS, value, '支持预约')
}

export function bookingUnitLabel(value) {
  return fromMap(BOOKING_UNIT_LABELS, value, '单位')
}

export function payStatusLabel(value) {
  return fromMap(PAY_STATUS_LABELS, value)
}

export function billTypeLabel(value) {
  return fromMap(BILL_TYPE_LABELS, value)
}

export function settlementStatusLabel(value) {
  return fromMap(SETTLEMENT_STATUS_LABELS, value)
}
