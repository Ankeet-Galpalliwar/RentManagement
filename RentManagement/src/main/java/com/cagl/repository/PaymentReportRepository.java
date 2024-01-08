package com.cagl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cagl.entity.PaymentReport;

@Repository
public interface PaymentReportRepository extends JpaRepository<PaymentReport, String>{

}
