// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 内部聊天接口 POST /internal/chat/completions */
export async function chatCompletions1(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.chatCompletions1Params,
  body: API.ChatRequest,
  options?: { [key: string]: any }
) {
  return request<Record<string, any>>('/internal/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    params: {
      ...params,
    },
    data: body,
    ...(options || {}),
  })
}
