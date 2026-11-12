package com.pentagon.LibraryManagement.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pentagon.LibraryManagement.entity.Author;

public interface AuthorRepository extends JpaRepository<Author,Integer> {
	List<Author> findByNameContainingIgnoreCase(String name);

}
