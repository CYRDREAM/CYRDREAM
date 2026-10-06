# 代码思路与面试练习

这份说明帮助理解实现，不是开发经历或个人贡献的证明。回答面试问题时，以自己能解释、验证的内容为准。

## 先理解一条注册请求

1. 测试工具发送 POST /user/register，JSON 包含 username、password。
2. UserController 的 @RequestBody 把 JSON 转成 Credentials，@Valid 检查不能为空和长度。
3. UserService 去除用户名两侧空白，查询是否已存在。
4. UserMapper 执行 INSERT，把数据写进 rt_users。
5. 数据库生成 id，MyBatis 的 @Options 把它回填到 User 对象。
6. Controller 只返回 id、username，不返回 password。

Controller 是入口，Service 管规则，Mapper 管 SQL。面试不要只背三个名称，要能找到对应方法并说清一条请求的路径。

## 为什么重复注册要检查两次

查询用户名能给出明确错误。但两个请求可能同时查询到“不存在”，随后一起插入。数据库 UNIQUE 约束才是最后保证；捕获 DuplicateKeyException 后返回 409。这不是重复代码，而是在不同位置解决不同问题。

## 登录做了什么

根据用户名查数据库，再使用 equals 比较密码字符串。账号不存在或密码不符都返回“用户名或密码错误”。基础版按题目允许明文存储，没有 Session/JWT。登录成功不会改变 BookController 的 CURRENT_USER_ID，也不能代表真实身份认证已完成。

## 书籍归属怎么检查

BookController 传固定用户 ID 给 Service。Mapper 的 WHERE 同时带 id 和 user_id。只拿到书籍 ID 不够，必须属于模拟用户才能查询、修改或删除。添加书籍时归属由后端设置，请求体里的 userId 不参与决定。

## 阅读状态怎么计算

先查询书籍总页数，检查 0 <= readPages <= totalPages。0 是 UNREAD；等于总页数是 READ；其余是 READING。状态由后端计算，避免客户端传来相互矛盾的进度和状态。SQL 同时更新进度和状态。

## 分页怎么算

offset = (page - 1) * size。例如 page=2、size=10，跳过前 10 条再取 10 条。LIMIT 控制条数，OFFSET 控制起点。COUNT 得到总量。ORDER BY created_at DESC, id DESC 保证相同创建时间时也有确定顺序。并发插入时 offset 分页不是跨请求快照。

## 常见注解

| 注解 | 作用 |
|---|---|
| @SpringBootApplication | 启动和扫描组件 |
| @RestController | 返回数据的接口类 |
| @RequestMapping / @PostMapping | 把路径和 HTTP 方法映射到 Java 方法 |
| @RequestBody | 读取 JSON 请求体 |
| @PathVariable | 获取路径里的书籍 id |
| @RequestParam | 获取 page、size |
| @Valid | 执行请求字段校验 |
| @Service | 将业务类交给 Spring 管理 |
| @Mapper | 让 MyBatis 实现 SQL 接口 |
| @Transactional | 在事务中完成相关数据库操作 |

## 自测问题

1. 指出注册请求从哪一行进入，哪一行真正操作数据库。
2. 为什么不能使用 == 比较密码字符串？
3. 如果删除 WHERE 中的 user_id 条件，会出现什么问题？
4. 第三页、每页 5 条，offset 是多少？（10）
5. 总页数 100，readPages 为 -1、0、30、100、101 时各是什么结果？
6. 登录了用户 2，为什么列表仍是用户 1 的书？下一阶段要怎样改？
7. 把 404 改成 200 会对测试工具判断结果有什么影响？

建议实际修改一条规则、运行接口、观察数据库，再解释修改前后的行为。能独立完成这样的练习，比背一段项目介绍更有帮助。
