package com.cagl.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.cagl.entity.User;
import com.cagl.repository.UserRepository;

@Service
public class CustomeUserDetailService implements UserDetailsService {

	@Autowired
	UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		// loading user from DataBase...!
		try {
			User user = this.userRepository.findById(username).get();
			return user;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	// now go to Config_class to say we are doing by database authentication
}
