package com.pentagon.LibraryManagement.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.pentagon.LibraryManagement.DAO.ProfileDAO;
import com.pentagon.LibraryManagement.DataTransferObjectDTO.ResponseStructure;
import com.pentagon.LibraryManagement.entity.Profile;

@Service
public class ProfileService {
	@Autowired
	private ProfileDAO pd;

	public ResponseEntity<ResponseStructure<Profile>> saveProfile(Profile p){
		Profile profile=pd.save(p);

		ResponseStructure<Profile> response = new ResponseStructure<Profile>();
		response.setStatuscode(HttpStatus.OK.value());
		response.setMessage("Profile saved...!");
		response.setData(profile);
		return new ResponseEntity<ResponseStructure<Profile>>(response,HttpStatus.OK);
	}
	public ResponseEntity<ResponseStructure<List<Profile>>> getAllprofile(){
		List<Profile> p= pd.getAllProfile();

		ResponseStructure<List<Profile>> response = new ResponseStructure<List<Profile>>();
		response.setStatuscode(HttpStatus.OK.value());
		response.setMessage("profile fetched...!");
		response.setData(p);
		return new ResponseEntity<ResponseStructure<List<Profile>>>(response,HttpStatus.OK);

	}

}
