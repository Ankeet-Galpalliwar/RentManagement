package com.cagl.controller;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
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
import com.cagl.dto.RentDueDto;
import com.cagl.dto.Rentduecalculation;
import com.cagl.dto.Responce;
import com.cagl.dto.RfBranchmasterDto;
import com.cagl.dto.provisionDto;
import com.cagl.entity.BranchDetail;
import com.cagl.entity.IfscMaster;
import com.cagl.entity.RentContract;
import com.cagl.entity.RentDue;
import com.cagl.entity.RfBranchMaster;
import com.cagl.entity.provision;
import com.cagl.repository.BranchDetailRepository;
import com.cagl.repository.RentContractRepository;
import com.cagl.repository.RfBrachRepository;
import com.cagl.repository.ifscMasterRepository;
import com.cagl.repository.provisionRepository;
import com.cagl.repository.rentDueRepository;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@CrossOrigin(origins = "*")
public class RentController {

	@Autowired
	RentContractRepository rentContractRepository;

	// check update

//	@Autowired
//	RecipiantRepository recipiantRepository;

	@Autowired
	BranchDetailRepository branchDetailRepository;

	@Autowired
	RfBrachRepository rfBrachRepository;

	@Autowired
	ifscMasterRepository ifscMasterRepository;

	@Autowired
	rentDueRepository dueRepository;

	@Autowired
	provisionRepository provisionRepository;

	// for adding provision
	@PostMapping("setprovision")
	public ResponseEntity<Responce> addprovison(@RequestParam String provisionType,
			@RequestBody provisionDto provisionDto) {
		provision provision = new provision();
		BeanUtils.copyProperties(provisionDto, provision);
		provision.setDateTime(LocalDate.now());
		if (provisionType.equalsIgnoreCase("Make"))
			provision.setProvisiontype(true);
		else
			provision.setProvisiontype(false);

		provision.setProvisionID(
				provisionDto.getContractID() + "-" + provisionDto.getMonth() + "/" + provisionDto.getYear());
		provision save = provisionRepository.save(provision);
		BeanUtils.copyProperties(save, provisionDto);
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data(provisionDto).error(Boolean.FALSE).msg("provision Added").build());
	}

	@GetMapping("getduereportUid") // Base on UniqueID
	public ResponseEntity<Responce> getDueReport(@RequestParam String value) {
		List<RentDue> getrentdue = dueRepository.getrentdue(value);
		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(getrentdue.stream().map(e -> {
			RentDueDto rentDue = new RentDueDto();
			BeanUtils.copyProperties(e, rentDue);
			String Status = rentContractRepository.getstatus(e.getContractID());
			rentDue.setStatus(Status);
			return rentDue;

		})).error(Boolean.FALSE).msg("Due Report Fetch").build());
	}

	@GetMapping("getduereportBid") // Base on BranchID
	public ResponseEntity<Responce> getDueReport1(@RequestParam String value) {
		List<RentDue> getrentdue = dueRepository.getrentdue1(value);
		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(getrentdue.stream().map(e -> {
			RentDueDto rentduedto = new RentDueDto();
			BeanUtils.copyProperties(e, rentduedto);
			String Status = rentContractRepository.getstatus(e.getContractID());
			rentduedto.setStatus(Status);
			return rentduedto;
		})).error(Boolean.FALSE).msg("Due Report Fetch").build());
	}

	/**
	 * @Api is only use for BackEnd Bulk Calculation Purpose..!
	 * 
	 */
//	@GetMapping("makeDue")
	public Boolean generateRentDue() {

		List<RentContract> allcontract = rentContractRepository.getduemakerIDs();
		allcontract.stream().map(e -> {
			return Rentduecalculation.builder().branchID(e.getBranchID()).contractID(e.getUniqueID())
					.escalation(e.getEscalation()).lesseeBranchType(e.getLesseeBranchType())
					.monthlyRent(e.getMonthlyRent()).renewalTenure(e.getAgreementTenure())
					.rentEndDate(e.getRentEndDate()).rentStartDate(e.getRentStartDate()).build();
		}).collect(Collectors.toList()).stream().forEach(data -> {
			createRentdue(data);
		});
		return true;
	}

	@PostMapping("/insertcontract")
	public ResponseEntity<Responce> insertRentContract(@RequestBody RentContractDto rentContractDto) {
		List<Object> responceData = new ArrayList<>();
		rentContractDto.getRecipiants().stream().forEach(data -> {
			RentContract rentContract = new RentContract();
			BeanUtils.copyProperties(rentContractDto, rentContract);
			List<Integer> ids = rentContractRepository.getids().stream().sorted().collect(Collectors.toList());
			if (ids.isEmpty())
				rentContract.setUniqueID(1); // Input value type is int
			else
				rentContract.setUniqueID((ids.get(ids.size() - 1) + 1)); // Input value type is int.

			rentContract.setLessorRecipiantsName(data.getLessorRecipiantsName());
			rentContract.setLessorBankName(data.getLessorBankName());
			rentContract.setLessorBranchName(data.getLessorBranchName());
			rentContract.setLessorIfscNumber(data.getLessorIfscNumber());
			rentContract.setLessorAccountNumber(data.getLessorAccountNumber());
			rentContract.setPanNo(data.getPanNo());
			rentContract.setGstNo(data.getGstNo());
			rentContract.setLessorRentAmount(data.getLessorRentAmount());
			rentContract.setMonthlyRent(data.getLessorRentAmount());

			RentContract save = rentContractRepository.save(rentContract);
			responceData.add(save);
			if (save != null)
				createRentdue(Rentduecalculation.builder().branchID(save.getBranchID()).contractID(save.getUniqueID())
						.escalation(save.getEscalation()).lesseeBranchType(save.getLesseeBranchType())
						.monthlyRent(save.getLessorRentAmount()).renewalTenure(save.getAgreementTenure())
						.rentEndDate(save.getRentEndDate()).rentStartDate(save.getRentStartDate()).build());

		});
		return ResponseEntity.status(HttpStatus.OK).body(
				Responce.builder().data(responceData).msg("Data Added Sucessfully...!").error(Boolean.FALSE).build());
	}

	@GetMapping("getbranchids")
	public List<String> getBranchIDs(@RequestParam String type) {
		if (type.toUpperCase().startsWith("RF")) {
			return rentContractRepository.getbranchIDs("RF").stream().distinct().collect(Collectors.toList());
		} else if (type.toUpperCase().startsWith("GL")) {
			return rentContractRepository.getbranchIDs("GL").stream().distinct().collect(Collectors.toList());
		} else {
			return new ArrayList<>();
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
				return ResponseEntity.status(HttpStatus.OK)
						.body(Responce.builder().error(Boolean.FALSE).data(build).msg("Data present..!").build());
			}
			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().error(Boolean.TRUE).data(null).msg("RF Data Not Present..!").build());
		} else if (type.toUpperCase().startsWith("GL")) {
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
			return ResponseEntity.status(HttpStatus.OK).body(
					Responce.builder().data(null).msg("GL Branch Data Not Exist...!").error(Boolean.TRUE).build());
		} else {
			return ResponseEntity.status(HttpStatus.OK).body(
					Responce.builder().data(new ArrayList<>()).msg("Data Not Exist...!").error(Boolean.TRUE).build());
		}

	}

	@GetMapping("renewalDetails")
	public ResponseEntity<Responce> getinfo(@RequestParam String BranchID) {

		List<RentContract> contractData = rentContractRepository.findByBranchID(BranchID);
		if (contractData != null)
			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().error(Boolean.FALSE)
							.data(contractData.stream().sorted(Comparator.comparing(RentContract::getUniqueID))
									.reduce((first, second) -> second).map(e -> {
										RentContractDto responceData = new RentContractDto();
										BeanUtils.copyProperties(e, responceData);
										return responceData;
									}))
							.msg("Branch Details").build());

		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Responce.builder().error(Boolean.TRUE).data(null).msg("Branch Details").build());

	}

	@GetMapping("/getcontracts")
	public ResponseEntity<Responce> getContracts(@RequestParam String branchID) {
		List<RentContractDto> contractInfos = new ArrayList<>();
		List<RentContract> allContractDetalis = rentContractRepository.findByBranchID(branchID);
		if (!allContractDetalis.isEmpty()) {
			allContractDetalis.stream().forEach(contractInfo -> {
				RentContractDto contractDto = new RentContractDto();
				BeanUtils.copyProperties(contractInfo, contractDto);
				contractInfos.add(contractDto);
			});
			return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().error(Boolean.FALSE)
					.msg("All Contracts Details fetch..!").data(contractInfos).build());
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().error(Boolean.TRUE).msg("Contracts Data Not present..!").data(null).build());

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
	public ResponseEntity<Responce> editContracts(@RequestParam int uniqueID,
			@RequestBody RentContractDto contractDto) {
		RentContract rentContract = rentContractRepository.findById(uniqueID).get();
		BeanUtils.copyProperties(contractDto, rentContract);
		rentContract.setUniqueID(uniqueID);
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
		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder()
				.data(rentContractRepository.getdistrict(state).stream().distinct().collect(Collectors.toList()))
				.error(Boolean.FALSE).msg("Get District").build());
	}

	@GetMapping("filterBranchIDs")
	public ResponseEntity<Responce> getBranchIdsforFilter() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder()
						.data(rentContractRepository.findAll().stream().map(e -> e.getBranchID()).distinct()
								.collect(Collectors.toList()))
						.error(Boolean.FALSE).msg("Get All Branch IDS base on contract master..!").build());
	}

	// ======================DUE CLCULATION LOGIC==========================

	/**
	 * method is use to calculate Rent Due..! {Base on rent StartDate-EndDate and
	 * Amount}
	 * 
	 * @param data
	 */
	public void createRentdue(Rentduecalculation data) {
		double monthlyRent = data.getMonthlyRent();
		double escalationPercent = Double.parseDouble(data.getEscalation().trim());

		LocalDate rentStartDate = LocalDate.parse(data.getRentStartDate() + "", DateTimeFormatter.ISO_DATE);
		LocalDate rentEndDate = LocalDate.parse(data.getRentEndDate() + "", DateTimeFormatter.ISO_DATE);

		int endYear = rentEndDate.getYear();
		int startYear = rentStartDate.getYear();
		LocalDate escalationApplyDate = rentStartDate.plusMonths(11);

		for (int y = rentStartDate.getYear(); y <= rentEndDate.getYear(); y++) {
			RentDue due = new RentDue();
			due.setRentDueID(data.getBranchID() + "-" + data.getContractID() + "-" + y);
			due.setYear(y);
			due.setContractID(data.getContractID());
			due.setEscalation(escalationPercent);
			due.setStartDate(rentStartDate);
			due.setEndDate(rentEndDate);

			if (y == startYear) {
				for (int m = rentStartDate.getMonthValue(); m <= 12; m++) {
					switch (m) {
					case 1:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setJanuary((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setJanuary((int) Math
										.round((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue())
								due.setJanuary((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setJanuary((int) Math.round(monthlyRent));
						}
						break;
					case 2:

						int day;
						if (Year.of(y).isLeap()) {
							day = 29;
						} else {
							day = 28;
						}

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / day)
									* ((day - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / day) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setFebruary((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {

							if (m == rentStartDate.getMonthValue())
								due.setFebruary((int) Math
										.round((monthlyRent / day) * ((day - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue())
								due.setFebruary((int) Math.round((monthlyRent / day) * rentEndDate.getDayOfMonth()));
							else
								due.setFebruary((int) Math.round(monthlyRent));
						}
						break;
					case 3:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setMarch((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setMarch((int) Math
										.round((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue())
								due.setMarch((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setMarch((int) Math.round(monthlyRent));
						}

						break;
					case 4:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setApril((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {

							if (m == rentStartDate.getMonthValue())
								due.setApril((int) Math
										.round((monthlyRent / 30) * ((30 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue())
								due.setApril((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
							else
								due.setApril((int) Math.round(monthlyRent));
						}

						break;
					case 5:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setMay((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setMay((int) Math
										.round((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue())
								due.setMay((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setMay((int) Math.round(monthlyRent));
						}

						break;
					case 6:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setJune((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setJune((int) Math
										.round((monthlyRent / 30) * ((30 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue())
								due.setJune((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
							else
								due.setJune((int) Math.round(monthlyRent));
						}

						break;
					case 7:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setJuly((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setJuly((int) Math
										.round((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue())
								due.setJuly((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setJuly((int) Math.round(monthlyRent));
						}

						break;
					case 8:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setAugust((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setAugust((int) Math
										.round((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue())
								due.setAugust((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setAugust((int) Math.round(monthlyRent));
						}

						break;
					case 9:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setSeptember((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setSeptember((int) Math
										.round((monthlyRent / 30) * ((30 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue())
								due.setSeptember((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
							else
								due.setSeptember((int) Math.round(monthlyRent));
						}

						break;
					case 10:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setOctober((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setOctober((int) Math
										.round((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue())
								due.setOctober((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setOctober((int) Math.round(monthlyRent));
						}

						break;
					case 11:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setNovember((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setNovember((int) Math
										.round((monthlyRent / 30) * ((30 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue())
								due.setNovember((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
							else
								due.setNovember((int) Math.round(monthlyRent));
						}

						break;
					case 12:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setDecember((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setDecember((int) Math
										.round((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue())
								due.setDecember((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setDecember((int) Math.round(monthlyRent));
						}

						break;
					}
				}
			} else if (y == endYear) {
				for (int m = 1; m <= rentEndDate.getMonthValue(); m++) {
					switch (m) {
					case 1:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setJanuary((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue())
								due.setJanuary((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setJanuary((int) Math.round(monthlyRent));
						}
						break;
					case 2:
						int day;
						if (Year.of(y).isLeap()) {
							day = 29;
						} else {
							day = 28;
						}

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / day)
									* ((day - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / day) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setFebruary((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {

							if (m == rentEndDate.getMonthValue())
								due.setFebruary((int) Math.round((monthlyRent / day) * rentEndDate.getDayOfMonth()));
							else
								due.setFebruary((int) Math.round(monthlyRent));
						}
						break;
					case 3:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setMarch((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue())
								due.setMarch((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setMarch((int) Math.round(monthlyRent));
						}

						break;
					case 4:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setApril((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {

							if (m == rentEndDate.getMonthValue())
								due.setApril((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
							else
								due.setApril((int) Math.round(monthlyRent));
						}

						break;
					case 5:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setMay((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue())
								due.setMay((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setMay((int) Math.round(monthlyRent));
						}

						break;
					case 6:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setJune((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue())
								due.setJune((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
							else
								due.setJune((int) Math.round(monthlyRent));
						}

						break;
					case 7:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setJuly((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue())
								due.setJuly((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setJuly((int) Math.round(monthlyRent));
						}

						break;
					case 8:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setAugust((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue())
								due.setAugust((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setAugust((int) Math.round(monthlyRent));
						}

						break;
					case 9:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setSeptember((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue())
								due.setSeptember((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
							else
								due.setSeptember((int) Math.round(monthlyRent));
						}

						break;
					case 10:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setOctober((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue())
								due.setOctober((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setOctober((int) Math.round(monthlyRent));
						}
						break;
					case 11:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setNovember((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue())
								due.setNovember((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
							else
								due.setNovember((int) Math.round(monthlyRent));
						}

						break;
					case 12:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setDecember((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue())
								due.setDecember((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
							else
								due.setDecember((int) Math.round(monthlyRent));
						}
						break;
					}
				}
			} else {
				for (int m = 1; m <= 12; m++) {
					switch (m) {
					case 1:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setJanuary((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent & escalation Date modify as per Updated Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							due.setJanuary((int) Math.round(monthlyRent));
						}
						break;
					case 2:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							int day;
							if (escalationApplyDate.isLeapYear()) {
								day = 29;
							} else {
								day = 28;
							}
							double rentafter = (((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / day)
									* ((day - (escalationApplyDate.getDayOfMonth()) + 1)));
							double rentbefor = ((monthlyRent / day) * (escalationApplyDate.getDayOfMonth() - 1));
							due.setFebruary((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							due.setFebruary((int) Math.round(monthlyRent));
						}
						break;
					case 3:
						if (y == escalationApplyDate.getYear() & m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setMarch((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							due.setMarch((int) Math.round(monthlyRent));
						}
						break;
					case 4:
						if (y == escalationApplyDate.getYear() & m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setApril((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							due.setApril((int) Math.round(monthlyRent));
						}
						break;
					case 5:
						if (y == escalationApplyDate.getYear() & m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setMay((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							due.setMay((int) Math.round(monthlyRent));
						}
						break;
					case 6:
						if (y == escalationApplyDate.getYear() & m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setJune((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							due.setJune((int) Math.round(monthlyRent));
						}

						break;
					case 7:
						if (y == escalationApplyDate.getYear() & m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setJuly((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							due.setJuly((int) Math.round(monthlyRent));
						}
						break;
					case 8:
						if (y == escalationApplyDate.getYear() & m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setAugust((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							due.setAugust((int) Math.round(monthlyRent));
						}
						break;
					case 9:
						if (y == escalationApplyDate.getYear() & m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setSeptember((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							due.setSeptember((int) Math.round(monthlyRent));
						}

						break;
					case 10:
						if (y == escalationApplyDate.getYear() & m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setOctober((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							due.setOctober((int) Math.round(monthlyRent));
						}
						break;
					case 11:
						if (y == escalationApplyDate.getYear() & m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setNovember((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							due.setNovember((int) Math.round(monthlyRent));
						}

						break;
					case 12:
						if (y == escalationApplyDate.getYear() & m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setDecember((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							due.setDecember((int) Math.round(monthlyRent));
						}
						break;
					}
				}

			}

			// save to rent due data in Data Base_Table
			dueRepository.save(due);

		}

	}

}
