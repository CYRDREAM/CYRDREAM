# 基础版接口

地址：`http://localhost:8080`。POST/PUT 使用 JSON 请求体和 `Content-Type: application/json`。

| 方法 | 路径 | 请求 | 结果 |
|---|---|---|---|
| GET | `/hello` | 无 | Hello JSON |
| POST | `/user/register` | username、password | 新用户 id 和 username，201 |
| POST | `/user/login` | username、password | 验证结果，200 |
| POST | `/books` | title、author（可选）、totalPages | 添加后的书籍，201 |
| PUT | `/books/{id}/progress` | readPages | 更新后的书籍，200 |
| DELETE | `/books/{id}` | 无 | 删除结果，200 |
| GET | `/books/{id}` | 无 | 书籍详情，200 |
| GET | `/books?page=1&size=10` | 查询参数 | items、total、page、size，200 |

用户名最长 50，密码最长 255，二者不能空白。用户名去除首尾空白、不能重复。

注册/登录请求：

```json
{"username":"tom","password":"123456"}
```

成功示例：

```json
{"message":"注册成功","data":{"id":1,"username":"tom"}}
```

没有 Cookie、Token 或会话。登录只是校验，书籍请求始终模拟 `CURRENT_USER_ID`。

添加书籍：

```json
{"title":"Java 入门","author":"示例作者","totalPages":100}
```

更新进度：

```json
{"readPages":25}
```

书籍返回字段：id、title、author、totalPages、readPages、status、userId、createdAt。初始 readPages=0、status=UNREAD。0 页为 UNREAD，全部读完为 READ，中间为 READING。

分页从 1 开始，每页 1–100 条；按创建时间倒序，同时间再按 id 倒序。total 是该用户书籍总数，items 是当前页列表。

常见错误：参数不合法 400、登录密码不匹配 401、重复用户名 409、书籍不存在或属于他人 404。错误采用 Spring Boot 默认格式，与成功响应结构不同；业务错误的 message 包含原因。

新数据库需先注册。若当前模拟用户不存在，添加书籍返回 400，并提示配置 CURRENT_USER_ID。

权限测试可在测试数据库创建第二个用户和属于该用户的书，然后用该书 ID 请求详情、进度和删除，均应返回 404。注册第二个用户并登录不会切换模拟用户，这是 Phase 3 的限制。
