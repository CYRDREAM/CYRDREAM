package com.sast.readtrack.mapper;

import com.sast.readtrack.model.Book;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface BookMapper {
    @Insert("INSERT INTO rt_books(title,author,total_pages,user_id) VALUES(#{title},#{author},#{totalPages},#{userId})")
    @Options(useGeneratedKeys=true, keyProperty="id")
    int insert(Book book);

    @Select("SELECT * FROM rt_books WHERE id=#{id} AND user_id=#{userId}")
    Book find(@Param("id") long id, @Param("userId") long userId);

    @Update("UPDATE rt_books SET read_pages=#{pages}, status=#{status} WHERE id=#{id} AND user_id=#{userId}")
    int progress(@Param("id") long id, @Param("userId") long userId,
                 @Param("pages") int pages, @Param("status") String status);

    @Delete("DELETE FROM rt_books WHERE id=#{id} AND user_id=#{userId}")
    int delete(@Param("id") long id, @Param("userId") long userId);

    @Select("SELECT * FROM rt_books WHERE user_id=#{userId} ORDER BY created_at DESC, id DESC LIMIT #{size} OFFSET #{offset}")
    List<Book> list(@Param("userId") long userId, @Param("size") int size,
                    @Param("offset") long offset);

    @Select("SELECT COUNT(*) FROM rt_books WHERE user_id=#{userId}")
    long count(@Param("userId") long userId);
}
