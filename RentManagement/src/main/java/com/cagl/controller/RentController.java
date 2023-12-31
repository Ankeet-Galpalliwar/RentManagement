package com.cagl.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cagl.dto.BranchDto;
import com.cagl.dto.PaymentReportDto;
import com.cagl.dto.RentContractDto;
import com.cagl.dto.RentDueDto;
import com.cagl.dto.Rentduecalculation;
import com.cagl.dto.Responce;
import com.cagl.dto.RfBranchmasterDto;
import com.cagl.dto.provisionDto;
import com.cagl.entity.BranchDetail;
import com.cagl.entity.IfscMaster;
import com.cagl.entity.PaymentReport;
import com.cagl.entity.RentContract;
import com.cagl.entity.RentDue;
import com.cagl.entity.RfBranchMaster;
import com.cagl.entity.provision;
import com.cagl.repository.BranchDetailRepository;
import com.cagl.repository.PaymentReportRepository;
import com.cagl.repository.RentContractRepository;
import com.cagl.repository.RfBrachRepository;
import com.cagl.repository.ifscMasterRepository;
import com.cagl.repository.provisionRepository;
import com.cagl.repository.rentDueRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Ankeet
 *
 */
@RestController
@Slf4j
@CrossOrigin(origins = "*")
public class RentController {

	@Autowired
	PaymentReportRepository paymentReportRepository;

	@Autowired
	RentContractRepository rentContractRepository;

	@Autowired
	BranchDetailRepository branchDetailRepository;

	@Autowired
	RfBrachRepository rfBrachRepository;

	@Autowired
	ifscMasterRepository ifscMasterRepository;

	@Autowired
	rentDueRepository dueRepository;

	@Autowired
	provisionRepository provisionRepository;// ouija

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

	/**
	 * @API -> TO make Provision or Reverse Provision
	 */
	@PostMapping("/setprovision")
	public ResponseEntity<Responce> addprovison(@RequestParam String provisionType,
			@RequestBody provisionDto provisionDto) throws ParseException {
		provision provision = new provision();
		BeanUtils.copyProperties(provisionDto, provision);
		if (provisionType.equalsIgnoreCase("Reverse"))
			provision.setProvisionAmount(-provisionDto.getProvisionAmount());
		provision.setDateTime(LocalDate.now());
		provision.setProvisiontype(provisionType);
		provision.setProvisionID(
				provisionDto.getContractID() + "-" + provisionDto.getMonth() + "/" + provisionDto.getYear());
		// Flag Value is use only for to generate Payment report.
		provision.setFlag(getFlagDate(provisionDto.getMonth(), provisionDto.getYear(), "start")); // setting some Dummy
																									// Date base
		// on Month&Year
		provision save = provisionRepository.save(provision);
		BeanUtils.copyProperties(save, provisionDto);
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data(provisionDto).error(Boolean.FALSE).msg("provision Added").build());
	}

	/**
	 * @API -> TO Get Provision or Reverse Provision
	 */
	@GetMapping("/getprovision")
	public ResponseEntity<Responce> getprovision(@RequestParam String flag, @RequestParam String year) {
		List<provisionDto> allprovisionDto = new ArrayList<>();
		if (flag.equalsIgnoreCase("all")) {
			List<provision> allprovision = provisionRepository.findAll();
			allprovisionDto = allprovision.stream().filter(e -> e.getYear() == Integer.parseInt(year)).map(e -> {
				provisionDto dto = new provisionDto();
				BeanUtils.copyProperties(e, dto);
				return dto;
			}).collect(Collectors.toList());
			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().data(allprovisionDto).error(Boolean.FALSE).msg("All provision").build());
		} else {
			List<provision> allprovision = provisionRepository.getprovion(flag, year);
			allprovisionDto = allprovision.stream().map(e -> {
				provisionDto dto = new provisionDto();
				BeanUtils.copyProperties(e, dto);
				return dto;
			}).collect(Collectors.toList());
			return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().error(Boolean.FALSE)
					.data(allprovisionDto).msg("provision Base on BranchID").build());
		}
	}

	/**
	 * @API-> Use to fetch value with Dynamic column Name Using JDBD Template
	 * @param sqlQuery
	 * @return -> Amount or 0(Zero)
	 */
	public List<String> getvalue(String sqlQuery) {

		return jdbcTemplate.query(sqlQuery, (resultSet, rowNum) -> {
			if (resultSet != null)
				return resultSet.getString(1);
			return "0.0";
		});
	}

	/**
	 * @API -> To Generate Payment Report
	 * @return Report DtoObject.
	 */
	public PaymentReportDto generatePaymentreport(String contractID, String month, String year) {
		double tds = 0.0;
		double DueValue = 0.0;
		double gross = 0.0;
		double Net = 0.0;
		double provision = 0.0;
		String strprovision = provisionRepository.getProvision(contractID, year + "", month);
		if (strprovision != null) {
			if (strprovision.startsWith("-"))
				DueValue = Double.parseDouble(strprovision) * -1;
			else
				provision = Double.parseDouble(strprovision);
		}
		RentContractDto info = new RentContractDto();

		try {
			// ---------Contract Info---------------
			RentContract rentContract = rentContractRepository.findById(Integer.parseInt(contractID)).get();
			BeanUtils.copyProperties(rentContract, info);

			// -------Calculate DUE-----------------
			LocalDate flagDate = getFlagDate(month, Integer.parseInt(year), "start");
			String overallprovisioin = provisionRepository.getoverallprovisioin(contractID, flagDate + "");
			// Query ->To fetch RentDue Value..!
			String SqlQuery = "SELECT " + month + " FROM rent_due e where e.contractid='" + contractID
					+ "' and e.year='" + year + "'";
			double MonthRent = Double.parseDouble(getvalue(SqlQuery).get(0));
			if (overallprovisioin != null) {
				// -----------provision value Initiate-----------------overall active base on
				// overall active base on date provision sum ...!
				provision += Double.parseDouble(overallprovisioin);

				DueValue += Double.parseDouble(overallprovisioin) + MonthRent;
			} else
				DueValue += MonthRent;

			// ----------Gross Value initiate---------
			gross = DueValue - provision;

			// ----------TDS Value initiate---------
			double overallTDSValue = 0.0;
			LocalDate sdate = null;
			if (flagDate.getMonthValue() < 4)
				sdate = LocalDate.of(flagDate.getYear() - 1, 4, 1);
			if (flagDate.getMonthValue() >= 4)
				sdate = LocalDate.of(flagDate.getYear(), 4, 1);
			for (int m = 0; m < 12; m++) {
				LocalDate CrDate = sdate.plusMonths(m);
				String tdsQuery = "SELECT " + CrDate.getMonth() + " FROM rent_due e where e.contractid='" + contractID
						+ "' and e.year='" + CrDate.getYear() + "'";
				if (!getvalue(tdsQuery).isEmpty())
					overallTDSValue += Double.parseDouble(getvalue(tdsQuery).get(0));
			}

			// IF(TDS->right)->Here TDS Value modify Base on Gross Value..!
			if (overallTDSValue >= 240000)
				tds = Math.round((gross * (10 / 100.0f)));// By Default its (0.0)

			// ----------Net Value initiate-----------
			Net = gross - tds;

		} catch (Exception e) {
			System.out.println(e + "---------Exception---------");
		}

		return PaymentReportDto.builder().due(DueValue).Gross(gross).Info(info).monthlyRent(info.getLessorRentAmount())
				.net(Net).provision(provision).tds(tds).monthYear(month + "/" + year).build();
//		return ResponseEntity.status(HttpStatus.OK)
//				.body(Responce.builder()
//						.data(PaymentReportDto.builder().due(DueValue).Gross(gross).Info(info)
//								.monthlyRent(info.getLessorRentAmount()).net(Net).provision(provision).tds(tds).build())
//						.error(Boolean.FALSE).msg("Payment Report Data..!").build());			
	}

	@GetMapping("/generatePaymentReport")
	public ResponseEntity<Responce> generatePaymentReport(@RequestParam String contractID, @RequestParam String month,
			@RequestParam String year) {
		if (contractID.equalsIgnoreCase("all")) {
			List<PaymentReportDto> reports = new ArrayList<>();

			String flagDate = year + "-12-31";
			List<String> getcontractIDs = rentContractRepository.getcontractIDs(flagDate);
			if (getcontractIDs != null)
				getcontractIDs.stream().map(e -> {
					return reports.add(generatePaymentreport(e, month, year));
				}).collect(Collectors.toList());

			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().data(reports).error(Boolean.FALSE).msg("Payment Report Data..!").build());
		} else {
			PaymentReportDto generatereport = generatePaymentreport(contractID, month, year);
			// Here we are saving(Generated Payment Report) Data for audit purpose.
			paymentReportRepository.save(PaymentReport.builder().branchID(generatereport.getInfo().getBranchID())
					.contractID(contractID).due(generatereport.getDue()).Gross(generatereport.getGross())
					.ID(contractID + "-" + generatereport.getMonthYear()).month(month)
					.monthlyRent(generatereport.getMonthlyRent()).net(generatereport.getNet())
					.provision(generatereport.getProvision()).tds(generatereport.getProvision()).year(year).build());
			return ResponseEntity.status(HttpStatus.OK).body(
					Responce.builder().data(generatereport).error(Boolean.FALSE).msg("Payment Report Data..!").build());
		}

	}

	public ResponseEntity<Responce> createRentDue() {
		return null;
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

	/**
	 * 
	 * @param uniqueID
	 * @param contractDto
	 * @return
	 */
	@PutMapping("/editcontracts")
	public ResponseEntity<Responce> editContracts(@RequestParam int uniqueID,
			@RequestBody RentContractDto contractDto) {
		RentContract rentContract = rentContractRepository.findById(uniqueID).get();
		boolean flagCheck = false; // its false don't calculate Due..!
		if (!rentContract.getRentStartDate().toString().equalsIgnoreCase(contractDto.getRentStartDate().toString())
				|| !rentContract.getRentEndDate().toString()
						.equalsIgnoreCase(contractDto.getRentEndDate().toString())) {
			List<RentDue> unusedDueData = dueRepository.getUnusedDueData(uniqueID + "");
			unusedDueData.stream().forEach(due -> {
				dueRepository.delete(due);
			});
			flagCheck = true;// if (True) Changes done in RentDue orElse no need to change any thing
		}
		BeanUtils.copyProperties(contractDto, rentContract);
		rentContract.setUniqueID(uniqueID);
		RentContract save = rentContractRepository.save(rentContract);
		if (save != null & flagCheck) {
			createRentdue(Rentduecalculation.builder().branchID("shjgdjgsjhdgjhsgj").contractID(save.getUniqueID())
					.escalation(save.getEscalation()).lesseeBranchType(save.getLesseeBranchType())
					.monthlyRent(save.getLessorRentAmount()).renewalTenure(save.getAgreementTenure())
					.rentEndDate(save.getRentEndDate()).rentStartDate(save.getRentStartDate()).build());
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().error(Boolean.FALSE).msg("Edit Sucessfully..!").data(contractDto).build());
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

	// =======================GENERATE FLAG DATE ===========================

	public LocalDate getFlagDate(String month, int year, String sd) throws ParseException {
		String monthInput = month;
		SimpleDateFormat sdfInput = new SimpleDateFormat("MMMM");
		Date date = sdfInput.parse(monthInput);
		java.util.Calendar calendar = java.util.Calendar.getInstance();
		calendar.setTime(date);
		int monthValue = calendar.get(java.util.Calendar.MONTH) + 1;
		// Convert to two-digit string with leading zeros
		String monthValueString = String.format("%02d", monthValue);
		if (sd.equalsIgnoreCase("start"))
			return LocalDate.parse(year + "-" + monthValueString + "-01");
		else
			return LocalDate.parse(year + "-" + monthValueString + "-31");
	}

	// ======================DUE CLCULATION LOGIC==========================

	/**
	 * method is use to calculate Rent Due..! {Base on rent StartDate-EndDate and
	 * Amount}and after every 11 month Escalation% should be add
	 * 
	 * @param Rentduecalculation -> DTO Object..!
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

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

							if (m == rentEndDate.getMonthValue() & y == rentEndDate.getYear()) {

								due.setDecember((int) Math
										.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
												* rentEndDate.getDayOfMonth()));
							} else {
								double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
										* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
								double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
								due.setDecember((int) Math.round(rentafter + rentbefor));
								// Here Monthly rent modify as per Escalation.
								escalationApplyDate = escalationApplyDate.plusMonths(11);
								monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
							}
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
							if (m == rentEndDate.getMonthValue()) {
								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

									due.setJanuary((int) Math
											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
													* rentEndDate.getDayOfMonth()));
								} else {
									due.setJanuary((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								}
							} else
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

							if (m == rentEndDate.getMonthValue()) {
								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

									due.setFebruary((int) Math
											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / day)
													* rentEndDate.getDayOfMonth()));
								} else {
									due.setFebruary(
											(int) Math.round((monthlyRent / day) * rentEndDate.getDayOfMonth()));
								}
							} else
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
							if (m == rentEndDate.getMonthValue()) {
								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

									due.setMarch((int) Math
											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
													* rentEndDate.getDayOfMonth()));
								} else {
									due.setMarch((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								}
							} else
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

							if (m == rentEndDate.getMonthValue()) {
								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

									due.setApril((int) Math
											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
													* rentEndDate.getDayOfMonth()));
								} else {
									due.setApril((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
								}
							} else
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
							if (m == rentEndDate.getMonthValue()) {
								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

									due.setMay((int) Math
											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
													* rentEndDate.getDayOfMonth()));
								} else {
									due.setMay((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								}
							} else
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
							if (m == rentEndDate.getMonthValue()) {
								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

									due.setJune((int) Math
											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
													* rentEndDate.getDayOfMonth()));
								} else {
									due.setJune((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
								}
							} else
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
							if (m == rentEndDate.getMonthValue()) {
								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

									due.setJuly((int) Math
											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
													* rentEndDate.getDayOfMonth()));
								} else {
									due.setJuly((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								}
							} else
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
							if (m == rentEndDate.getMonthValue()) {
								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

									due.setAugust((int) Math
											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
													* rentEndDate.getDayOfMonth()));
								} else {
									due.setAugust((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								}
							} else
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
							if (m == rentEndDate.getMonthValue()) {
								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

									due.setSeptember((int) Math
											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
													* rentEndDate.getDayOfMonth()));
								} else {
									due.setSeptember(
											(int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
								}
							} else
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
							if (m == rentEndDate.getMonthValue()) {
								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

									due.setOctober((int) Math
											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
													* rentEndDate.getDayOfMonth()));
								} else {
									due.setOctober((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								}
							} else
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
							if (m == rentEndDate.getMonthValue()) {
								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

									due.setNovember((int) Math
											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 30)
													* rentEndDate.getDayOfMonth()));
								} else {
									due.setNovember((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
								}
							} else
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
							if (m == rentEndDate.getMonthValue()) {
								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

									due.setDecember((int) Math
											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
													* rentEndDate.getDayOfMonth()));
								} else {
									due.setDecember((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								}
							} else
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
