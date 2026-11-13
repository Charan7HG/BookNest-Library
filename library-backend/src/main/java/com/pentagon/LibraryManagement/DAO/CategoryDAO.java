package com.pentagon.LibraryManagement.DAO;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pentagon.LibraryManagement.Repository.CategoryRepository;
import com.pentagon.LibraryManagement.Repository.BookRepository;
import com.pentagon.LibraryManagement.entity.Book;
import com.pentagon.LibraryManagement.entity.Category;

@Repository
public class CategoryDAO {
	@Autowired
	private CategoryRepository category;
	@Autowired
	private BookRepository books;

	public Category addCategory(Category c) {
		return category.save(c);
	}

	public Optional<Category> findByNameIgnoreCase(String name) {
		return category.findFirstByNameIgnoreCase(name);
	}

	public List<Category>getAllCategory(){
		return category.findAll();
	}
	 public Category getCategoryById(int id) {

	        Optional<Category> optional = category.findById(id);

	        return optional.orElse(null);

	    }
	 public List<Category> searchByName(String name) {

	        return category.findByNameContainingIgnoreCase(name);

	    }

	 public Category updateCategory(int id, Category updatedCategory) {

	        Optional<Category> optional = category.findById(id);

	        if(optional.isPresent()){

	            Category existingCategory = optional.get();

	            existingCategory.setName(updatedCategory.getName());

	            return category.save(existingCategory);

	        }

	        return null;

	    }



 @Transactional
 public boolean deleteCategory(int id) {

	        Optional<Category> optional = category.findById(id);

	        if(optional.isPresent()){

			Category existingCategory = optional.get();
			books.findDistinctByCategory_Id(id).forEach(book -> {
				if (book.getCategory() != null) {
					book.getCategory().removeIf(bookCategory -> bookCategory.getId() == id);
				}
			});
			category.delete(existingCategory);

	            return true;

	        }

	        return false;

	    }



	 public List<Book> getBooks(int id) {

		    Optional<Category> optional = category.findById(id);

		    if (optional.isPresent()) {

		        return optional.get().getBook();

		    }

		    return null;
		}

}
