package com.cagl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cagl.entity.ApiCallRecords;

@Repository
public interface ApiCallRepository extends JpaRepository<ApiCallRecords, Long> {

}
