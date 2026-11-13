package com.pentagon.LibraryManagement.entity;

import java.util.List;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

@Entity
public class Book {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	private String name;
	private double price;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Column(name = "published_year")
	private Integer publishedYear;

	@Column(name = "language", length = 64)
	private String language;

	@ManyToOne
	@JoinColumn(name="author_id")
	private Author author;

	@ManyToOne
	@JoinColumn(name="user_id")

	private Users users;

	@ManyToMany
	@JoinTable(joinColumns=@JoinColumn(name="book_id"),inverseJoinColumns=@JoinColumn(name="category_id"))

	private List<Category> category;

	// Copy counts are read-only values derived from book_copy. They are never
	// persisted on the book row and are filled by BookService before returning.
	@Transient
	private int totalCopies;

	@Transient
	private int availableCopies;

	@Transient
	private int onLoanCopies;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public Author getAuthor() {
		return author;
	}

	public void setAuthor(Author author) {
		this.author = author;
	}



	public List<Category> getCategory() {
		return category;
	}

	public void setCategory(List<Category> category) {
		this.category = category;
	}

	public Users getUsers() {
		return users;
	}

	public void setUsers(Users users) {
		this.users = users;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getPublishedYear() {
		return publishedYear;
	}

	public void setPublishedYear(Integer publishedYear) {
		this.publishedYear = publishedYear;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public int getTotalCopies() {
		return totalCopies;
	}

	public void setTotalCopies(int totalCopies) {
		this.totalCopies = totalCopies;
	}

	public int getAvailableCopies() {
		return availableCopies;
	}

	public void setAvailableCopies(int availableCopies) {
		this.availableCopies = availableCopies;
	}

	public int getOnLoanCopies() {
		return onLoanCopies;
	}

	public void setOnLoanCopies(int onLoanCopies) {
		this.onLoanCopies = onLoanCopies;
	}





}
