package com.sast.readtrack.mapper;
import com.sast.readtrack.model.User;
import org.apache.ibatis.annotations.*;
@Mapper
public interface UserMapper {
    @Select("SELECT id, username, password FROM rt_users WHERE username = #{username}")
    User findByUsername(@Param("username") String username);
    @Insert("INSERT INTO rt_users(username,password) VALUES(#{username},#{password})")
    @Options(useGeneratedKeys=true, keyProperty="id")
    int insert(User user);
}
