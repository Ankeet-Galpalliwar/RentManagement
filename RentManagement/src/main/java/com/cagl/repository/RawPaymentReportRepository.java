package com.cagl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cagl.entity.RawPaymentReport;

public interface RawPaymentReportRepository extends JpaRepository<RawPaymentReport, String>{

	List<RawPaymentReport> findByMonthAndYear(String month, String year);

}
