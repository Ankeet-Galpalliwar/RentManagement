package com.cagl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cagl.entity.Recipiant;

@Repository
public interface RecipiantRepository extends JpaRepository<Recipiant, String> {

	@Query(value = "SELECT recipiantsid FROM rentmanagement.recipiant", nativeQuery = true)
	List<String> getRecipiantID();

}
