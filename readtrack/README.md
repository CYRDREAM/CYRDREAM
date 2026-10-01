# ReadTrack · 2026 SAST 后端组免试题

Java 17 + Spring Boot 4.1.1 + MyBatis 4.1.0 + MySQL 8.4 的阅读管理后端。实现全部必做接口，以及 Session 登录态、BCrypt 密码哈希、书名搜索、阅读统计、全局异常处理。项目使用 AI 辅助实现，提交者需要理解代码并能解释设计。

## 快速开始

在 IDEA 中打开本目录的 `pom.xml`，作为 Maven 项目加载。使用 JDK 17、21 或 25；项目编译目标为 Java 17。

### MySQL 模式（默认，正式验收）

1. 启动 MySQL 8.4，执行 `sql/create-database.sql` 创建 `readtrack` 数据库。
2. 在 IDEA 的 `ReadtrackApplication` 运行配置中设置环境变量 `MYSQL_PASSWORD` 为本机数据库密码。默认用户名为 `root`，也可用 `MYSQL_USER` 指定已授权的专用账号。
3. 运行 `ReadtrackApplication`。应用自动创建 `rt_users` 和 `rt_books`，不会删除已有数据。表名前缀避免与早期练习的 `users` 表混用，旧的明文账号不会自动迁移。
4. 访问 `http://localhost:8080/hello`，应返回 `{"message":"Hello, World!"}`。

也可以在 PowerShell 中运行（先配置 `JAVA_HOME`）：

```powershell
$env:MYSQL_PASSWORD = '替换为你自己的数据库密码'
.\mvnw.cmd spring-boot:run
```

环境变量：

| 名称 | 默认值 / 用途 |
|---|---|
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/readtrack?connectionTimeZone=Asia/Shanghai` |
| `MYSQL_USER` | `root` |
| `MYSQL_PASSWORD` | 无预置密码，需要自行设置 |
| `PORT` | `8080` |
| `COOKIE_SECURE` | `false`；通过 HTTPS 部署时设为 `true` |

### 无需配置数据库的体验模式

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
```

使用 H2 文件数据库，数据保存在本项目 `data/`，重启后保留。H2 只是快速体验方式；默认配置及实际兼容性验收均使用 MySQL。未开放 H2 管理控制台。

### 测试与打包

```powershell
.\mvnw.cmd verify
java -jar target/readtrack-1.0.0.jar --spring.profiles.active=demo
```

`verify` 会自动启动随机端口的 HTTP 服务，默认使用独立 H2 内存数据库运行 3 个端到端测试方法，再打包可运行 JAR。不会依赖本机的 root 密码。

对独立 MySQL 测试库运行同一套测试：

```powershell
$env:TEST_DB_URL = 'jdbc:mysql://localhost:3306/readtrack_test'
$env:TEST_DB_USER = '你的测试账号'
$env:TEST_DB_PASSWORD = '测试数据库密码'
$env:TEST_DB_DRIVER = 'com.mysql.cj.jdbc.Driver'
.\mvnw.cmd verify
```

先创建 `readtrack_test` 并授权。测试会创建随机测试账号和书籍，请勿指向生产数据库。

## 接口

详细请求、响应和错误见 [API.md](docs/API.md)。可在 IDEA 中打开 [requests.http](requests.http)，依次点击请求旁的运行按钮；也可将请求填入 Apifox/Postman，开启 Cookie 保存。

| 方法 | 路径 | 功能 |
|---|---|---|
| GET | `/hello` | 入门接口 |
| POST | `/user/register` | 注册，不自动登录 |
| POST | `/user/login` | 登录，返回 Session Cookie |
| GET | `/user/me` | 当前用户 |
| POST | `/user/logout` | 注销会话 |
| POST | `/books` | 添加书籍 |
| PUT | `/books/{id}/progress` | 更新进度，自动计算状态 |
| DELETE | `/books/{id}` | 删除自己的书籍 |
| GET | `/books/{id}` | 获取自己的书籍 |
| GET | `/books?page=1&size=10` | 分页，创建时间和 ID 倒序 |
| GET | `/books/search?keyword=Java` | 按书名搜索自己的书籍 |
| GET | `/books/stats` | 当前用户阅读统计 |

## 设计与学习顺序

```text
请求 → Controller → Service → Mapper → 数据库
         DTO校验     业务规则     参数化SQL
```

- `dto/Requests`：区分账号、添加书籍、修改进度三种输入，限制空白、长度、页数。
- `controller`：接收请求，读取 Session 身份，返回统一 JSON，不直接编写 SQL。
- `service`：密码处理、重复注册、归属校验、状态计算、事务。
- `mapper`：实体 + 接口 + MyBatis 注解 SQL。所有单本书 SQL 都同时限制 `id` 和 `user_id`。
- `config`：拦截未登录的书籍请求，返回 401。
- `common`：统一错误结构，隐藏数据库异常细节。
- `model`：用户和书籍实体；密码字段不会通过用户接口返回。

先阅读 `UserController → UserService → UserMapper`，再阅读书籍对应三层。面试前应能解释：为什么数据库必须有唯一约束、为何从 Session 取 userId、为什么 BCrypt 不能解密、分页如何计算 offset、为何进度与状态在同一 SQL 更新、为什么不直接返回 User 实体。

## 业务边界

- 用户名去除首尾空白，长度 1–50；大小写匹配遵循数据库排序规则。
- 密码不可空白，BCrypt 输入最多 72 个 UTF-8 字节（不是 72 个汉字）。
- `totalPages > 0`，`0 <= readPages <= totalPages`；允许向前或向后调整进度。
- 状态规则：0 → UNREAD；总页数 → READ；其余 → READING。
- 当前身份只来自 Session，请求体中伪造 userId 不生效。
- 未登录返回 401；他人书籍和不存在的书籍均返回 404。
- Session 空闲 30 分钟过期；应用重启会使当前登录失效，重新登录即可。Cookie 为 HttpOnly、SameSite=Strict。
- 搜索的 `%`、`_` 按字面字符处理，不作为用户可控的通配符。
- 页码从 1 开始，每页 1–100 条。时间相同再按 ID 排序，静态数据翻页不重复；并发插入时 offset 分页不提供跨请求快照。

## 验收结果

2026-10-01，在 Java 25（编译目标 17）上通过 Maven `verify`，同一套 3 个 HTTP 集成测试分别在 H2 和独立 MySQL 8.4.11 上通过，0 失败、0 错误。覆盖注册、重复用户名、空值、错误密码、密码哈希、注销、未登录访问、进度三个状态、非法页数、增删查、分页、跨用户读改删、伪造归属、搜索和统计。

选做的 SpringDoc 在线文档未集成；提供可运行 HTTP 请求和完整 Markdown 接口文档。没有前端页面，也不需要前端即可完成题目验收。
