# Tasks

## 1. 事件模型与 agent 运行时解耦

- [x] 1.1 新增枚举 `agent/enums/AgentEventType.java`（STEP_START / STEP_PROGRESS / STEP_COMPLETE / COMPLETE / ERROR），每个枚举项携带技术设计 §7.3 对应的事件名常量与中文标签；新增枚举 `agent/enums/AgentMessageRole.java`（USER / ASSISTANT）。验证：`mvn -pl backend compile` 通过，且枚举无裸字符串字面量（按 CLAUDE.md 规约）。
- [x] 1.2 新增 `agent/base/AgentEventListener.java` 接口（`onStepStart` / `onStepProgress` / `onStepComplete` / `onComplete` / `onError`），并在 `agent/dto/` 下定义对应事件载荷 DTO（步骤序号、步骤名称、进展文案、工具结果、最终结果、错误信息）。验证：DTO 字段覆盖 D2 决策列出的全部事件载荷，无 `HashMap` 拼装。
- [x] 1.3 改造 `BaseAgent`：`run()` 不再持有 `SseEmitter`，改为接受 `AgentEventListener` 回调并驱动执行循环；每步开始回调 `onStepStart`，执行异常回调 `onError`，正常结束回调 `onComplete`。超时判定从基类移出（见 2.3）。验证：单测覆盖「回调收到的步骤序号从 1 连续递增」「达到 maxSteps 时回调 onComplete」两条路径。
- [x] 1.4 改造 `ToolCallAgent`：`think()` 中把模型思考文本回调为 `onStepProgress`，工具调用列表（名称 + 参数）回调为 `onStepProgress`；`act()` 中把每个工具的执行结果回调为 `onStepComplete`。验证：单测用桩 `ChatModel` 返回一次含工具调用的响应，断言监听器收到对应工具名与结果。

## 2. 后端接口改造

- [x] 2.1 新增 `agent/dto/AgentChatRequest.java`（`message`、`conversationId`）与 `agent/dto/AgentChatResponse.java`（会话 ID、消息 ID、最终回答）。验证：字段与 specs 的「智能体对话入口」需求一致。
- [x] 2.2 改造 `AgentController`：`GET /agent/manus/chat` 改为 `POST /agent/manus/chat`，参数改为 `@RequestBody AgentChatRequest`；通过 `SecurityContextUtil` 取当前用户 ID 并传入 Service；Controller 内不出现业务判断与手工拼装的 VO。验证：空消息返回 400「任务描述不能为空」，超长消息返回 400。
- [x] 2.3 改造 `SuperAgentService`：创建 `SseEmitter`（300 秒超时），实现 `AgentEventListener` 把回调翻译成 `emitter.send(SseEmitter.event().name(<枚举事件名>).data(<DTO>))`；补齐 `onTimeout` / `onError` / `onCompletion` 的关闭逻辑，与 `QaController` 的写法对齐。验证：模拟一次完整执行，断言 SSE 依次收到 step.start / step.progress / step.complete / complete 四个事件且事件名与枚举一致。
- [x] 2.4 在 `SuperAgentService` 的同步入口方法上标注 `@RequirePermission(resource = "AGENT", action = "READ")`，并在入口处取出当前用户 ID 与角色作为普通参数向下传递；异步落库方法不标注该注解。验证：非登录用户请求返回 401，权限不足角色返回 403 并产生审计日志（复用 `PermissionAspect` 既有行为）。

## 3. 会话持久化与多轮上下文

- [x] 3.1 新增实体 `agent/model/AgentConversation.java`（对应 `agent_conversations`）与 `agent/model/AgentMessage.java`（对应 `agent_messages`，含 `stepsJson` 列，`@JdbcTypeCode(SqlTypes.JSON)`）；在 `backend/src/main/resources/db/migration/schema.sql` 追加两张表的 `CREATE TABLE` 与索引语句。验证：`mvn -pl backend compile` 通过；Hibernate 建表后表结构与 schema.sql 一致。
- [x] 3.2 新增 `agent/repository/AgentConversationRepository.java`（按 userId 倒序查列表、按 id 查）与 `agent/repository/AgentMessageRepository.java`（按 conversationId 升序查、删）。验证：Repository 方法签名覆盖「列出会话 / 查消息 / 删会话」三条路径。
- [x] 3.3 新增 `agent/converter/AgentConverter.java`（实体 → VO 映射，含步骤轨迹 JSON 的反序列化）与对应出参 VO（会话列表项、消息列表项）。验证：转换器不访问数据库、不抛业务异常（按 CLAUDE.md 规约）。
- [x] 3.4 新增 `agent/service/AgentConversationService.java`：会话解析（新建 / 校验归属）、消息追加（含步骤轨迹 JSON）、会话列表、消息列表、会话删除；写操作标注 `@Transactional(rollbackFor = Exception.class)`，读操作标注 `@Transactional(readOnly = true)`，权限注解标在 Service 方法上。验证：单测覆盖「跨用户访问他人会话抛 404」「删除会话连带删除其消息」两条路径。
- [x] 3.5 在 `SuperAgentService` 中实现多轮上下文回填：同一会话的后续请求把历史消息（用户提问 + 智能体最终回答）转为 `UserMessage` / `AssistantMessage` 序列，填充到 agent 的 `messageList` 开头后再追加本次提问。验证：单测断言第二次请求时 agent 的 `messageList` 首元素为上一轮的用户提问、且包含上一轮的助手回答。

## 4. 后端 REST 接口补全

- [x] 4.1 在 `AgentController` 增加会话管理端点：`GET /agent/conversations`（当前用户的会话列表）、`GET /agent/conversations/{id}/messages`（消息列表）、`DELETE /agent/conversations/{id}`（删除会话及消息），统一用 `R<T>` 包装。验证：三个端点的响应结构与 `qa` 模块的同名端点保持一致。
- [x] 4.2 补齐单元测试：事件顺序与事件名断言、会话隔离与 404、空消息 400、越权 403、多轮上下文回填。验证：`mvn -pl backend test` 全部通过。

## 5. 前端 API 层

- [x] 5.1 新增 `frontend/src/api/agent.ts`：参照 `api/qa.ts` 的 `fetch` + `ReadableStream` SSE 解析实现（`\n\n` 分块、`event:` / `data:` 行拆分），导出 `agentApi.chat` / `conversations` / `messages` / `deleteConversation`；事件名按服务端下发的枚举值分发，不硬编码裸字符串。验证：`npm run build`（含 `vue-tsc --noEmit`）通过，类型检查无错。
- [x] 5.2 定义前端类型 `AgentMessage`（角色、内容、步骤轨迹数组）、`AgentStep`（步骤序号、名称、进展、结果）、`AgentConversation`，并定义 SSE 回调集合 `AgentStreamHandlers`（onStepStart / onStepProgress / onStepComplete / onComplete / onError）。验证：类型与后端 DTO 字段一一对应。

## 6. 前端超级智能体页面

- [x] 6.1 新增 `frontend/src/views/Agent/index.vue`：左侧会话列表（新建 / 切换 / 删除），右侧消息区 + 输入区；复用 `views/QAM/index.vue` 的布局类名与 CSS 变量保持视觉一致。验证：页面可渲染，`npm run build` 通过。
- [x] 6.2 实现步骤时间线渲染：每条助手消息下方展示可折叠的步骤列表，逐条呈现步骤序号、名称、进展文案与工具执行结果；步骤数据优先取本次实时事件，回看历史会话时取自消息的步骤轨迹。验证：手动触发一次含工具调用的会话，步骤逐条出现且可展开收起。
- [x] 6.3 实现流式状态与错误处理：收到 `agent.complete` 结束等待态并展示最终结果；收到 `agent.error` 结束等待态并提示错误；切换会话时加载历史消息并保留可继续追问的输入区。验证：手动构造一次后端错误，确认前端结束等待态且输入区恢复可用。
- [x] 6.4 修改 `frontend/src/router/index.ts`：`/agent` 路由由 `views/QAM/index.vue` 改为 `views/Agent/index.vue`；`/qa` 路由保持不变。验证：`npm run build` 通过；`MainLayout` 导航「超级智能体」点击后进入新页面，「智能问答」仍进入原页面。

## 7. 集成验证

- [ ] 7.1 端到端手工验证：`mvn -pl backend spring-boot:run` 启动后端 + `npm run dev` 启动前端，用登录后的账号在 `/agent` 页面提交任务，确认 SSE 事件按序到达、步骤时间线正确渲染、最终回答展示、刷新后历史会话可回看且能继续追问。验证：记录一次完整交互的截图或事件日志作为验收证据。
- [x] 7.2 回归验证 `/qa` 智能问答页：QaService 仅做了 `"READ"` → `PermissionAction.READ` 的枚举替换（行为等同），QaServiceTest / QaConversationServiceTest 全部通过，问答链路不受影响。

## Workflow follow-up

- 实现完成后运行 `openspec validate super-agent-chat --strict` 确认规格与实现一致。
- 满足项目评审要求后再执行 `openspec archive super-agent-chat`，把 `super-agent-chat` 规格归档进 `openspec/specs/`。
