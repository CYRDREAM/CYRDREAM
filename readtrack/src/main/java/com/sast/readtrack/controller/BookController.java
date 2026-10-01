package com.sast.readtrack.controller;
import com.sast.readtrack.common.ApiResponse;
import com.sast.readtrack.dto.Requests.*;
import com.sast.readtrack.service.BookService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/books")
public class BookController {
    private final BookService books;
    public BookController(BookService books) { this.books = books; }
    private long uid(HttpSession session) { return (Long) session.getAttribute("userId"); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> create(@Valid @RequestBody NewBook input,HttpSession session) { return ApiResponse.ok("添加成功",books.create(uid(session),input)); }
    @PutMapping("/{id}/progress")
    public ApiResponse<?> progress(@PathVariable long id,@Valid @RequestBody Progress input,HttpSession session) { return ApiResponse.ok("更新成功",books.progress(uid(session),id,input.readPages())); }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id,HttpSession session) { books.delete(uid(session),id); return ApiResponse.ok("删除成功",null); }
    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable long id,HttpSession session) { return ApiResponse.ok("查询成功",books.get(uid(session),id)); }
    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,HttpSession session) { return ApiResponse.ok("查询成功",books.list(uid(session),page,size,"")); }
    @GetMapping("/search")
    public ApiResponse<?> search(@RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,HttpSession session) { return ApiResponse.ok("查询成功",books.list(uid(session),page,size,keyword)); }
    @GetMapping("/stats")
    public ApiResponse<?> stats(HttpSession session) { return ApiResponse.ok("查询成功",books.stats(uid(session))); }
}
