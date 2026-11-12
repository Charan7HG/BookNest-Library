package com.pentagon.LibraryManagement.TestController;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.Service.ProfileService;
import com.pentagon.LibraryManagement.entity.Profile;

@RestController
public class ProfileController {
	@Autowired
	private ProfileService ps;

	@GetMapping("/saveprofile")
	public  ResponseEntity<ResponseStructure<Profile>> saveProfile(@PathVariable Profile p){

		return ps.saveProfile(p);
	}

	@GetMapping("/getall")
	public  ResponseEntity<ResponseStructure<List<Profile>>> getAllprofile(){

		return ps.getAllprofile();
	}

}
