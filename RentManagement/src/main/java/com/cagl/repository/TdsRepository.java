package com.cagl.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cagl.entity.Tds;

public interface TdsRepository extends JpaRepository<Tds, String> {
}
