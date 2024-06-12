package com.cagl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cagl.entity.PaymentReport;

@Repository
public interface PaymentReportRepository extends JpaRepository<PaymentReport, String>{

	List<PaymentReport> findByMonthAndYear(String string, String string2);
	
	
	@Query(value = "SELECT sum(gross) FROM rentmanagement.payment_report where month=:m and year=:y")
	int getGrossSum(@Param("m") String m,@Param("y") int y);
	
	
	
	

}
