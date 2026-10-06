package com.sast.readtrack.service;

import com.sast.readtrack.dto.Requests.NewBook;
import com.sast.readtrack.mapper.BookMapper;
import com.sast.readtrack.mapper.UserMapper;
import com.sast.readtrack.model.Book;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookService {
    private final BookMapper bookMapper;
    private final UserMapper userMapper;

    public BookService(BookMapper bookMapper, UserMapper userMapper) {
        this.bookMapper = bookMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public Book create(long userId, NewBook input) {
        if (userMapper.findById(userId) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "模拟用户不存在，请先注册并配置 CURRENT_USER_ID");
        }
        Book book = new Book();
        book.setTitle(input.title().strip());
        book.setAuthor(input.author());
        book.setTotalPages(input.totalPages());
        book.setUserId(userId);
        bookMapper.insert(book);
        return get(userId, book.getId());
    }

    public Book get(long userId, long id) {
        Book book = bookMapper.find(id, userId);
        if (book == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "书籍不存在或无权访问");
        }
        return book;
    }

    @Transactional
    public Book progress(long userId, long id, int pages) {
        Book book = get(userId, id);
        if (pages < 0 || pages > book.getTotalPages()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已读页数必须在 0 到总页数之间");
        }

        String status;
        if (pages == 0) {
            status = "UNREAD";
        } else if (pages == book.getTotalPages()) {
            status = "READ";
        } else {
            status = "READING";
        }

        if (bookMapper.progress(id, userId, pages, status) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "书籍不存在或无权访问");
        }
        return get(userId, id);
    }

    public void delete(long userId, long id) {
        if (bookMapper.delete(id, userId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "书籍不存在或无权访问");
        }
    }

    public record Page(List<Book> items, long total, int page, int size) {}

    @Transactional(readOnly=true)
    public Page list(long userId, int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page 至少为 1，size 范围为 1 到 100");
        }
        long offset = ((long) page - 1) * size;
        List<Book> items = bookMapper.list(userId, size, offset);
        long total = bookMapper.count(userId);
        return new Page(items, total, page, size);
    }
}
