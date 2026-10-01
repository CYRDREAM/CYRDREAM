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
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
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
    HttpClient user(String prefix) throws Exception {
        var c = client();
        var input = credentials(prefix+UUID.randomUUID().toString().substring(0,8),"password123");
        call(c,"POST","/user/register",input,201);
        call(c,"POST","/user/login",input,200);
        return c;
    }
    long book(HttpClient c,String title) throws Exception {
        return call(c,"POST","/books",Map.of("title",title,"author","作者","totalPages",100),201).path("data").path("id").asLong();
    }

    @Test void registrationLoginAndLogout() throws Exception {
        var c=client(); var input=credentials("tom"+UUID.randomUUID().toString().substring(0,8),"secret123");
        call(c,"GET","/hello",null,200);
        call(c,"GET","/books",null,401);
        call(c,"POST","/user/register",credentials("   ","abc"),400);
        call(c,"POST","/user/register",credentials("tom",""),400);
        call(c,"POST","/user/register",Map.of("username","tom"),400);
        call(c,"POST","/user/register",credentials("x","密".repeat(25)),400);
        var registered=call(c,"POST","/user/register",input,201);
        assertFalse(registered.path("data").has("password"));
        String stored=jdbc.queryForObject("SELECT password FROM rt_users WHERE username=?",String.class,input.get("username"));
        assertNotEquals(input.get("password"),stored);
        assertTrue(stored.startsWith("$2"));
        call(c,"POST","/user/register",input,409);
        call(c,"POST","/user/login",credentials(input.get("username"),"wrong"),401);
        call(c,"POST","/user/login",credentials("absent","wrong"),401);
        call(c,"POST","/user/login",input,200);
        call(c,"GET","/user/me",null,200);
        call(c,"POST","/user/logout",Map.of(),200);
        call(c,"GET","/books",null,401);
    }

    @Test void lifecycleAndValidation() throws Exception {
        var c=user("life"); long id=book(c,"Java 入门"); String path="/books/"+id;
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
        call(c,"GET","/books/9223372036854775807",null,404);
    }

    @Test void isolationPaginationSearchAndStatistics() throws Exception {
        var a=user("alice"); var b=user("bob");
        long first=book(a,"Java 基础"); long second=book(a,"Java 进阶"); long third=book(a,"100% 阅读");
        book(b,"别人的 Java");
        call(b,"GET","/books/"+first,null,404);
        call(b,"PUT","/books/"+first+"/progress",Map.of("readPages",1),404);
        call(b,"DELETE","/books/"+first,null,404);
        var page1=call(a,"GET","/books?page=1&size=2",null,200).path("data");
        var page2=call(a,"GET","/books?page=2&size=2",null,200).path("data");
        assertEquals(3,page1.path("total").asLong());
        assertEquals(2,page1.path("items").size()); assertEquals(1,page2.path("items").size());
        assertEquals(third,page1.path("items").get(0).path("id").asLong());
        assertEquals(second,page1.path("items").get(1).path("id").asLong());
        assertEquals(first,page2.path("items").get(0).path("id").asLong());
        assertEquals(2,call(a,"GET","/books/search?keyword=Java",null,200).path("data").path("total").asInt());
        assertEquals(1,call(a,"GET","/books/search?keyword=%25",null,200).path("data").path("total").asInt());
        assertEquals(1,call(b,"GET","/books",null,200).path("data").path("total").asInt());
        call(a,"PUT","/books/"+first+"/progress",Map.of("readPages",100),200);
        call(a,"PUT","/books/"+second+"/progress",Map.of("readPages",20),200);
        var stats=call(a,"GET","/books/stats",null,200).path("data");
        assertEquals(3,stats.path("total").asInt()); assertEquals(1,stats.path("read").asInt());
        assertEquals(1,stats.path("reading").asInt()); assertEquals(1,stats.path("unread").asInt());
        // 请求体传入别人的 userId 也不能改变归属：实际身份由 Session 决定。
        var bobId=call(b,"GET","/user/me",null,200).path("data").path("id").asLong();
        var added=call(a,"POST","/books",Map.of("title","伪造归属","totalPages",10,"userId",bobId),201).path("data");
        assertNotEquals(bobId,added.path("userId").asLong());
    }
}
