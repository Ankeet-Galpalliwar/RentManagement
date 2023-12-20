package com.cagl.controller;

import java.time.LocalDate;
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
import com.cagl.dto.Rentduecalculation;
import com.cagl.dto.Responce;
import com.cagl.dto.RfBranchmasterDto;
import com.cagl.dto.provisionDto;
import com.cagl.entity.BranchDetail;
import com.cagl.entity.IfscMaster;
import com.cagl.entity.Recipiant;
import com.cagl.entity.RentContract;
import com.cagl.entity.RentDue;
import com.cagl.entity.RfBranchMaster;
import com.cagl.entity.provision;
import com.cagl.repository.BranchDetailRepository;
import com.cagl.repository.RecipiantRepository;
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

	@Autowired
	RecipiantRepository recipiantRepository;

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
	
	
	
	@PostMapping("addprovision")
	public ResponseEntity<Responce> addprovison(@RequestBody provisionDto provisionDto){
		provision provision=new provision();
		BeanUtils.copyProperties(provisionDto, provision);
		provision.setProvisionID(provisionDto.getContractID()+"-"+provisionDto.getMonth()+"/"+provisionDto.getYear());
		provision save = provisionRepository.save(provision);
		BeanUtils.copyProperties(save, provisionDto);
		return ResponseEntity
				.status(HttpStatus.OK).body(
						Responce.builder()
								.data(provisionDto)
								.error(Boolean.FALSE).msg("provision Added").build());
	}

	@GetMapping("getduereport")
	public ResponseEntity<Responce> getDueReport(@RequestParam String value) {
		List<RentDue> getrentdue = dueRepository.getrentdue(value);
		return ResponseEntity
				.status(HttpStatus.OK).body(
						Responce.builder()
								.data(getrentdue.stream().sorted(Comparator.comparing(RentDue::getRentDueID))
										.collect(Collectors.toList()))
								.error(Boolean.FALSE).msg("Due Report Fetch").build());
	}

	@PostMapping("/insertcontract")
	public ResponseEntity<Responce> insertRentContract(@RequestBody RentContractDto rentContractDto) {
		RentContract rentContract = new RentContract();

		BeanUtils.copyProperties(rentContractDto, rentContract);
		List<Integer> ids = rentContractRepository.getids().stream().map(e -> Integer.parseInt(e)).sorted()
				.collect(Collectors.toList());
		rentContract.setUniqueID((ids.get(ids.size() - 1) + 1) + ""); // Input value type is String.

		RentContract save = rentContractRepository.save(rentContract);

		if (save != null)

			createRentdue(Rentduecalculation.builder().branchID(save.getBranchID()).contractID(save.getUniqueID())
					.escalation(save.getEscalation()).lesseeBranchType(save.getLesseeBranchType())
					.monthlyRent(save.getMonthlyRent()).renewalTenure(save.getRenewalTenure())
					.rentEndDate(save.getRentEndDate()).rentStartDate(save.getRentStartDate()).build());// Existing
																										// method call
																										// to calculate
																										// rent due
																										// data.
		else

			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().data(null).msg("ERROR WHILE ADDING DATA...!").error(Boolean.TRUE).build());

		rentContractDto.getRecipiants().stream().forEach(data -> {
			List<Integer> recipiantIDs = recipiantRepository.getRecipiantIDs().stream().map(e -> Integer.parseInt(e))
					.sorted().collect(Collectors.toList());

			Recipiant build = Recipiant.builder().recipiantsID((recipiantIDs.get(recipiantIDs.size() - 1) + 1) + "")
					.lessorBankName(data.getLessorBankName()).lessorRecipiantsName(data.getLessorRecipiantsName())
					.rentContractRecipiant(save).lessorAccountNumber(data.getLessorAccountNumber())
					.lessorBranchName(data.getLessorBranchName()).lessorIfscNumber(data.getLessorIfscNumber()).build();
			recipiantRepository.save(build);
		});

		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(rentContractDto)
				.msg("Data Added Sucessfully...!").error(Boolean.FALSE).build());
	}

	/**
	 * method is use to calculate Rent Due..!
	 * 
	 * @param data
	 */
	public void createRentdue(Rentduecalculation data) {
		double monthlyRent = data.getMonthlyRent();
		int escalationPercent = Integer.parseInt(data.getEscalation().trim());

		LocalDate rentStartDate = LocalDate.parse(data.getRentStartDate() + "", DateTimeFormatter.ISO_DATE);
		LocalDate rentEndDate = LocalDate.parse(data.getRentEndDate() + "", DateTimeFormatter.ISO_DATE);

		int endYear = rentEndDate.getYear();
		int startYear = rentStartDate.getYear();
		LocalDate escalationApplyDate = rentStartDate.plusMonths(11);

		for (int y = rentStartDate.getYear(); y <= rentEndDate.getYear(); y++) {
			RentDue due = new RentDue();
			due.setRentDueID(data.getBranchID() + "-" + data.getContractID() + "-"+ y);
			due.setYear(y);
			due.setContractID(data.getContractID());
			due.setEscalation(escalationPercent);
			due.setStartDate(rentStartDate);
			due.setEndDate(rentEndDate);

			if (y == startYear) {
				for (int m = rentStartDate.getMonthValue(); m <= 12; m++) {
					switch (m) {
					case 1:
						if (m == rentStartDate.getMonthValue())
							due.setJanuary((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1));
						else
							due.setJanuary(monthlyRent);
						break;
					case 2:
						if (m == rentStartDate.getMonthValue()) {
							if (rentStartDate.isLeapYear())
								due.setFebruary((monthlyRent / 29) * ((29 - rentStartDate.getDayOfMonth()) + 1));
							else
								due.setFebruary((monthlyRent / 28) * ((28 - rentStartDate.getDayOfMonth()) + 1));

						} else
							due.setFebruary(monthlyRent);
						break;
					case 3:
						if (m == rentStartDate.getMonthValue())
							due.setMarch((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1));
						else
							due.setMarch(monthlyRent);
						break;
					case 4:
						if (m == rentStartDate.getMonthValue())
							due.setApril((monthlyRent / 30) * ((30 - rentStartDate.getDayOfMonth()) + 1));
						else
							due.setApril(monthlyRent);
						break;
					case 5:
						if (m == rentStartDate.getMonthValue())
							due.setMay((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1));
						else
							due.setMay(monthlyRent);
						break;
					case 6:
						if (m == rentStartDate.getMonthValue())
							due.setJune((monthlyRent / 30) * ((30 - rentStartDate.getDayOfMonth()) + 1));
						else
							due.setJune(monthlyRent);
						break;
					case 7:
						if (m == rentStartDate.getMonthValue())
							due.setJuly((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1));
						else
							due.setJuly(monthlyRent);
						break;
					case 8:
						if (m == rentStartDate.getMonthValue())
							due.setAugust((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1));
						else
							due.setAugust(monthlyRent);
						break;
					case 9:
						if (m == rentStartDate.getMonthValue())
							due.setSeptember((monthlyRent / 30) * ((30 - rentStartDate.getDayOfMonth()) + 1));
						else
							due.setSeptember(monthlyRent);
						break;
					case 10:
						if (m == rentStartDate.getMonthValue())
							due.setOctober((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1));
						else
							due.setOctober(monthlyRent);

						break;
					case 11:
						if (m == rentStartDate.getMonthValue())
							due.setNovember((monthlyRent / 30) * ((30 - rentStartDate.getDayOfMonth()) + 1));
						else
							due.setNovember(monthlyRent);
						break;
					case 12:
						if (m == rentStartDate.getMonthValue())
							due.setDecember((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1));
						else
							due.setDecember(monthlyRent);
						break;
					}
				}
			} else if (y == endYear) {
				for (int m = 1; m <= rentEndDate.getMonthValue(); m++) {
					switch (m) {
					case 1:
						if (m == rentEndDate.getMonthValue())
							due.setJanuary((monthlyRent / 31) * rentEndDate.getDayOfMonth());
						else
							due.setJanuary(monthlyRent);
						break;
					case 2:
						if (m == rentEndDate.getMonthValue()) {
							if (rentEndDate.isLeapYear())
								due.setFebruary((monthlyRent / 29) * rentEndDate.getDayOfMonth());
							else
								due.setFebruary((monthlyRent / 28) * rentEndDate.getDayOfMonth());

						} else
							due.setFebruary(monthlyRent);
						break;
					case 3:
						if (m == rentEndDate.getMonthValue())
							due.setMarch((monthlyRent / 31) * rentEndDate.getDayOfMonth());
						else
							due.setMarch(monthlyRent);
						break;
					case 4:
						if (m == rentEndDate.getMonthValue())
							due.setApril((monthlyRent / 30) * rentEndDate.getDayOfMonth());
						else
							due.setApril(monthlyRent);
						break;
					case 5:
						if (m == rentEndDate.getMonthValue())
							due.setMay((monthlyRent / 31) * rentEndDate.getDayOfMonth());
						else
							due.setMay(monthlyRent);
						break;
					case 6:
						if (m == rentEndDate.getMonthValue())
							due.setJune((monthlyRent / 30) * rentEndDate.getDayOfMonth());
						else
							due.setJune(monthlyRent);
						break;
					case 7:
						if (m == rentEndDate.getMonthValue())
							due.setJuly((monthlyRent / 31) * rentEndDate.getDayOfMonth());
						else
							due.setJuly(monthlyRent);
						break;
					case 8:
						if (m == rentEndDate.getMonthValue())
							due.setAugust((monthlyRent / 31) * rentEndDate.getDayOfMonth());
						else
							due.setAugust(monthlyRent);
						break;
					case 9:
						if (m == rentEndDate.getMonthValue())
							due.setSeptember((monthlyRent / 30) * rentEndDate.getDayOfMonth());
						else
							due.setSeptember(monthlyRent);
						break;
					case 10:
						if (m == rentEndDate.getMonthValue())
							due.setOctober((monthlyRent / 31) * rentEndDate.getDayOfMonth());
						else
							due.setOctober(monthlyRent);

						break;
					case 11:
						if (m == rentEndDate.getMonthValue())
							due.setNovember((monthlyRent / 30) * rentEndDate.getDayOfMonth());
						else
							due.setNovember(monthlyRent);
						break;
					case 12:
						if (m == rentEndDate.getMonthValue())
							due.setDecember((monthlyRent / 31) * rentEndDate.getDayOfMonth());
						else
							due.setDecember(monthlyRent);
						break;
					}
				}
			} else {
				for (int m = 1; m <= 12; m++) {
					switch (m) {
					case 1:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setJanuary(rentafter + rentbefor);
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100) * monthlyRent));
						} else {
							due.setJanuary(monthlyRent);
						}
						break;
					case 2:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							int day;
							if (escalationApplyDate.isLeapYear())
								day = 28;
							else
								day = 29;

							double rentafter = ((monthlyRent + ((escalationPercent / 100) * monthlyRent)) / day)
									* ((day - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / day) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setFebruary(rentafter + rentbefor);
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100) * monthlyRent));
						} else {
							due.setFebruary(monthlyRent);
						}
						break;
					case 3:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setMarch(rentafter + rentbefor);
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100) * monthlyRent));
						} else {
							due.setMarch(monthlyRent);
						}
						break;
					case 4:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setApril(rentafter + rentbefor);
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100) * monthlyRent));
						} else {
							due.setApril(monthlyRent);
						}
						break;
					case 5:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setMay(rentafter + rentbefor);
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100) * monthlyRent));
						} else {
							due.setMay(monthlyRent);
						}
						break;
					case 6:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setJune(rentafter + rentbefor);
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100) * monthlyRent));
						} else {
							due.setJune(monthlyRent);
						}

						break;
					case 7:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setJuly(rentafter + rentbefor);
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100) * monthlyRent));
						} else {
							due.setJuly(monthlyRent);
						}
						break;
					case 8:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setAugust(rentafter + rentbefor);
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100) * monthlyRent));
						} else {
							due.setAugust(monthlyRent);
						}
						break;
					case 9:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setSeptember(rentafter + rentbefor);
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100) * monthlyRent));
						} else {
							due.setSeptember(monthlyRent);
						}

						break;
					case 10:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setOctober(rentafter + rentbefor);
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100) * monthlyRent));
						} else {
							due.setOctober(monthlyRent);
						}

						break;
					case 11:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100) * monthlyRent)) / 30)
									* ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setNovember(rentafter + rentbefor);
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100) * monthlyRent));
						} else {
							due.setNovember(monthlyRent);
						}

						break;
					case 12:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setDecember(rentafter + rentbefor);
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths(11);
							monthlyRent = (monthlyRent + ((escalationPercent / 100) * monthlyRent));
						} else {
							due.setDecember(monthlyRent);
						}
						break;
					}
				}

			}

			// save to rent due data in Data Base_Table
			dueRepository.save(due);

		}

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
		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder()
				.data(rentContractRepository.getdistrict(state).stream().distinct().collect(Collectors.toList()))
				.error(Boolean.FALSE).msg("Get District").build());
	}

//	@GetMapping("getdistrict")
//	public ResponseEntity<Responce> getDistrictBaseonState(@RequestParam String state) {
//		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(rentContractRepository.findAll()
//				.stream()
//				.filter(e -> e.getLesseeState().toUpperCase().equalsIgnoreCase(state.toUpperCase()))
//				.map(data -> data.getPremesisDistrict()).distinct().collect(Collectors.toList())).error(Boolean.FALSE)
//				.msg("Get District").build());
//	}

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
