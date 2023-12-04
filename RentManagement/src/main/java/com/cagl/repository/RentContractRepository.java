package com.cagl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cagl.entity.RentContract;

@Repository
public interface RentContractRepository extends JpaRepository<RentContract, String> {

	@Query(value = "SELECT  branchid FROM rentmanagement.rent_contract where lessee_branch_type LIKE %:uu%", nativeQuery = true)
	List<String> getbranchIDs(@Param("uu") String uu);
	
	@Query(value = "SELECT premesis_district FROM rentmanagement.rent_contract where lessee_state LIKE %:state%", nativeQuery = true)
	List<String> getdistrict(@Param("state") String state);

	List<RentContract> findByBranchID(String branchID);

}
