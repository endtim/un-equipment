import request from '../utils/request'

export function getAdminSettlements(params) {
  return request({
    url: '/api/admin/settlements',
    method: 'get',
    params
  })
}

export function getAdminSettlementDetail(id) {
  return request({
    url: `/api/admin/settlements/${id}`,
    method: 'get'
  })
}

export function settleAdminSettlement(id) {
  return request({
    url: `/api/admin/settlements/${id}/settle`,
    method: 'post'
  })
}

export function refundAdminSettlement(id, data) {
  return request({
    url: `/api/admin/settlements/${id}/refund`,
    method: 'post',
    data
  })
}

export function requestRefundAdminSettlement(id, data) {
  return request({
    url: `/api/admin/settlements/${id}/refund-request`,
    method: 'post',
    data
  })
}
