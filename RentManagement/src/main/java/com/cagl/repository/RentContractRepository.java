package com.cagl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cagl.entity.RentContract;

@Repository
public interface RentContractRepository extends JpaRepository<RentContract, String>{
	
	
	
	List<RentContract> findByBranchID(String branchID);

}
