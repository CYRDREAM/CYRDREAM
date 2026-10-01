package com.sast.readtrack.service;
import com.sast.readtrack.common.ApiException;
import com.sast.readtrack.dto.Requests.NewBook;
import com.sast.readtrack.mapper.BookMapper;
import com.sast.readtrack.model.Book;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class BookService {
    private final BookMapper books;
    public BookService(BookMapper books) { this.books = books; }
    @Transactional public Book create(long uid, NewBook input) {
        Book book = new Book();
        book.setTitle(input.title().strip()); book.setAuthor(input.author());
        book.setTotalPages(input.totalPages()); book.setUserId(uid);
        books.insert(book);
        return get(uid, book.getId());
    }
    public Book get(long uid, long id) {
        Book book = books.find(id, uid);
        // 不区分不存在与无权访问，避免暴露其他人的书籍。
        if (book == null) throw new ApiException(404, "书籍不存在或无权访问");
        return book;
    }
    @Transactional public Book progress(long uid, long id, int pages) {
        Book book = get(uid, id);
        if (pages < 0 || pages > book.getTotalPages()) throw new ApiException(400, "已读页数必须在 0 到总页数之间");
        String status = pages == 0 ? "UNREAD" : pages == book.getTotalPages() ? "READ" : "READING";
        if (books.progress(id, uid, pages, status) == 0) throw new ApiException(404, "书籍不存在或无权访问");
        return get(uid, id);
    }
    public void delete(long uid, long id) {
        if (books.delete(id, uid) == 0) throw new ApiException(404, "书籍不存在或无权访问");
    }
    public record Page(List<Book> items,long total,int page,int size) {}
    @Transactional(readOnly=true) public Page list(long uid,int page,int size,String keyword) {
        if (page < 1 || size < 1 || size > 100) throw new ApiException(400, "page 至少为 1，size 范围为 1 到 100");
        if (keyword.length() > 200) throw new ApiException(400, "搜索关键词不能超过 200 字");
        String pattern = "%" + keyword.replace("!","!!").replace("%","!%").replace("_","!_") + "%";
        return new Page(books.list(uid,pattern,size,((long) page-1)*size),books.count(uid,pattern),page,size);
    }
    @Transactional(readOnly=true) public Map<String,Long> stats(long uid) {
        return Map.of("total",books.count(uid,"%"),"unread",books.countStatus(uid,"UNREAD"),
            "reading",books.countStatus(uid,"READING"),"read",books.countStatus(uid,"READ"));
    }
}
