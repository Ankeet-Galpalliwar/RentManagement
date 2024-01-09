package com.cagl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cagl.entity.RentContract;

@Repository
public interface RentContractRepository extends JpaRepository<RentContract, Integer> {

	@Query(value = "SELECT  branchid FROM rent_contract where lessee_branch_type LIKE %:uu%", nativeQuery = true)
	List<String> getbranchIDs(@Param("uu") String uu);

	@Query(value = "SELECT premesis_district FROM rent_contract where lessee_state LIKE %:state%", nativeQuery = true)
	List<String> getdistrict(@Param("state") String state);

	List<RentContract> findByBranchID(String branchID);

	@Query(value = "Select uniqueid from rent_contract", nativeQuery = true)
	List<Integer> getids();

	@Query(value = "SELECT * FROM rent_contract where rent_end_date > '2023-12-31'", nativeQuery = true)
	List<RentContract> getduemakerIDs();

	@Query(value = "SELECT agreement_activation_status FROM rent_contract where uniqueid=:uid", nativeQuery = true)
	String getstatus(@Param("uid") int uid);

	//and rent_start_date <=:flagDate
	@Query(value = "SELECT uniqueid FROM rent_contract where agreement_activation_status ='Open' and rent_start_date<=:flagDate and rent_end_date>=:flagDate", nativeQuery = true)
	List<String> getcontractIDs(@Param("flagDate")String flagDate);

}
