package com.cagl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cagl.entity.RentContract;

@Repository
public interface RentContractRepository extends JpaRepository<RentContract, String> {

	@Query(value = "SELECT  branchid FROM rentmanagement.rent_contract where lessee_branch_type LIKE:type", nativeQuery = true)
	List<String> getbranchIDs(@Param("type") String type);

	List<RentContract> findByBranchID(String branchID);

}
