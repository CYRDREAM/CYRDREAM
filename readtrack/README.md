# ReadTrack 阅读管理系统
开发使用了 AI 辅助，确实没什么基础，以前学过c++，后端这部分比较感兴趣所以尝试一下，面试的时候下手轻点，后面好好学学
## 启动

1. 用 IDEA 打开本目录的 `pom.xml`，加载 Maven，项目 JDK 选择 17、21 或 25。
2. 在 MySQL 中执行 `sql/create-database.sql`。
3. 给 `ReadtrackApplication` 的运行配置设置环境变量 `MYSQL_PASSWORD`，值为本机数据库密码；默认数据库用户是 root，地址是 localhost:3306/readtrack。
4. 运行主类，访问 `http://localhost:8080/hello`。
5. 注册用户，记录返回的 `data.id`。在新数据库中首次注册通常是 1。如果不是 1，把 `BookController` 的 `CURRENT_USER_ID` 改成注册返回的 ID，重新启动后再测试书籍接口。

应用自动建立 `rt_users` 和 `rt_books`，不会清空数据。已有旧版 BCrypt 账号无法在明文版本登录，请注册新账号；不会自动改动旧密码。

PowerShell 启动：

```powershell
$env:MYSQL_PASSWORD = '你的数据库密码'
.\mvnw.cmd spring-boot:run
```

无需 MySQL 的体验模式：

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
```

体验模式采用 H2 文件库 `data/readtrack-basic`，与原版体验库分开。也需要先注册、确认固定用户 ID。

## 测试和打包

```powershell
.\mvnw.cmd verify
java -jar target/readtrack-1.0.0.jar --spring.profiles.active=demo
```

默认测试使用独立 H2 内存库。MySQL 测试需要创建空的 `readtrack_test`，设置 `TEST_DB_URL`、`TEST_DB_USER`、`TEST_DB_PASSWORD` 和 `TEST_DB_DRIVER=com.mysql.cj.jdbc.Driver`，再执行 verify。测试会清空指定测试库的 rt_users/rt_books，不能指向你自己的练习或生产库。

三个 HTTP 集成测试覆盖：注册登录、空值和重复注册、书籍生命周期、进度状态切换、非法参数、分页顺序、不能读改删他人书籍、不能伪造 userId。验收工具请求在 [requests.http](requests.http)，字段说明在 [接口文档](docs/API.md)。

2026-10-06 在 Java 25（编译目标 17）上，H2 与独立 MySQL 8.4.11 的 `verify` 均通过：3 个测试，0 失败，0 错误。

## 代码结构

```text
controller  接收 HTTP 请求
service     处理业务规则
mapper      执行 SQL
model       数据库实体
dto         请求参数和校验
common      成功响应对象
```

错误使用 Spring Boot 默认响应和 ResponseStatusException，不提供选做的全局异常处理器。
