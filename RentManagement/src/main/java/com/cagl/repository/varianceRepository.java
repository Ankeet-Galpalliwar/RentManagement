package com.cagl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cagl.entity.Variance;

@Repository
public interface varianceRepository extends JpaRepository<Variance, String> {
	
	@Query(value = "SELECT sum(variance_amount) FROM Variance where contractid=:contractID and flag<:flag", nativeQuery = true)
	public String getoverallvariance(@Param("contractID") String contractID, @Param("flag") String flag);

	public List<Variance> findByContractID(String contractID);


}
