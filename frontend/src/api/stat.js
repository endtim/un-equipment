import request from '../utils/request'

export function getOverview() {
  return request({
    url: '/api/stats/overview',
    method: 'get'
  })
}
