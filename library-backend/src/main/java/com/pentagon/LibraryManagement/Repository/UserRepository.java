package com.pentagon.LibraryManagement.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pentagon.LibraryManagement.entity.Users;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<Users,Integer> {
	List<Users> findByNameContainingIgnoreCase(String name);
	Users findByProfileEmail(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Users u where u.id = :id")
	Optional<Users> findByIdForUpdate(@Param("id") int id);
}
