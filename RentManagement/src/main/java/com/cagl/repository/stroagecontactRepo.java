package com.cagl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cagl.entity.StroageRentContract;

@Repository
public interface stroagecontactRepo extends JpaRepository<StroageRentContract
, Integer> {

}
