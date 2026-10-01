# ReadTrack API

基础地址：`http://localhost:8080`。请求体统一使用 `Content-Type: application/json`。

成功和失败统一为：

```json
{"message":"操作说明","data":null}
```

`data` 在有返回值时为对象。注册和添加书籍返回 HTTP 201，其余成功返回 200。参数错误 400、未登录或登录失败 401、书籍不存在/无权访问 404、重复用户名 409、非预期服务错误 500。

## 注册和登录

`POST /user/register` 与 `POST /user/login`：

```json
{"username":"tom","password":"123456"}
```

成功：

```json
{"message":"登录成功","data":{"id":1,"username":"tom"}}
```

登录时响应头包含 `Set-Cookie: JSESSIONID=...`。后续请求必须携带该 Cookie；IDEA HTTP Client、Postman、Apifox通常可以自动保存和发送。不同用户测试时使用独立 Cookie 会话。密码不出现在响应中。

`GET /user/me` 查询会话用户；`POST /user/logout` 使当前会话失效。

## 添加书籍

`POST /books`（需登录）：

```json
{"title":"Java 入门","author":"示例作者","totalPages":100}
```

成功示例（ID 和时间由服务生成）：

```json
{"message":"添加成功","data":{"id":1,"title":"Java 入门","author":"示例作者","totalPages":100,"readPages":0,"status":"UNREAD","userId":1,"createdAt":"2026-10-01T12:00:00"}}
```

`author` 可省略，书名不能为空，所有 JSON 字段使用 camelCase；数据库列采用 snake_case。

## 阅读进度

`PUT /books/1/progress`：

```json
{"readPages":25}
```

返回更新后的完整书籍。0 页为 `UNREAD`，达到总页数为 `READ`，中间为 `READING`。负数、超出总页数或缺少参数返回 400。

## 详情与删除

- `GET /books/1`：返回完整书籍。
- `DELETE /books/1`：返回 `{"message":"删除成功","data":null}`。
- 删除后再查返回 404；其他用户无法查询、删除或修改该书。

## 分页与搜索

`GET /books?page=1&size=10`：

```json
{"message":"查询成功","data":{"items":[],"total":0,"page":1,"size":10}}
```

`items` 包含完整书籍对象；`total` 是当前用户匹配书籍总量，而不是本页条数。无数据时返回空列表。按 `created_at DESC, id DESC` 排序。

`GET /books/search?keyword=Java&page=1&size=10` 返回同样结构，查询条件为书名包含关键词。关键词可省略，最多 200 字；URL 中中文和特殊字符须编码。

## 阅读统计

`GET /books/stats`：

```json
{"message":"查询成功","data":{"total":3,"unread":1,"reading":1,"read":1}}
```

只统计当前会话用户的数据。

## PowerShell 测试

以下命令为 Windows PowerShell 5.1/PowerShell 7 通用写法。`-WebSession` 让请求带上登录 Cookie。

```powershell
$base = 'http://localhost:8080'
$account = @{ username = 'tom'; password = '123456' } | ConvertTo-Json
Invoke-RestMethod "$base/user/register" -Method Post -ContentType 'application/json' -Body $account
Invoke-RestMethod "$base/user/login" -Method Post -ContentType 'application/json' -Body $account -SessionVariable login
$body = @{ title = 'Java'; author = 'Example'; totalPages = 100 } | ConvertTo-Json
$book = Invoke-RestMethod "$base/books" -Method Post -ContentType 'application/json' -Body $body -WebSession $login
$id = $book.data.id
Invoke-RestMethod "$base/books/$id/progress" -Method Put -ContentType 'application/json' -Body '{"readPages":100}' -WebSession $login
Invoke-RestMethod "$base/books?page=1&size=10" -WebSession $login
Invoke-RestMethod "$base/books/stats" -WebSession $login
Invoke-RestMethod "$base/books/$id" -Method Delete -WebSession $login
```
