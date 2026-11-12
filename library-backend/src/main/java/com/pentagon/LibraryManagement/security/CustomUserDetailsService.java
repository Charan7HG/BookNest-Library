package com.pentagon.LibraryManagement.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import com.pentagon.LibraryManagement.DAO.UsersDAO;
import com.pentagon.LibraryManagement.entity.Users;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsersDAO usersDAO;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Users user = usersDAO.findByEmail(email);

        if (user == null || user.getProfile() == null || user.getProfile().getEmail() == null
                || user.getRole() == null) {
            throw new UsernameNotFoundException("User Not Found");
        }

        return User.builder()
                .username(user.getProfile().getEmail())
                .password(user.getPassword() == null ? "" : user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}