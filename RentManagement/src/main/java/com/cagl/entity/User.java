package com.cagl.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
public class User implements UserDetails {
	
	@Id
	private String userID;

	private String password;
	
	private String name;
	
	private String status;

	
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "User_Roles", joinColumns = @JoinColumn(name="ID"),inverseJoinColumns =@JoinColumn(name="roleID") )
	private List<UserRole> roles;


	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<SimpleGrantedAuthority> auth = new ArrayList<>();
		for (UserRole userRole : roles) {
			SimpleGrantedAuthority authority = new SimpleGrantedAuthority(userRole.getRoleName());
			auth.add(authority);
		}
		return auth;
	}

	public String getStatus() {
		// TODO Auto-generated method stub
		return this.status;
	}

	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return this.userID;
	}
	public List<String> getRoles() {
		return this.roles.stream().map(e -> e.getRoleName()).collect(Collectors.toList());
	}

	@Override
	public boolean isAccountNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}


	@Override
	public boolean isAccountNonLocked() {
		// TODO Auto-generated method stub
		return true;
	}


	@Override
	public boolean isCredentialsNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}


	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return true;
	}

}
