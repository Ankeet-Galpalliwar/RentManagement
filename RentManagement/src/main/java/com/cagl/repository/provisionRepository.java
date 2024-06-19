package com.cagl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cagl.entity.provision;

public interface provisionRepository extends JpaRepository<provision, String> {

	@Query(value = "SELECT sum(provision_amount) FROM provision where contractid=:contractID and flag<=:flag", nativeQuery = true)
	public String getoverallprovisioin(@Param("contractID") String contractID, @Param("flag") String flag);

	@Query(value = "SELECT provision_amount FROM provision where contractid=:contractid and month=:month and year=:year", nativeQuery = true)
	public String getProvision(@Param("contractid") String contractid, @Param("year") String year,
			@Param("month") String month);

	@Query(value = "SELECT * FROM provision where contractid=:flag and year=:year", nativeQuery = true)
	public List<provision> getprovion(@Param("flag") String flag, @Param("year") String year);

	public provision findByContractIDAndYearAndMonth(String contractID, int year, String month);

	public List<provision> findByContractID(String flag);

	public provision findByContractIDAndYearAndMonthAndRemark(String contractID, int year, String month, String string);

	public List<provision> findByContractIDAndYearAndMonthAndProvisiontypeAndPaymentFlag(String contractID,
			int parseInt, String month, String type, String flag);

//	public List<provision> findByContractIDAndYearAndMonthAndProvisiontype(String contractID, int parseInt,
//			String month, String type);

	@Query(value = "SELECT provisionid FROM provision where provisionid like %:like%", nativeQuery = true)
	List<String> getSimilarIDs(@Param("like") String like);

	@Query(value = "select distinct payment_flag from rentmanagement.provision where provisionid like %:like% and provisiontype='Reversed'", nativeQuery = true)
	String getReversedProvisionPaymentFlag(@Param("like") String like);

	@Query(value = "SELECT sum(provision_amount)  FROM rentmanagement.provision where month=:m and year=:y and  provisiontype=:type", nativeQuery = true)
	String getProvisionSum(@Param("m") String m, @Param("y") int y, @Param("type") String type);
}
