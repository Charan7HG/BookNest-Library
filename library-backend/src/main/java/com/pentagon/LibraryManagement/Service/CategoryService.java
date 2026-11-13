package com.pentagon.LibraryManagement.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.pentagon.LibraryManagement.DAO.CategoryDAO;
import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.entity.Book;
import com.pentagon.LibraryManagement.entity.Category;

@Service
public class CategoryService {
	@Autowired
	private CategoryDAO cry;

	public ResponseEntity<ResponseStructure<Category>> addCategory(Category c){
		if (c == null || c.getName() == null || c.getName().isBlank()) {
			return categoryResponse(HttpStatus.BAD_REQUEST, "Category name is required", null);
		}

		String name = c.getName().trim();
		if (cry.findByNameIgnoreCase(name).isPresent()) {
			return categoryResponse(HttpStatus.CONFLICT, "A category with this name already exists", null);
		}

		c.setName(name);
		Category category = cry.addCategory(c);
		return categoryResponse(HttpStatus.CREATED, "Category added...!", category);
	}
	public ResponseEntity<ResponseStructure<List<Category>>> getAll(){
		List<Category> c = cry.getAllCategory();

		ResponseStructure<List<Category>> response = new ResponseStructure<List<Category>>();
		response.setStatuscode(HttpStatus.OK.value());
		response.setMessage("Categories fetched...!");
		response.setData(c);
		return new ResponseEntity<ResponseStructure<List<Category>>>(response,HttpStatus.OK);

	}

	public ResponseEntity<ResponseStructure<List<Book>>> getBooks(int id) {

	    List<Book> books = cry.getBooks(id);

	    ResponseStructure<List<Book>> response = new ResponseStructure<>();

	    if (books != null) {

	        response.setStatuscode(HttpStatus.OK.value());
	        response.setMessage("Categories Fetched Successfully");
	        response.setData(books);

	        return new ResponseEntity<>(response, HttpStatus.OK);

	    } else {

	        response.setStatuscode(HttpStatus.NOT_FOUND.value());
	        response.setMessage("Category Not Found");
	        response.setData(null);

	        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);

	    }

	}
	public ResponseEntity<ResponseStructure<Category>> findCategoryById(int c_id){
		Category c= cry.getCategoryById(c_id);
		if(c != null) {
			ResponseStructure<Category> response = new ResponseStructure<Category>();
			response.setStatuscode(HttpStatus.OK.value());
			response.setMessage("Categories returned...!");
			response.setData(c);
			return new ResponseEntity<ResponseStructure<Category>>(response,HttpStatus.OK);
		}else {
			ResponseStructure<Category> response = new ResponseStructure<Category>();
			response.setStatuscode(HttpStatus.NOT_FOUND.value());
			response.setMessage("Category Not Found");
			response.setData(null);
			return new ResponseEntity<ResponseStructure<Category>>(response,HttpStatus.NOT_FOUND);
		}
	}
	public ResponseEntity<ResponseStructure<List<Category>>> searchCategoryByName(String name) {

	    List<Category> categories = cry.searchByName(name);

	    ResponseStructure<List<Category>> response = new ResponseStructure<>();

	    response.setStatuscode(HttpStatus.OK.value());
	    response.setMessage("Categories Found");
	    response.setData(categories);

	    return new ResponseEntity<>(response, HttpStatus.OK);

	}
	public ResponseEntity<ResponseStructure<Category>> updateCategory( int id,Category category)  {

	    if (category == null || category.getName() == null || category.getName().isBlank()) {
	        return categoryResponse(HttpStatus.BAD_REQUEST, "Category name is required", null);
	    }

	    String name = category.getName().trim();
	    Category duplicate = cry.findByNameIgnoreCase(name).orElse(null);
	    if (duplicate != null && duplicate.getId() != id) {
	        return categoryResponse(HttpStatus.CONFLICT, "A category with this name already exists", null);
	    }
	    category.setName(name);

	    Category updatedCategory = cry.updateCategory(id,category);

	    ResponseStructure<Category> response = new ResponseStructure<>();

	    if (updatedCategory != null) {

	        response.setStatuscode(HttpStatus.OK.value());
	        response.setMessage("Category Updated Successfully");
	        response.setData(updatedCategory);

	        return new ResponseEntity<>(response, HttpStatus.OK);

	    } else {

	        response.setStatuscode(HttpStatus.NOT_FOUND.value());
	        response.setMessage("Category Not Found");
	        response.setData(null);

	        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);

	    }
	}

	public ResponseEntity<ResponseStructure<Boolean>> deleteCategory(int c_id) {

		   boolean category = cry.deleteCategory(c_id);

		    ResponseStructure<Boolean> response = new ResponseStructure<>();

		    if (category) {

		        response.setStatuscode(HttpStatus.OK.value());
		        response.setMessage("Category Deleted Successfully");
		        response.setData(category);

		        return new ResponseEntity<>(response, HttpStatus.OK);

		    } else {

		        response.setStatuscode(HttpStatus.NOT_FOUND.value());
		        response.setMessage("Category Not Found");
		        response.setData(null);

		        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);

		    }
		}

	private ResponseEntity<ResponseStructure<Category>> categoryResponse(
			HttpStatus status, String message, Category category) {
		ResponseStructure<Category> response = new ResponseStructure<Category>();
		response.setStatuscode(status.value());
		response.setMessage(message);
		response.setData(category);
		return new ResponseEntity<ResponseStructure<Category>>(response, status);
	}
}
