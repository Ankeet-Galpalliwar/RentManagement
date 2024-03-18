package com.cagl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cagl.entity.ConfirmPaymentReport;

@Repository
public interface confirmPaymentRepository extends JpaRepository<ConfirmPaymentReport, String> {

}
