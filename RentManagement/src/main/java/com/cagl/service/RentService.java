package com.cagl.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cagl.dto.MakeActualDto;
import com.cagl.dto.Responce;
import com.cagl.dto.provisionDto;

public interface RentService {

	Map<String, String> makeactual(List<MakeActualDto> actualDto);

	provisionDto addprovision(String provisionType, provisionDto provisionDto);

	Responce getprovision(String flag, String year);

	Responce getPaymentReport(String contractID, String month, String year);

}
