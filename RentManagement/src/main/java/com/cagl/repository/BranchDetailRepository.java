package com.cagl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cagl.entity.BranchDetail;

public interface BranchDetailRepository extends JpaRepository<BranchDetail,String>{
	@Query(value = "SELECT branchid FROM branch_detail", nativeQuery = true)
	List<String> getbranchIDs();

}
