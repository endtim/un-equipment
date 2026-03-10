import request from '../utils/request'

export function getMyAccount() {
  return request({
    url: '/api/account/me',
    method: 'get'
  })
}

export function submitRecharge(data) {
  return request({
    url: '/api/recharges',
    method: 'post',
    data
  })
}

export function getRechargeOrders() {
  return request({
    url: '/api/admin/finance/recharges',
    method: 'get'
  })
}

export function auditRecharge(id, data) {
  return request({
    url: `/api/admin/finance/recharges/${id}/audit`,
    method: 'post',
    data
  })
}
