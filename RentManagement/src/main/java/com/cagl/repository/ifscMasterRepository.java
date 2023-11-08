package com.cagl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cagl.entity.IfscMaster;

@Repository
public interface ifscMasterRepository extends JpaRepository<IfscMaster,String> {
	
}
