package com.cagl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cagl.entity.PaymentReport;

@Repository
public interface PaymentReportRepository extends JpaRepository<PaymentReport, String>{

	List<PaymentReport> findByMonthAndYear(String string, String string2);

}
