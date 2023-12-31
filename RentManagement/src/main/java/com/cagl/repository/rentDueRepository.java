package com.cagl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cagl.entity.RentDue;

@Repository
public interface rentDueRepository extends JpaRepository<RentDue, String> {
	@Query(value = "SELECT * FROM rent_due where contractid =:contractid order by year asc,contractid asc", nativeQuery = true)
	List<RentDue> getrentdue(@Param("contractid") String contractid);

	@Query(value = "SELECT * FROM rent_due where rent_dueid like %:contractid% order by year asc,contractid asc", nativeQuery = true)
	List<RentDue> getrentdue1(@Param("contractid") String contractid);

//	@Query(value = "SELECT * FROM rent_due", nativeQuery = true)
//	List<RentDue> getRentvalue(@Param("contractid") String contractid, @Param("year") int year,
//			@Param("month") String month);

	
	@Query(value = "SELECT :selectedColumns FROM rent_due e where e.contractid=:contractid and e.year=:year", nativeQuery = true)
	public String getRentvalue(@Param("contractid") String contractid, @Param("year") String year, @Param("selectedColumns") String selectedColumns);

	@Query(value = "SELECT * FROM rent_due where contractid=:contractid", nativeQuery = true)
	public List<RentDue> getUnusedDueData(@Param("contractid") String contractid);
	
}
