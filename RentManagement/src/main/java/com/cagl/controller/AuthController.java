package com.cagl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cagl.Security.JwtAuthRequest;
import com.cagl.Security.JwtAuthResponse;
import com.cagl.Security.JwtTokenHelper;
import com.cagl.customexceptation.InvalidUser;
import com.cagl.entity.User;
import com.cagl.repository.UserRepository;
import com.cagl.repository.UserRoleRepository;

@RestController
@CrossOrigin(origins = "*")
public class AuthController {

	@Autowired
	JwtTokenHelper jwtTokenHelper;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	UserDetailsService userDetailsService;

	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRoleRepository roleRepository;

	@Autowired
	UserRepository userRepository;

	@PostMapping("/login")
	public ResponseEntity<JwtAuthResponse> creatToken(@RequestBody JwtAuthRequest authRequest) throws Exception {
		this.authenticate(authRequest.getUserName(), authRequest.getPassword());

		UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUserName());
		if (userDetails != null) {
			// System.out.println("=============Token Generated===============");
			JwtAuthResponse authResponse = null;
			try {
				String token = this.jwtTokenHelper.generateToken(userDetails);
				authResponse = new JwtAuthResponse();
				authResponse.setToken(token);
				authResponse.setUserName(userDetails.getUsername());

				User user = userRepository.findById(userDetails.getUsername()).get();
				authResponse.setUserrole(user.getRoles());
				if (!user.getStatus().equalsIgnoreCase("ACTIVE")) {
					throw new InvalidUser();
				}

			} catch (Exception e) {
				throw new InvalidUser();
			}

			return new ResponseEntity<>(authResponse, HttpStatus.OK);
		}
		// Exception Throw If Invalid User..!
		throw new InvalidUser();

	}

	// check user is valid or not...!
	private void authenticate(String userName, String password) throws Exception {
		try {
			UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userName,
					password);

			authenticationManager.authenticate(authenticationToken);

		} catch (Exception e) {
			throw new InvalidUser();
		}
	}

	// =======External purpose=============
	@GetMapping("generatepassword")
	public String passwordCreate(@RequestParam String password) {
		return encoder.encode(password);
	}

}
