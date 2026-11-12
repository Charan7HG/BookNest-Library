package com.pentagon.LibraryManagement.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pentagon.LibraryManagement.entity.Book;

public interface BookRepository extends JpaRepository<Book, Integer> {

    List<Book> findByNameContainingIgnoreCase(String name);
    List<Book> findTop5ByOrderByIdDesc();
    List<Book> findByUsers_Id(int userId);
    List<Book> findByAuthor_Id(int authorId);
    List<Book> findDistinctByCategory_Id(int categoryId);

}
