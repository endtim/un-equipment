import request from '../utils/request'

export function getOperationLogs(params) {
  return request({
    url: '/api/admin/logs',
    method: 'get',
    params
  })
}
