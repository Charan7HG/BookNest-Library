package com.pentagon.LibraryManagement.TestController;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pentagon.LibraryManagement.Repository.AuthorRepository;
import com.pentagon.LibraryManagement.Repository.BookRepository;
import com.pentagon.LibraryManagement.Repository.CategoryRepository;
import com.pentagon.LibraryManagement.Repository.UserRepository;

@RestController
public class DashBoardController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private BookRepository bookRepo;

    @Autowired
    private AuthorRepository authorRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @GetMapping("/dashboard")
    public Map<String,Object> dashboard(){

        Map<String,Object> map = new HashMap<>();

        map.put("users", userRepo.count());
        map.put("books", bookRepo.count());
        map.put("authors", authorRepo.count());
        map.put("categories", categoryRepo.count());

        map.put("recentBooks",
                bookRepo.findTop5ByOrderByIdDesc());

        return map;
    }
}