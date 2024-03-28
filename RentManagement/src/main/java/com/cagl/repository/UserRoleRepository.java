package com.cagl.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cagl.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Integer>{
	public UserRole findByRoleName(String rolename);

}
