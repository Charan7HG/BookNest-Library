package com.pentagon.LibraryManagement.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pentagon.LibraryManagement.entity.Profile;

public interface ProfileRepository extends JpaRepository<Profile,String>{

}
