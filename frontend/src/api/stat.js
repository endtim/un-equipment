import request from '../utils/request'

export function getOverview(params) {
  return request({
    url: '/api/stats/overview',
    method: 'get',
    params
  })
}
