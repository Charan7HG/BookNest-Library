package com.pentagon.LibraryManagement.TestController;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.Service.CategoryService;
import com.pentagon.LibraryManagement.entity.Book;
import com.pentagon.LibraryManagement.entity.Category;

@RestController
public class CategoryController {
	@Autowired
	private CategoryService cs;

	@PostMapping("/category")
	public ResponseEntity<ResponseStructure<Category>>addCategory(@RequestBody Category c){

		return cs.addCategory(c);
	}

	@GetMapping("/categories")
	public ResponseEntity<ResponseStructure<List<Category>>>getAllCategories(){

		return cs.getAll();
	}
	@GetMapping("/category/books/{id}")
	public ResponseEntity<ResponseStructure<List<Book>>> getBooks(
	        @PathVariable int id) {

	    return  cs.getBooks(id);

	}
	@GetMapping("/category/{id}")
	public ResponseEntity<ResponseStructure<Category>>findCategoryById(@PathVariable int id){

			return cs.findCategoryById(id);
		}

	@GetMapping("/category/search")
	public ResponseEntity<ResponseStructure<List<Category>>>searchCategoryByName(@RequestParam	 String name){

			return cs.searchCategoryByName(name);
		}


	@PutMapping("/category/{id}")
public ResponseEntity<ResponseStructure<Category>>updateCategory(@PathVariable int id, @RequestBody Category category){

		return cs.updateCategory(id, category);
	}

	@DeleteMapping("/category/{id}")
	public ResponseEntity<ResponseStructure<Boolean>> deleteCategory(@PathVariable int id){

			return cs.deleteCategory(id);
		}
}
