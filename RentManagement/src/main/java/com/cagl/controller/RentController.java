package com.cagl.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cagl.dto.BranchDto;
import com.cagl.dto.RentContractDto;
import com.cagl.dto.Responce;
import com.cagl.dto.RfBranchmasterDto;
import com.cagl.entity.BranchDetail;
import com.cagl.entity.IfscMaster;
import com.cagl.entity.Recipiant;
import com.cagl.entity.RentContract;
import com.cagl.entity.RfBranchMaster;
import com.cagl.repository.BranchDetailRepository;
import com.cagl.repository.RecipiantRepository;
import com.cagl.repository.RentContractRepository;
import com.cagl.repository.RfBrachRepository;
import com.cagl.repository.ifscMasterRepository;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@CrossOrigin(origins = "*")
public class RentController {

	@Autowired
	RentContractRepository rentContractRepository;

	@Autowired
	RecipiantRepository recipiantRepository;

	@Autowired
	BranchDetailRepository branchDetailRepository;

	@Autowired
	RfBrachRepository rfBrachRepository;

	@Autowired
	ifscMasterRepository ifscMasterRepository;

	@PostMapping("/insertcontract")
	public ResponseEntity<Responce> insertRentContract(@RequestBody RentContractDto rentContractDto) {
		RentContract rentContract = new RentContract();
		// ID Creation Logic
//		String ID = rentContractDto.getBranchID() + "_"
//				+ DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now());

		BeanUtils.copyProperties(rentContractDto, rentContract);
		rentContract.setUniqueID(
				rentContractDto.getBranchID() + DateTimeFormatter.ofPattern("MMddHH:MM").format(LocalDateTime.now()));
		RentContract save = rentContractRepository.save(rentContract);

		rentContractDto.getRecipiants().stream().forEach(data -> {
			String recipantID = rentContractDto.getBranchID() + "-R";
			List<String> recipiantID = recipiantRepository.getRecipiantID();
			if (!recipiantID.isEmpty()) {
				List<String> collect = recipiantID.stream().filter(e -> e.contains(rentContractDto.getBranchID()))
						.map(e -> e.substring(e.indexOf("-") + 2)).sorted().collect(Collectors.toList());
				if (collect.isEmpty()) {
					recipantID += "1";
				} else {
					int count = Integer.parseInt(collect.get(collect.size() - 1)) + 1;
					recipantID += "" + count;
				}
			} else {
				recipantID += "1";
			}
			Recipiant build = Recipiant.builder().recipiantsID(recipantID).lessorBankName(data.getLessorBankName())
					.lessorRecipiantsName(data.getLessorRecipiantsName()).rentContractRecipiant(save)
					.lessorAccountNumber(data.getLessorAccountNumber()).lessorBranchName(data.getLessorBranchName())
					.lessorIfscNumber(data.getLessorIfscNumber()).build();
			recipiantRepository.save(build);
		});

		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(rentContractDto)
				.msg("Data Added Sucessfully...!").error(Boolean.FALSE).build());
	}

	@GetMapping("getbranchids")
	public List<String> getBranchIDs(@RequestParam String type) {
		if (type.toUpperCase().startsWith("RF")) {
			return rfBrachRepository.findAll().stream().map(data -> data.getRfBranchID()).distinct()
					.collect(Collectors.toList());
		} else {
			return branchDetailRepository.getbranchIDs().stream().distinct().collect(Collectors.toList());
		}
	}

	@GetMapping("getbranchdetails")
	public ResponseEntity<Responce> getBranchDetails(@RequestParam String BranchID, @RequestParam String type) {

		if (type.toUpperCase().startsWith("RF")) {
			Optional<RfBranchMaster> data = rfBrachRepository.findById(BranchID);
			if (data.isPresent()) {
				RfBranchMaster rfBranchMaster = data.get();
				RfBranchmasterDto build = RfBranchmasterDto.builder()
						.amContactNumber(rfBranchMaster.getAmContactNumber()).areaName(rfBranchMaster.getAreaName())
						.branchName(rfBranchMaster.getBranchName()).region(rfBranchMaster.getRegion())
						.rfBranchID(rfBranchMaster.getRfBranchID()).build();
				return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().error(Boolean.FALSE)
						.data(build)
						.msg("Data present..!").build());
			}
			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().error(Boolean.TRUE).data(null).msg("Data Not Present..!").build());
		} else {
			Optional<BranchDetail> data = branchDetailRepository.findById(BranchID);
			if (data.isPresent()) {
				BranchDetail branchDetail = data.get();
				BranchDto build = BranchDto.builder().areaName(branchDetail.getAreaName())
						.branchID(branchDetail.getBranchID()).branchName(branchDetail.getBranchName())
						.region(branchDetail.getRegion()).state(branchDetail.getState()).zone(branchDetail.getZone())
						.build();
				return ResponseEntity.status(HttpStatus.OK)
						.body(Responce.builder().data(build).msg("Branch Data Exist...!").error(Boolean.FALSE).build());
			}
			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().data(null).msg("Branch Data Not Exist...!").error(Boolean.FALSE).build());
		}
	}

	@GetMapping("/getallcontracts")
	public ResponseEntity<Responce> getAllContracts(@RequestParam String district) {
		List<RentContractDto> contractInfos = new ArrayList<>();
		List<RentContract> allContractDetalis = rentContractRepository.findAll();
		if (!allContractDetalis.isEmpty()) {
			allContractDetalis.stream()
					.filter(data -> data.getPremesisDistrict().trim().equalsIgnoreCase(district.trim()))
					.forEach(contractInfo -> {
						RentContractDto contractDto = new RentContractDto();
						BeanUtils.copyProperties(contractInfo, contractDto);
//				contractDto.setUniqueID(null)
						contractInfos.add(contractDto);
					});
			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().error(Boolean.FALSE).msg("All Contracts Details fetch..!")
							.data(contractInfos.stream().sorted(Comparator.comparing(RentContractDto::getUniqueID)))
							.build());
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().error(Boolean.TRUE).msg("Contracts Data Not present..!").data(null).build());
	}

	@PutMapping("/editcontracts")
	public ResponseEntity<Responce> editContracts(@RequestParam String uniqueID,
			@RequestBody RentContractDto contractDto) {
		RentContract rentContract = rentContractRepository.findById(uniqueID).get();
		contractDto.setUniqueID(uniqueID);
		List<Recipiant> recipiants = rentContract.getRecipiants();
		BeanUtils.copyProperties(contractDto, rentContract);
		contractDto.getRecipiants().stream().forEach(e -> {
//			Recipiant recipiant = recipiantRepository.findById(e.getRecipiantsID()).get();
			Recipiant build = Recipiant.builder().lessorAccountNumber(e.getLessorAccountNumber())
					.lessorBankName(e.getLessorBankName()).lessorBranchName(e.getLessorBranchName())
					.lessorIfscNumber(e.getLessorIfscNumber()).lessorRecipiantsName(e.getLessorRecipiantsName())
					.recipiantsID(e.getRecipiantsID()).rentContractRecipiant(rentContract).build();
			recipiantRepository.save(build);
		});
		rentContractRepository.save(rentContract);

		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().error(Boolean.FALSE).msg("Edit Sucessfully..!").data(contractDto).build());

	}

	@GetMapping("ifscinfo")
	public ResponseEntity<Responce> getIfscInfo(@RequestParam String ifscNumber) {
		Optional<IfscMaster> data = ifscMasterRepository.findById(ifscNumber);
		if (data.isPresent())
			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().data(data.get()).error(Boolean.FALSE).msg("ifsc info").build());
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data(null).error(Boolean.TRUE).msg("IFSC Info Not Present in Master").build());
	}

	@GetMapping("getstate")
	public ResponseEntity<Responce> getState() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce
						.builder().data(rentContractRepository.findAll().stream().map(e -> e.getLesseeState())
								.distinct().collect(Collectors.toList()))
						.error(Boolean.FALSE).msg("Get All State").build());
	}

	@GetMapping("getdistrict")
	public ResponseEntity<Responce> getDistrictBaseonState(@RequestParam String state) {
		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(rentContractRepository.findAll()
				.stream()
				.filter(e -> e.getLesseeState().trim().toUpperCase().equalsIgnoreCase(state.trim().toUpperCase()))
				.map(data -> data.getPremesisDistrict()).distinct().collect(Collectors.toList())).error(Boolean.FALSE)
				.msg("Get District").build());
	}

	@GetMapping("filterBranchIDs")
	public ResponseEntity<Responce> getBranchIdsforFilter() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder()
						.data(rentContractRepository.findAll().stream().map(e -> e.getBranchID()).distinct()
								.collect(Collectors.toList()))
						.error(Boolean.FALSE).msg("Get All Branch IDS base on contract master..!").build());
	}

	// ========================== NON-USE==========================

	@GetMapping("/getcontracts")
	public ResponseEntity<Responce> getContracts(@RequestParam String branchID) {
		List<RentContractDto> contractInfos = new ArrayList<>();
		List<RentContract> allContractDetalis = rentContractRepository.findByBranchID(branchID);
		if (!allContractDetalis.isEmpty()) {
			allContractDetalis.stream().forEach(contractInfo -> {
				RentContractDto contractDto = new RentContractDto();
				BeanUtils.copyProperties(contractInfo, contractDto);
//				contractDto.setUniqueID(null)
				contractInfos.add(contractDto);
			});
			return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().error(Boolean.FALSE)
					.msg("All Contracts Details fetch..!").data(contractInfos).build());
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().error(Boolean.TRUE).msg("Contracts Data Not present..!").data(null).build());

	}

}
