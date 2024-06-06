package com.cagl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cagl.entity.RentContract;

@Repository
public interface RentContractRepository extends JpaRepository<RentContract, Integer> {

	@Query(value = "SELECT  distinct lessee_branch_name  FROM rent_contract", nativeQuery = true)
	List<String> getbranchNames();
	
	@Query(value = "SELECT  distinct lessee_area_name  FROM rent_contract", nativeQuery = true)
	List<String> getAreaName();
	
	@Query(value = "SELECT  distinct lessee_zone  FROM rent_contract", nativeQuery = true)
	List<String> getZoneName();
	
	@Query(value = "SELECT  distinct lessee_division  FROM rent_contract", nativeQuery = true)
	List<String> getDivisionName();

	@Query(value = "SELECT  distinct branchid  FROM rent_contract", nativeQuery = true)
	List<String> getbranchIds();

	@Query(value = "SELECT  branchid FROM rent_contract where lessee_branch_type LIKE %:uu%", nativeQuery = true)
	List<String> getbranchIDs(@Param("uu") String uu);

	@Query(value = "SELECT premesis_district FROM rent_contract where lessee_state LIKE %:state%", nativeQuery = true)
	List<String> getdistrict(@Param("state") String state);

	List<RentContract> findByBranchID(String branchID);
	
	List<RentContract> findByLesseeBranchName(String branchName);
	
	/**
	 * @API use to avoid Duplicate in p_Contrct_ID
	 * @param ID
	 * @return
	 */
	List<RentContract>  findByPriviousContractID(int ID);

	/**
	 * @ for Pending COntract(Checker Screen)
	 * @param ContractZoneStatus and e_time_zone 
	 * @return
	 */
	@Query(value = "SELECT * FROM rent_contract where contract_zone='PENDING' and e_time_zone is null", nativeQuery = true)
	List<RentContract> getNewPendingContract();

	@Query(value = "SELECT * FROM rent_contract where contract_zone='PENDING' and e_time_zone is not null", nativeQuery = true)
	List<RentContract> getUpdatedPendingContract();

	@Query(value = "Select uniqueid from rent_contract", nativeQuery = true)
	List<Integer> getids();

	@Query(value = "SELECT * FROM rent_contract where rent_end_date > '2023-12-31'", nativeQuery = true)
	List<RentContract> getduemakerIDs();

	@Query(value = "SELECT agreement_activation_status FROM rent_contract where uniqueid=:uid", nativeQuery = true)
	String getstatus(@Param("uid") int uid);

	@Query(value = "SELECT uniqueid FROM rent_contract where agreement_activation_status ='Open' and rent_start_date<=:EflagDate and rent_end_date>=:SflagDate and contract_zone='APPROVED'", nativeQuery = true)
	List<String> getcontractIDs(@Param("SflagDate") String SflagDate, @Param("EflagDate") String EflagDate);

	@Query(value = "SELECT max(uniqueid) FROM rent_contract where contract_zone='Approved'",nativeQuery = true)
	int getmaxapprovedID();

	@Query(value = "SELECT max(uniqueid) FROM rent_contract",nativeQuery = true)
	int getMaxID();

	@Query(value = "SELECT * FROM rentmanagement.rent_contract where contract_zone='pending' and uniqueid<:maxID",nativeQuery = true)
	 List<RentContract> getalertcontract(@Param("maxID")int maxID);

	@Query(value = "SELECT count(*)  FROM provision where year=:year",nativeQuery = true)
	int getProvisionCount(@Param("year") int year);

	@Query(value = "SELECT count(*)  FROM variance where year=:year",nativeQuery = true)
	int getVarianceCount(@Param("year") int year);

	List<RentContract> findByAgreementActivationStatus(String string);

	List<RentContract> findByLesseeStateAndContractZone(String state,String zone);
	
	List<RentContract> findByPremesisDistrictAndContractZone(String state,String zone);


	/*
	 * @ Error->{Due to incorrect ResultSet}
	 */
//	@Query(value = "update rent_contract set contract_zone='APPROVED' where uniqueid=:contractID", nativeQuery = true)
//	int changeContractZone(@Param("contractID") int ContractID);

}
