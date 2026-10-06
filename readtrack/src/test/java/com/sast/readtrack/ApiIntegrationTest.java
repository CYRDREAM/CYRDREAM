package com.sast.readtrack;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 真正启动 HTTP 服务与数据库，覆盖题目验收和跨用户访问。 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:readtrack;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${TEST_DB_USER:sa}",
    "spring.datasource.password=${TEST_DB_PASSWORD:}",
    "spring.datasource.driver-class-name=${TEST_DB_DRIVER:org.h2.Driver}"
})
class ApiIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    final ObjectMapper json = new ObjectMapper();

    HttpClient client() {
        return HttpClient.newHttpClient();
    }
    JsonNode call(HttpClient client,String method,String path,Object body,int status) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:"+port+path))
            .header("Content-Type","application/json")
            .method(method,body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response = client.send(request.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(status,response.statusCode(),method+" "+path+" "+response.body());
        return json.readTree(response.body());
    }
    Map<String,String> credentials(String user,String password) { return Map.of("username",user,"password",password); }
    long book(HttpClient c,String title) throws Exception {
        return call(c,"POST","/books",Map.of("title",title,"author","作者","totalPages",100),201).path("data").path("id").asLong();
    }


    @org.junit.jupiter.api.BeforeEach
    void prepareUsers() {
        jdbc.update("DELETE FROM rt_books");
        jdbc.update("DELETE FROM rt_users");
        jdbc.update("INSERT INTO rt_users(id,username,password) VALUES(1,'owner','test-password')");
        jdbc.update("INSERT INTO rt_users(id,username,password) VALUES(2,'other','test-password')");
    }

    @Test void registrationAndLogin() throws Exception {
        var c=client();
        var input=credentials("tom"+UUID.randomUUID().toString().substring(0,8),"secret123");
        call(c,"GET","/hello",null,200);
        call(c,"POST","/user/register",credentials("   ","abc"),400);
        call(c,"POST","/user/register",credentials("tom",""),400);
        call(c,"POST","/user/register",Map.of("username","tom"),400);
        var registered=call(c,"POST","/user/register",input,201);
        assertFalse(registered.path("data").has("password"));
        String stored=jdbc.queryForObject("SELECT password FROM rt_users WHERE username=?",String.class,input.get("username"));
        assertEquals(input.get("password"),stored);
        call(c,"POST","/user/register",input,409);
        assertEquals("用户名已存在",call(c,"POST","/user/register",input,409).path("message").asText());
        call(c,"POST","/user/login",credentials(input.get("username"),"wrong"),401);
        call(c,"POST","/user/login",credentials("absent","wrong"),401);
        call(c,"POST","/user/login",input,200);
        call(c,"GET","/user/me",null,404);
        call(c,"POST","/user/logout",Map.of(),404);
    }

    @Test void lifecycleAndValidation() throws Exception {
        var c=client(); long id=book(c,"Java 入门"); String path="/books/"+id;
        var initial=call(c,"GET",path,null,200).path("data");
        assertEquals(0,initial.path("readPages").asInt());
        assertEquals("UNREAD",initial.path("status").asText());
        for (int pages : new int[]{25,100,0}) {
            var result=call(c,"PUT",path+"/progress",Map.of("readPages",pages),200).path("data");
            assertEquals(pages==0?"UNREAD":pages==100?"READ":"READING",result.path("status").asText());
        }
        call(c,"PUT",path+"/progress",Map.of("readPages",-1),400);
        call(c,"PUT",path+"/progress",Map.of("readPages",101),400);
        call(c,"POST","/books",Map.of("title"," ","totalPages",1),400);
        call(c,"POST","/books",Map.of("title","bad","totalPages",0),400);
        call(c,"GET","/books?page=0",null,400);
        call(c,"GET","/books?size=101",null,400);
        call(c,"GET","/books?page=abc",null,400);
        call(c,"DELETE",path,null,200);
        call(c,"GET",path,null,404);
        call(c,"DELETE",path,null,404);
        jdbc.update("DELETE FROM rt_users WHERE id=1");
        call(c,"POST","/books",Map.of("title","test","totalPages",10),400);
    }

    @Test void ownershipAndPagination() throws Exception {
        var c=client();
        long first=book(c,"Java 基础"); long second=book(c,"Java 进阶"); long third=book(c,"阅读");
        jdbc.update("INSERT INTO rt_books(title,total_pages,user_id) VALUES('其他用户的书',100,2)");
        long other=jdbc.queryForObject("SELECT id FROM rt_books WHERE user_id=2",Long.class);
        call(c,"GET","/books/"+other,null,404);
        call(c,"PUT","/books/"+other+"/progress",Map.of("readPages",1),404);
        call(c,"DELETE","/books/"+other,null,404);
        var page1=call(c,"GET","/books?page=1&size=2",null,200).path("data");
        var page2=call(c,"GET","/books?page=2&size=2",null,200).path("data");
        assertEquals(3,page1.path("total").asLong());
        assertEquals(2,page1.path("items").size()); assertEquals(1,page2.path("items").size());
        assertEquals(third,page1.path("items").get(0).path("id").asLong());
        assertEquals(second,page1.path("items").get(1).path("id").asLong());
        assertEquals(first,page2.path("items").get(0).path("id").asLong());
        var added=call(c,"POST","/books",Map.of("title","伪造归属","totalPages",10,"userId",2),201).path("data");
        assertEquals(1,added.path("userId").asLong());
        // 删除选做接口后，它们不应再返回成功。
        call(c,"GET","/books/search?keyword=Java",null,400);
        call(c,"GET","/books/stats",null,400);
    }
}
