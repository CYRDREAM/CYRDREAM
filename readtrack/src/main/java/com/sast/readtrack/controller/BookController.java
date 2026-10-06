package com.sast.readtrack.controller;

import com.sast.readtrack.common.ApiResponse;
import com.sast.readtrack.dto.Requests.NewBook;
import com.sast.readtrack.dto.Requests.Progress;
import com.sast.readtrack.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
public class BookController {
    // 基础版使用固定用户 ID，未实现登录态。
    private static final long CURRENT_USER_ID = 1L;
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> create(@Valid @RequestBody NewBook input) {
        return ApiResponse.ok("添加成功", bookService.create(CURRENT_USER_ID, input));
    }

    @PutMapping("/{id}/progress")
    public ApiResponse<?> progress(@PathVariable long id, @Valid @RequestBody Progress input) {
        return ApiResponse.ok("更新成功", bookService.progress(CURRENT_USER_ID, id, input.readPages()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        bookService.delete(CURRENT_USER_ID, id);
        return ApiResponse.ok("删除成功", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable long id) {
        return ApiResponse.ok("查询成功", bookService.get(CURRENT_USER_ID, id));
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue="1") int page,
                               @RequestParam(defaultValue="10") int size) {
        return ApiResponse.ok("查询成功", bookService.list(CURRENT_USER_ID, page, size));
    }
}
