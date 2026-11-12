package com.pentagon.LibraryManagement.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pentagon.LibraryManagement.entity.Users;

public interface UserRepository extends JpaRepository<Users,Integer> {
	List<Users> findByNameContainingIgnoreCase(String name);
	Users findByProfileEmail(String email);
}
