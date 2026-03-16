import request from '../utils/request'

export function getOverview(params) {
  return request({
    url: '/api/stats/overview',
    method: 'get',
    params
  })
}

export function getPlatformMembers() {
  return request({
    url: '/api/stats/platform-members',
    method: 'get'
  })
}

export function getAdminOverview(params) {
  return request({
    url: '/api/admin/stats/overview',
    method: 'get',
    params
  })
}
