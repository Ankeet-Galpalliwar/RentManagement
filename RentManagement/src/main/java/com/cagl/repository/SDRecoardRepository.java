package com.cagl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cagl.entity.SDRecords;

@Repository
public interface SDRecoardRepository extends JpaRepository<SDRecords, String> {

}
