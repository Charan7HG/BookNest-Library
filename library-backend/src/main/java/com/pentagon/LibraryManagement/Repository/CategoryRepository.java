package com.pentagon.LibraryManagement.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pentagon.LibraryManagement.entity.Category;

public interface CategoryRepository extends JpaRepository<Category,Integer>{

	List<Category> findByNameContainingIgnoreCase(String name);

	Optional<Category> findFirstByNameIgnoreCase(String name);

}
