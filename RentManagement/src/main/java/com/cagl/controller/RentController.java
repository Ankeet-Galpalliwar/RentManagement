package com.cagl.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cagl.dto.BranchDto;
import com.cagl.dto.BulkProvisionDeletion;
import com.cagl.dto.MakeActualDto;
import com.cagl.dto.PaymentReportDto;
import com.cagl.dto.RecipiantDto;
import com.cagl.dto.RentContractDto;
import com.cagl.dto.RentDueDto;
import com.cagl.dto.Rentduecalculation;
import com.cagl.dto.Responce;
import com.cagl.dto.RfBranchmasterDto;
import com.cagl.dto.SDRecoardDto;
import com.cagl.dto.TenureDto;
import com.cagl.dto.provisionDto;
import com.cagl.dto.varianceDto;
import com.cagl.entity.ApiCallRecords;
import com.cagl.entity.BranchDetail;
import com.cagl.entity.IfscMaster;
import com.cagl.entity.PaymentReport;
import com.cagl.entity.RawPaymentReport;
import com.cagl.entity.RentContract;
import com.cagl.entity.RentDue;
import com.cagl.entity.RfBranchMaster;
import com.cagl.entity.SDRecords;
import com.cagl.entity.Variance;
import com.cagl.entity.provision;
import com.cagl.repository.ApiCallRepository;
import com.cagl.repository.BranchDetailRepository;
import com.cagl.repository.PaymentReportRepository;
import com.cagl.repository.RawPaymentReportRepository;
import com.cagl.repository.RentActualRepository;
import com.cagl.repository.RentContractRepository;
import com.cagl.repository.RfBrachRepository;
import com.cagl.repository.SDRecoardRepository;
import com.cagl.repository.ifscMasterRepository;
import com.cagl.repository.provisionRepository;
import com.cagl.repository.rentDueRepository;
import com.cagl.repository.varianceRepository;
import com.cagl.service.RentService;

import net.bytebuddy.asm.Advice.Local;

/**
 * @author Ankeet G.
 */
@RestController
@Component
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
	provisionRepository provisionRepository;
	@Autowired
	RentActualRepository actualRepository;
	@Autowired
	SDRecoardRepository sdRepository;
	@Autowired
	RentService rentService;
	@Autowired
	varianceRepository varianceRepository;
	@Autowired
	ApiCallRepository apirecords;
	@Autowired
	JdbcTemplate jdbcTemplate;

	@GetMapping("/getvariance")
	public ResponseEntity<Responce> getvariance(@RequestParam String contractID) {
		List<varianceDto> allvariDtos = new ArrayList<>();
		List<Variance> allVariance = new ArrayList<>();
		if (contractID.equalsIgnoreCase("all"))
			allVariance = varianceRepository.findAll();
		else
			allVariance = varianceRepository.findByContractID(contractID);
		if (!allVariance.isEmpty() & allVariance != null) {
			allVariance.stream().forEach(e -> {
				varianceDto varianceDto = new varianceDto();
				BeanUtils.copyProperties(e, varianceDto);
				RentContractDto contractDto = new RentContractDto();
				BeanUtils.copyProperties(e.getContractInfo(), contractDto);
				varianceDto.setInfo(contractDto);
				allvariDtos.add(varianceDto);
			});
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder()
						.data(allvariDtos.stream().sorted(Comparator.comparing(varianceDto::getFlag))
								.collect(Collectors.toList()))
						.error(Boolean.FALSE).msg("ALL variance [" + contractID + "]").build());
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
						.builder().data(rentContractRepository.findAll().stream().map(e -> e.getLesseeState()).sorted()
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
		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder()
				.data(rentContractRepository.getbranchIds().stream().sorted().distinct().collect(Collectors.toList()))
				.error(Boolean.FALSE).msg("Get All Branch IDS base on contract master..!").build());
	}

	@GetMapping("getBranchName")
	public ResponseEntity<Responce> getBranchNames() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder()
						.data(rentContractRepository.getbranchNames().stream().sorted().collect(Collectors.toList()))
						.error(Boolean.FALSE).msg("All Brannches").build());
	}

	/**
	 * NON USE API
	 * 
	 * @param sdrecord
	 * @throws ParseException
	 */
	@PostMapping("setsd")
	public ResponseEntity<Responce> getSd(@RequestBody SDRecoardDto sdrecord) throws ParseException {
		Optional<provision> optionalProvision = provisionRepository
				.findById(sdrecord.getContractID() + "-" + sdrecord.getMonth() + "/" + sdrecord.getYear());
		if (optionalProvision.isPresent())
			return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(optionalProvision.get())
					.error(Boolean.TRUE).msg(" Cant't make SD provision Already Exist").build());

		RentContract rentContract = rentContractRepository.findById(sdrecord.getContractID()).get();
		// If month Year not match Send conflict Error in ResponSe
		if (LocalDate.now().isAfter(rentContract.getRentEndDate())) {
			ResponseEntity<Responce> responce = addprovison("REVERSED",
					provisionDto.builder().branchID(rentContract.getBranchID())
							.contractID(sdrecord.getContractID() + "").dateTime(LocalDate.now())
							.month(sdrecord.getMonth()).provisionAmount(sdrecord.getSdAmount())
							.provisiontype("REVERSED").remark("SD RETURN" + sdrecord.getRemark())
							.year(sdrecord.getYear()).build());
			if (responce.getStatusCode() == HttpStatus.CONFLICT)
				return ResponseEntity.status(HttpStatus.OK)
						.body(Responce.builder().data(null).error(Boolean.FALSE).msg("SD SETTLEMENT FAIL..!").build());
			else
				return ResponseEntity.status(HttpStatus.OK)
						.body(Responce.builder().data(null).error(Boolean.FALSE).msg("SD SETTLEMENT DONE..!").build());
		} else {
			return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(null).error(Boolean.TRUE)
					.msg("Cant't make SD [RENT END DATE CONFLICT]").build());
		}
	}

	@PostMapping("makeactual")
	public ResponseEntity<Responce> makeActual(@RequestBody List<MakeActualDto> ActualDto) {
		Map<String, String> responce = rentService.makeactual(ActualDto);
		// ---API CALL RECORD SAVE---
		try {
			apirecords.save(ApiCallRecords.builder().apiname("makeactual")
					.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
					.msg(ActualDto.toString() + "Responce=>" + responce.toString()).build());
		} catch (Exception e) {
			// --> IF MSG Field Large.
			apirecords.save(ApiCallRecords.builder().apiname("makeactual")
					.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
					.msg(ActualDto.toString()).build());
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data(responce).error(Boolean.FALSE).msg("Actual Done").build());
	}

	/**
	 * @API -> TO make Provision or Reverse Provision
	 */
	@PostMapping("/setprovision")
	public ResponseEntity<Responce> addprovison(@RequestParam String provisionType,
			@RequestBody provisionDto provisionDto) throws ParseException {

		Optional<provision> optionalProvision = provisionRepository
				.findById(provisionDto.getContractID() + "-" + provisionDto.getMonth() + "/" + provisionDto.getYear());
		if (optionalProvision.isPresent())
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Responce.builder().data(optionalProvision.get())
					.error(Boolean.TRUE).msg("provision Already Exist").build());

		provisionDto pDto = rentService.addprovision(provisionType, provisionDto);
		// ---API CALL RECORD SAVE---
		apirecords.save(ApiCallRecords.builder().apiname("setprovision")
				.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
				.msg(pDto.toString()).build());

		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data(pDto).error(Boolean.FALSE).msg("provision Added").build());
	}

	/**
	 * @API -> TO Get Provision or Reverse Provision
	 */
	@GetMapping("/getprovision")
	public ResponseEntity<Responce> getprovision(@RequestParam String flag, @RequestParam String year) {
		Responce responce = rentService.getprovision(flag, year);

		return ResponseEntity.status(HttpStatus.OK).body(responce);
	}

//	@Transactional
	@DeleteMapping("deleteProvision")

	public String deleteProvision(@RequestParam String contractID, @RequestParam int year, @RequestParam String month) {
		try {
			if (!(LocalDate.now().getMonth() + "").equalsIgnoreCase(month) && LocalDate.now().getYear() != year)
				throw new RuntimeException("ALLOWED ONLY FOR CURRENT MONTH&YEAR");
			provision provision = provisionRepository.findByContractIDAndYearAndMonth(contractID, year, month);
			provisionRepository.delete(provision);
			// -----------------------------------------
			// ---API CALL RECORD SAVE---
			apirecords.save(ApiCallRecords.builder().apiname("Delete Provision")
					.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
					.msg(provision.toString()).build());

			/**
			 * ---Delete Record from Payment Report--- AFTER THAT.
			 * 
			 * @ones We make Again Actual with '--' updated monthly report & Delete Variance
			 *       in @Payment_Report_Table
			 **/
			// ------Reset Actual & TDS Value & Variance---------
			List<MakeActualDto> dto = new ArrayList<>();
			dto.add(MakeActualDto.builder().amount("--").month(month).year(year)
					.contractID(Integer.parseInt(contractID)).build());
			makeActual(dto);
			// ----------Generate Payment Report-----
			generatePaymentReport(contractID, month, year + "", "make");
			return "PROVISION DELETION DONE " + contractID + "-" + month + "/" + year;
		} catch (Exception e) {
			return "PROVISION DELETION FAILED" + contractID + "-" + month + "/" + year + "[ " + e.getMessage() + " ]";
		}
	}

	@PostMapping("/BulkProvisionDelete")
	public ResponseEntity<Responce> bulkpProvisionDelete(
			@RequestBody List<BulkProvisionDeletion> bulkProvisionDeletion) {
		// ---API CALL RECORD SAVE---
		apirecords.save(ApiCallRecords.builder().apiname("BulkProvisionDelete")
				.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
				.msg(bulkProvisionDeletion.toString()).build());

		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data(bulkProvisionDeletion.stream().map(e -> {
					return deleteProvision(e.getContractID(), e.getYear(), e.getMonth());
				}).collect(Collectors.toList())).msg("Bulk provision Deleted...!").error(Boolean.FALSE).build());
	}

	@PostMapping("/getenure")
	public ResponseEntity<List<Long>> getTenure(@RequestBody TenureDto data) {
		long tenure = ChronoUnit.MONTHS.between(data.getStartDate(), data.getEndDate()) + 1;
		ArrayList<Long> tenuredata = new ArrayList<Long>();
		tenuredata.add(tenure);
		return ResponseEntity.status(HttpStatus.OK).body(tenuredata);
	}

	/**
	 * @param contractID-> ALL AND CID(7000)
	 * @param month
	 * @param year
	 * @param purpose      -> view(Don't hit actual_API & make(Hit Actual_API)
	 * @return
	 */
	@GetMapping("/generatePaymentReport")
	public ResponseEntity<Responce> generatePaymentReport(@RequestParam String contractID, @RequestParam String month,
			@RequestParam String year, @RequestParam String purpose) {
		Responce responce = rentService.getPaymentReport(contractID, month, year, purpose);
		return ResponseEntity.status(HttpStatus.OK).body(responce);
	}

	@Autowired
	RawPaymentReportRepository rawPaymentReportRepository;

	@GetMapping("/generateRawPaymentReport")
	public ResponseEntity<Responce> generateRawPaymentReport(@RequestParam String contractID,
			@RequestParam String month, @RequestParam String year) {
		List<RawPaymentReport> data = rawPaymentReportRepository.findByMonthAndYear(month, year);
		List<PaymentReportDto> prDto = new ArrayList<>();
		if (data != null) {

			data.stream().forEach(e -> {
				RentContractDto rentContractDto = new RentContractDto();
				BeanUtils.copyProperties(e.getContractInfo(), rentContractDto);
				PaymentReportDto PRDTo = PaymentReportDto.builder().actualAmount(e.getActualAmount()).due(e.getDue())
						.Gross(e.getGross()).gstamt(e.getGST() + "").Info(rentContractDto).monthRent(e.getMonthlyRent())
						.monthYear(e.getMonth() + "/" + e.getYear()).net(e.getNet() + "")
						.provision(e.getProvision() + "").reporttds(e.getTds() + "").build();
				try {
					if (Double.parseDouble(e.getActualAmount()) != e.getGross())
						PRDTo.setPaymentFlag(false);
					else
						PRDTo.setPaymentFlag(true);
				} catch (Exception e2) {
					// TODO: handle exception
				}

				prDto.add(PRDTo);
			});
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data(prDto).msg("Raw Data fetched").error(Boolean.FALSE).build());
	}

	@GetMapping("getduereportUid") // Base on UniqueID
	public ResponseEntity<Responce> getDueReport(@RequestParam String value) {
		List<RentDue> getrentdue = dueRepository.getrentdue(value);
		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(getrentdue.stream().map(e -> {
			RentDueDto rentDue = new RentDueDto();
			BeanUtils.copyProperties(e, rentDue);
			RentContract rentContract = rentContractRepository.findById(e.getContractID()).get();
			rentDue.setStatus(rentContract.getAgreementActivationStatus());
			RentContractDto dto = new RentContractDto();
			BeanUtils.copyProperties(rentContract, dto);
			rentDue.setInfo(dto);
			rentDue.setStatus(dto.getAgreementActivationStatus());
			return rentDue;
		})).error(Boolean.FALSE).msg("Due Report Fetch").build());
	}

	@GetMapping("getduereportBid") // Base on BranchID
	public ResponseEntity<Responce> getDueReport1(@RequestParam String value) {
		List<RentDue> getrentdue = dueRepository.getrentdue1(value);
		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(getrentdue.stream().map(e -> {
			RentDueDto rentduedto = new RentDueDto();
			BeanUtils.copyProperties(e, rentduedto);
			RentContract rentContract = rentContractRepository.findById(e.getContractID()).get();
			rentduedto.setStatus(rentContract.getAgreementActivationStatus());
			RentContractDto dto = new RentContractDto();
			BeanUtils.copyProperties(rentContract, dto);
			rentduedto.setInfo(dto);
			return rentduedto;
		})).error(Boolean.FALSE).msg("Due Report Fetch").build());
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
	public ResponseEntity<Responce> getBranchDetails(@RequestParam String BranchID) {

		Optional<RfBranchMaster> rfdata = rfBrachRepository.findById(BranchID);
		if (rfdata.isPresent()) {
			RfBranchMaster rfBranchMaster = rfdata.get();
			RfBranchmasterDto build = RfBranchmasterDto.builder().amContactNumber(rfBranchMaster.getAmContactNumber())
					.areaName(rfBranchMaster.getAreaName()).branchName(rfBranchMaster.getBranchName())
					.region(rfBranchMaster.getRegion()).rfBranchID(rfBranchMaster.getRfBranchID()).build();
			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().error(Boolean.FALSE).data(build).msg("Data present..!").build());
		} else {
			Optional<BranchDetail> glData = branchDetailRepository.findById(BranchID);
			if (glData.isPresent()) {
				BranchDetail branchDetail = glData.get();
				BranchDto build = BranchDto.builder().areaName(branchDetail.getAreaName())
						.branchID(branchDetail.getBranchID()).branchName(branchDetail.getBranchName())
						.region(branchDetail.getRegion()).state(branchDetail.getState()).zone(branchDetail.getZone())
						.build();
				return ResponseEntity.status(HttpStatus.OK)
						.body(Responce.builder().data(build).msg("Branch Data Exist...!").error(Boolean.FALSE).build());
			}
			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().data(null).msg("Branch Data Not Exist...!").error(Boolean.TRUE).build());
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
										responceData.setRentStartDate(e.getRentEndDate().plusDays(1));
										responceData.setRentEndDate(e.getRentEndDate().plusMonths(11));
										List<RecipiantDto> recipiants = new ArrayList<>();
										recipiants.add(
												RecipiantDto.builder().lessorAccountNumber(e.getLessorAccountNumber())
														.lessorBankName(e.getLessorBankName())
														.lessorBranchName(e.getLessorBranchName())
														.lessorIfscNumber(e.getLessorIfscNumber())
														.lessorRecipiantsName(e.getLessorRecipiantsName())
														.lessorRentAmount(e.getLessorRentAmount())
														.panNo(e.getLessorPanNumber()).build());
										responceData.setRecipiants(recipiants);
										return responceData;
									}))
							.msg("Branch Details").build());

		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Responce.builder().error(Boolean.TRUE).data(null).msg("Branch Details").build());

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
				rentContract.setUniqueID((ids.get(ids.size() - 1) + 1)); // Input value type is Integer.

			rentContract.setLessorRecipiantsName(data.getLessorRecipiantsName());
			rentContract.setLessorBankName(data.getLessorBankName());
			rentContract.setLessorBranchName(data.getLessorBranchName());
			rentContract.setLessorIfscNumber(data.getLessorIfscNumber());
			rentContract.setLessorAccountNumber(data.getLessorAccountNumber());
			rentContract.setPanNo(data.getPanNo());
			rentContract.setGstNo(data.getGstNo());
			rentContract.setLessorRentAmount(data.getLessorRentAmount());
			rentContract.setMonthlyRent(data.getLessorRentAmount());
			// ====Dummy Value Set void Error in Excel Download.!====
//			rentContract.setPriviousContractID(0);
			// =====IF Any Error found then-> By Default Escalated Month Value is 11=====
			try {
				double escalatedMonth = Double.parseDouble(rentContractDto.getSchedulePrimesis());
				rentContract.setSchedulePrimesis(escalatedMonth + "");
			} catch (Exception e) {
				// TODO: handle exception
				rentContract.setSchedulePrimesis("11");
			}

			RentContract save = rentContractRepository.save(rentContract);
			responceData.add(save);

			if (save != null) {
				createRentdue(
						Rentduecalculation.builder().escalatedMonth(Double.parseDouble(save.getSchedulePrimesis()))
								.branchID(save.getBranchID()).contractID(save.getUniqueID())
								.escalation(save.getEscalation()).lesseeBranchType(save.getLesseeBranchType())
								.monthlyRent(save.getLessorRentAmount()).renewalTenure(save.getAgreementTenure())
								.rentEndDate(save.getRentEndDate()).rentStartDate(save.getRentStartDate()).build());
			}

		});
		return ResponseEntity.status(HttpStatus.OK).body(
				Responce.builder().data(responceData).msg("Data Added Sucessfully...!").error(Boolean.FALSE).build());
	}

	/**
	 * EDIT CONTRACT -> IF RENTEND DATE CHANGED -> RENT DUE ALL RECALCULATE BASE ON
	 * UPDATED RENT END DATA..!
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
				|| !rentContract.getRentEndDate().toString().equalsIgnoreCase(contractDto.getRentEndDate().toString())
				|| contractDto.getLessorRentAmount() != rentContract.getLessorRentAmount()
				|| !rentContract.getSchedulePrimesis().equalsIgnoreCase(contractDto.getSchedulePrimesis())) {
			/*
			 * @NOTE:->In above condition [SchedulePrimesis] field is use As a Escalated
			 * Month for Rent_Due Calculation
			 */
//			List<RentDue> unusedDueData = dueRepository.getUnusedDueData(uniqueID + "");
//			unusedDueData.stream().forEach(due -> {
//				dueRepository.delete(due);
//			});
//			flagCheck = true;// if (True) Changes done in RentDue orElse no need to change any thing
		}
		BeanUtils.copyProperties(contractDto, rentContract);
		rentContract.setUniqueID(uniqueID);
		RentContract save = rentContractRepository.save(rentContract);
		if (save != null & flagCheck) {
			// ---API CALL RECORD SAVE---
			apirecords.save(ApiCallRecords.builder().apiname("modifyRentDue")
					.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
					.msg(save.getUniqueID() + "/" + save.getMonthlyRent()).build());

//			createRentdue(Rentduecalculation.builder().branchID(save.getBranchID())
//					.escalatedMonth(Double.parseDouble(save.getSchedulePrimesis())).contractID(save.getUniqueID())
//					.escalation(save.getEscalation()).lesseeBranchType(save.getLesseeBranchType())
//					.monthlyRent(save.getLessorRentAmount()).renewalTenure(save.getAgreementTenure())
//					.rentEndDate(save.getRentEndDate()).rentStartDate(save.getRentStartDate()).build());
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().error(Boolean.FALSE).msg("Edit Sucessfully..!").data(contractDto).build());
	}

	/**
	 * Get contract Details Base on BranchID.->List
	 * 
	 * @param branchID
	 * @return list of Contract
	 */
	@GetMapping("/getcontracts")
	public ResponseEntity<Responce> getContractsBID(@RequestParam String branchID) {
		List<RentContractDto> contractInfos = new ArrayList<>();
		List<RentContract> allContractDetalis = rentContractRepository.findByBranchID(branchID);
		if (!allContractDetalis.isEmpty()) {
			allContractDetalis.stream().forEach(contractInfo -> {
				RentContractDto contractDto = new RentContractDto();
				BeanUtils.copyProperties(contractInfo, contractDto);
				contractInfos.add(contractDto);
			});
			return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().error(Boolean.FALSE)
					.msg("All Contracts Details fetch..!").data(contractInfos.stream()
							.sorted(Comparator.comparing(RentContractDto::getUniqueID)).collect(Collectors.toList()))
					.build());
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().error(Boolean.TRUE).msg("Contracts Data Not present..!").data(null).build());
	}

	/**
	 * Get contract Details Base on contract ID->1 Object
	 * 
	 * @param ContractID
	 * @return contract Object ->count 1
	 */
	@GetMapping("/getcontractsCID")
	public ResponseEntity<Responce> getContractCID(@RequestParam int ContractID) {
		Optional<RentContract> contractData = rentContractRepository.findById(ContractID);
		if (contractData.isPresent()) {
			RentContractDto contractDto = new RentContractDto();
			BeanUtils.copyProperties(contractData.get(), contractDto);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().data(contractDto).error(Boolean.FALSE).msg("Data Fetched..!").build());
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Responce.builder().data(null).error(Boolean.TRUE).msg("INCORRECT CONTRACT_ID.!").build());
	}

	/**
	 * GET ALL CONTRACT BASE ON BRANCH NAME..!
	 * 
	 * @param branchName
	 * @return
	 */
	@GetMapping("/getcotractBranchName")
	public ResponseEntity<Responce> getAllContrtactBaseOnBranchName(@RequestParam String branchName) {
		List<RentContractDto> contractInfos = new ArrayList<>();
		List<RentContract> allContractDetalis = rentContractRepository.findAll();
		if (!allContractDetalis.isEmpty()) {
			allContractDetalis.stream()
					.filter(data -> data.getLesseeBranchName().trim().equalsIgnoreCase(branchName.trim()))
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

	/**
	 * GET ALL CONTRACT BASE ON DISTRICT NAME.
	 * 
	 * @param district
	 * @return
	 */
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

	// =======================GENERATE FLAG DATE ===========================

	public static LocalDate getFlagDate(String month, int year, String startORend) throws ParseException {
		String monthInput = month;
		SimpleDateFormat sdfInput = new SimpleDateFormat("MMMM");
		Date date = sdfInput.parse(monthInput);
		java.util.Calendar calendar = java.util.Calendar.getInstance();
		calendar.setTime(date);
		int monthValue = calendar.get(java.util.Calendar.MONTH) + 1;
		// Convert to two-digit string with leading zeros
		String monthValueString = String.format("%02d", monthValue);
		if (startORend.equalsIgnoreCase("start"))
			return LocalDate.parse(year + "-" + monthValueString + "-01");
		else {
			LocalDate tempDate = LocalDate.parse(year + "-" + monthValueString + "-01");
			return tempDate.with(TemporalAdjusters.lastDayOfMonth());
		}
	}

	// =======================GENERATE BULK DUE ===========================
	/**
	 * @Api is only use for BackEnd Bulk Remaining RentDue Calculation Purpose..!
	 * @only one Time Used While After DataDump Process
	 */
//	@GetMapping("makeDue")
	public Boolean generateRentDue() {
		// Get all pending Due Calculation ContractIDs
		List<RentContract> allcontract = rentContractRepository.getduemakerIDs();
		allcontract.stream().map(e -> {
			return Rentduecalculation.builder().branchID(e.getBranchID())
					.escalatedMonth(Integer.parseInt(e.getSchedulePrimesis())).contractID(e.getUniqueID())
					.escalation(e.getEscalation()).lesseeBranchType(e.getLesseeBranchType())
					.monthlyRent(e.getMonthlyRent()).renewalTenure(e.getAgreementTenure())
					.rentEndDate(e.getRentEndDate()).rentStartDate(e.getRentStartDate()).build();
		}).collect(Collectors.toList()).stream().forEach(data -> {
			createRentdue(data);
		});
		return true;
	}

	// =====MODIFY PAYMENTREPORT TABLE AT 10:30(PM)& 2:30(AM) EVERY NIGHT=======

	@Scheduled(cron = "0 30 22 * * ?") // 10:30(PM)
	public void apiCall1() throws ParseException {
		modifyPaymentReport(LocalDate.now().getMonth() + "", LocalDate.now().getYear() + "");
	}

	@Scheduled(cron = "0 30 2 * * ?") // 2:30(PM)
	public void apiCall2() throws ParseException {
		if (LocalDate.now().getDayOfMonth() == 1)// only execute month start
			modifyPaymentReport(LocalDate.now().getMonth() + "", LocalDate.now().getYear() + "");
	}

	@PostMapping("/ModifyPaymentReport")
	public Set<String> modifyPaymentReport(@RequestParam String month, @RequestParam String year)
			throws ParseException {
		// ---API CALL RECORD SAVE---
		apirecords.save(ApiCallRecords.builder().apiname("ModifyPaymentReport")
				.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now())).build());
		// -----Here We Delete All Current Month Record..!-----
		List<PaymentReport> ExistingRecord = paymentReportRepository.findByMonthAndYear(month, year);
		try {
			paymentReportRepository.deleteAll(ExistingRecord);
		} catch (Exception e) {
		}
		// To avoid unused contract object flag date is use in Query.
		LocalDate SflagDate = getFlagDate(month, Integer.parseInt(year), "Start");
		LocalDate EflagDate = getFlagDate(month, Integer.parseInt(year), "End");
//		System.out.println(SflagDate);
//		System.out.println(EflagDate);

		List<String> getcontractIDs = rentContractRepository.getcontractIDs(SflagDate + "", EflagDate + "");
//      some time out of Start_End Date also payment is there -> Query Use to take Actual Table & provision_Table CID.

//		String Query="SELECT contractid FROM rent_actual where year="+year+" and "+ month+"!='--' and agreement_activation_status ='Open';";
//		List<String> getcontractIDsActualTable = jdbcTemplate.queryForList(Query,String.class );

		String ProvisionIDsQuery = "SELECT contractid FROM provision where month='" + month + "' and year=" + year + "";
		List<String> ProvisionIDs = jdbcTemplate.queryForList(ProvisionIDsQuery, String.class);
		// Used set to avoid DublicateIds
		Set<String> IDs = new HashSet<>();
		IDs.addAll(getcontractIDs);
//		IDs.addAll(getcontractIDsActualTable);
		IDs.addAll(ProvisionIDs);

		if (IDs != null & !IDs.isEmpty())
			IDs.stream().forEach(cID -> {
				// HERE WE MODIFY PAYMENT REPORT
				generatePaymentReport(cID, month, year, "make");
			});

		return IDs;// return null to Exit..!
	}

	// ================DUE CLCULATION LOGIC=================

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
		LocalDate escalationApplyDate = rentStartDate.plusMonths((long) data.getEscalatedMonth());

		for (int y = rentStartDate.getYear(); y <= rentEndDate.getYear(); y++) {
			RentDue due = new RentDue();
			due.setRentDueID(data.getBranchID() + "-" + data.getContractID() + "-" + y);
			due.setYear(y);
			due.setContractID(data.getContractID());
			due.setEscalation(escalationPercent);
			due.setStartDate(rentStartDate);
			due.setEndDate(rentEndDate);

			if (y == startYear) {
				int monthValue = 12;
				if (rentEndDate.getYear() == rentStartDate.getYear()) {
					monthValue = rentEndDate.getMonthValue();
				}
				for (int m = rentStartDate.getMonthValue(); m <= monthValue; m++) {
					switch (m) {
					case 1:
						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()
								&& m != rentEndDate.getMonthValue()) {
							double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
									* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
							double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
							due.setJanuary((int) Math.round(rentafter + rentbefor));
							// Here Monthly rent modify as per Escalation.
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {

							if (m == rentStartDate.getMonthValue())
								due.setFebruary((int) Math
										.round((monthlyRent / day) * ((day - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue() & y == rentEndDate.getYear())
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setMarch((int) Math
										.round((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue() & y == rentEndDate.getYear())
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {

							if (m == rentStartDate.getMonthValue())
								due.setApril((int) Math
										.round((monthlyRent / 30) * ((30 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue() & y == rentEndDate.getYear())
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setMay((int) Math
										.round((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue() & y == rentEndDate.getYear())
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setJune((int) Math
										.round((monthlyRent / 30) * ((30 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue() & y == rentEndDate.getYear())
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setJuly((int) Math
										.round((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue() & y == rentEndDate.getYear())
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setAugust((int) Math
										.round((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue() & y == rentEndDate.getYear())
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setSeptember((int) Math
										.round((monthlyRent / 30) * ((30 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue() & y == rentEndDate.getYear())
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setOctober((int) Math
										.round((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue() & y == rentEndDate.getYear())
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setNovember((int) Math
										.round((monthlyRent / 30) * ((30 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue() & y == rentEndDate.getYear())
								due.setNovember((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
							else
								due.setNovember((int) Math.round(monthlyRent));
						}

						break;
					case 12:

						if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {

							if (m == rentEndDate.getMonthValue() & y == rentEndDate.getYear()) {
								/**
								 * This is additional Scenario for escalation apply for start date with 01-JAN
								 */
								if (rentStartDate.getDayOfMonth() == 01)
									due.setDecember((int) Math
											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
													* rentEndDate.getDayOfMonth()));
								else
									due.setDecember(
											(int) Math.round(((monthlyRent) / 31) * rentEndDate.getDayOfMonth()));

							} else {
								double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
										* ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
								double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
								due.setDecember((int) Math.round(rentafter + rentbefor));
								// Here Monthly rent modify as per Escalation.
								escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
								monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
							}
						} else {
							if (m == rentStartDate.getMonthValue())
								due.setDecember((int) Math
										.round((monthlyRent / 31) * ((31 - rentStartDate.getDayOfMonth()) + 1)));
							else if (m == rentEndDate.getMonthValue() & y == rentEndDate.getYear())
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue()) {

								if (escalationApplyDate.isAfter(rentEndDate)
										|| escalationApplyDate.isEqual(rentEndDate)) {
									due.setJanuary((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								} else {
									double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent))
											/ 31) * ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
									double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
									due.setJanuary((int) Math.round(rentafter + rentbefor));
								}

								// ============================
//								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
//									due.setJanuary((int) Math
//											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
//													* rentEndDate.getDayOfMonth()));
//								} else {
//									due.setJanuary((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
//								}
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {

							if (m == rentEndDate.getMonthValue()) {

								if (escalationApplyDate.isAfter(rentEndDate)
										|| escalationApplyDate.isEqual(rentEndDate)) {
									due.setFebruary(
											(int) Math.round((monthlyRent / day) * rentEndDate.getDayOfMonth()));
								} else {
									double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent))
											/ day) * ((day - (escalationApplyDate.getDayOfMonth()) + 1));
									double rentbefor = (monthlyRent / day) * (escalationApplyDate.getDayOfMonth() - 1);
									due.setFebruary((int) Math.round(rentafter + rentbefor));
								}
								// ======================
//								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
//
//									due.setFebruary((int) Math
//											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / day)
//													* rentEndDate.getDayOfMonth()));
//								} else {
//									due.setFebruary(
//											(int) Math.round((monthlyRent / day) * rentEndDate.getDayOfMonth()));
//								}
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue()) {
								if (escalationApplyDate.isAfter(rentEndDate)
										|| escalationApplyDate.isEqual(rentEndDate)) {
									due.setMarch((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								} else {
									double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent))
											/ 31) * ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
									double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
									due.setMarch((int) Math.round(rentafter + rentbefor));
								}

//								if (y == escalationApplyDate.getYear() && m == escalationApplyDate.getMonthValue()) {
//
//									due.setMarch((int) Math
//											.round(((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent)) / 31)
//													* rentEndDate.getDayOfMonth()));
//								} else {
//									due.setMarch((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
//								}
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {

							if (m == rentEndDate.getMonthValue()) {

								if (escalationApplyDate.isAfter(rentEndDate)
										|| escalationApplyDate.isEqual(rentEndDate)) {
									due.setApril((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
								} else {
									double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent))
											/ 30) * ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
									double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
									due.setApril((int) Math.round(rentafter + rentbefor));
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue()) {

								if (escalationApplyDate.isAfter(rentEndDate)
										|| escalationApplyDate.isEqual(rentEndDate)) {
									due.setMay((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								} else {
									double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent))
											/ 31) * ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
									double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
									due.setMay((int) Math.round(rentafter + rentbefor));
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue()) {
								if (escalationApplyDate.isAfter(rentEndDate)
										|| escalationApplyDate.isEqual(rentEndDate)) {
									due.setJune((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
								} else {
									double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent))
											/ 30) * ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
									double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
									due.setJune((int) Math.round(rentafter + rentbefor));
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue()) {
								if (escalationApplyDate.isAfter(rentEndDate)
										|| escalationApplyDate.isEqual(rentEndDate)) {
									due.setJuly((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								} else {
									double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent))
											/ 31) * ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
									double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
									due.setJuly((int) Math.round(rentafter + rentbefor));
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue()) {
								if (escalationApplyDate.isAfter(rentEndDate)
										|| escalationApplyDate.isEqual(rentEndDate)) {
									due.setAugust((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								} else {
									double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent))
											/ 31) * ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
									double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
									due.setAugust((int) Math.round(rentafter + rentbefor));
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue()) {
								if (escalationApplyDate.isAfter(rentEndDate)
										|| escalationApplyDate.isEqual(rentEndDate)) {
									due.setSeptember(
											(int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
								} else {
									double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent))
											/ 30) * ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
									double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
									due.setSeptember((int) Math.round(rentafter + rentbefor));
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue()) {
								if (escalationApplyDate.isAfter(rentEndDate)
										|| escalationApplyDate.isEqual(rentEndDate)) {
									due.setOctober((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								} else {
									double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent))
											/ 31) * ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
									double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
									due.setOctober((int) Math.round(rentafter + rentbefor));
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue()) {
								if (escalationApplyDate.isAfter(rentEndDate)
										|| escalationApplyDate.isEqual(rentEndDate)) {
									due.setNovember((int) Math.round((monthlyRent / 30) * rentEndDate.getDayOfMonth()));
								} else {
									double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent))
											/ 30) * ((30 - (escalationApplyDate.getDayOfMonth()) + 1));
									double rentbefor = (monthlyRent / 30) * (escalationApplyDate.getDayOfMonth() - 1);
									due.setNovember((int) Math.round(rentafter + rentbefor));
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
							monthlyRent = (monthlyRent + ((escalationPercent / 100.0f) * monthlyRent));
						} else {
							if (m == rentEndDate.getMonthValue()) {
								if (escalationApplyDate.isAfter(rentEndDate)
										|| escalationApplyDate.isEqual(rentEndDate)) {
									due.setDecember((int) Math.round((monthlyRent / 31) * rentEndDate.getDayOfMonth()));
								} else {
									double rentafter = ((monthlyRent + ((escalationPercent / 100.0f) * monthlyRent))
											/ 31) * ((31 - (escalationApplyDate.getDayOfMonth()) + 1));
									double rentbefor = (monthlyRent / 31) * (escalationApplyDate.getDayOfMonth() - 1);
									due.setDecember((int) Math.round(rentafter + rentbefor));
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
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
							escalationApplyDate = escalationApplyDate.plusMonths((long) data.getEscalatedMonth());
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

	int rno = 1;// To handle local static scope we declared as a Global

	@GetMapping("/DownloadPaymentReport")
	public ResponseEntity<InputStreamResource> ExcelDownload(@RequestParam String month, @RequestParam String year)
			throws IOException {
		List<PaymentReport> collect = paymentReportRepository.findByMonthAndYear(month, year);
		if (!collect.isEmpty() & collect != null) {
			List<PaymentReport> data = collect.stream()
					.sorted(Comparator.comparing(PaymentReport::getContractID).reversed()).collect(Collectors.toList());
			// Create Excel Structure
			SXSSFWorkbook workBook = new SXSSFWorkbook();
			SXSSFSheet sheet = workBook.createSheet("My Data");
			// custom text
			Font font = workBook.createFont();
			font.setFontName("Arial");
			font.setBold(false);
			font.setColor(IndexedColors.WHITE.getIndex());
			CellStyle cellStyle = workBook.createCellStyle();
			cellStyle.setFont(font);
			cellStyle.setFillForegroundColor(IndexedColors.BLACK.getIndex());
			cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			// create header row

			SXSSFRow header = sheet.createRow(0);// SXSSFSheet

			SXSSFCell cell0 = header.createCell(0);
			cell0.setCellStyle(cellStyle);
			cell0.setCellValue("Contract_ID");

			SXSSFCell cell2 = header.createCell(1);
			cell2.setCellStyle(cellStyle);
			cell2.setCellValue("Status");

			SXSSFCell cell = header.createCell(2);
			cell.setCellStyle(cellStyle);
			cell.setCellValue("Branch_ID");

			SXSSFCell cellu = header.createCell(3);
			cellu.setCellStyle(cellStyle);
			cellu.setCellValue("Office_Name");

			SXSSFCell cell3 = header.createCell(4);
			cell3.setCellStyle(cellStyle);
			cell3.setCellValue("lessor_Name");

			SXSSFCell cell4 = header.createCell(5);
			cell4.setCellStyle(cellStyle);
			cell4.setCellValue("Bank_Name");

			SXSSFCell cell5 = header.createCell(6);
			cell5.setCellStyle(cellStyle);
			cell5.setCellValue("IFSC");

			SXSSFCell cell6 = header.createCell(7);
			cell6.setCellStyle(cellStyle);
			cell6.setCellValue("Account_Number");

			SXSSFCell cell7 = header.createCell(8);
			cell7.setCellStyle(cellStyle);
			cell7.setCellValue("Rent_Start_Date");

			SXSSFCell cell8 = header.createCell(9);
			cell8.setCellStyle(cellStyle);
			cell8.setCellValue("Rent_End_Date");

			SXSSFCell cell9 = header.createCell(10);
			cell9.setCellStyle(cellStyle);
			cell9.setCellValue("Initial_MonthRent");

			SXSSFCell cell10 = header.createCell(11);
			cell10.setCellStyle(cellStyle);
			cell10.setCellValue("Current_MonthRent");

			SXSSFCell cell11 = header.createCell(12);
			cell11.setCellStyle(cellStyle);
			cell11.setCellValue("Due");

			SXSSFCell cell12 = header.createCell(13);
			cell12.setCellStyle(cellStyle);
			cell12.setCellValue("Provision");

			SXSSFCell cell13 = header.createCell(14);
			cell13.setCellStyle(cellStyle);
			cell13.setCellValue("Gross");

			SXSSFCell cell14 = header.createCell(15);
			cell14.setCellStyle(cellStyle);
			cell14.setCellValue("TDS");

			SXSSFCell cell15 = header.createCell(16);
			cell15.setCellStyle(cellStyle);
			cell15.setCellValue("NET");

			SXSSFCell cell16 = header.createCell(17);
			cell16.setCellStyle(cellStyle);
			cell16.setCellValue("GST");

			SXSSFCell cell17 = header.createCell(18);
			cell17.setCellStyle(cellStyle);
			cell17.setCellValue("Actual Amount");

			SXSSFCell cell18 = header.createCell(19);
			cell18.setCellStyle(cellStyle);
			cell18.setCellValue("Remarks");

			// added row
			// style row
			CellStyle style = workBook.createCellStyle();
			style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
			style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			CellStyle style1 = workBook.createCellStyle();
			style1.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
			style1.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			CellStyle cellstyle = workBook.createCellStyle();
			cellstyle.setFillForegroundColor(IndexedColors.RED.getIndex());
			cellstyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			rno = 1;
			List<PaymentReport> doneList = new ArrayList<>();
			List<PaymentReport> tempL = new ArrayList<>();
			data.stream().forEach(D -> {
				if (!doneList.contains(D)) {
					// clear temp_list
					tempL.clear();
					if (data.stream().anyMatch(obj -> obj.getContractID()
							.equalsIgnoreCase(D.getContractInfo().getPriviousContractID() + ""))) {

						tempL.add(D);
						PaymentReport linkedcontract = data.stream()
								.filter(id -> id.getContractID()
										.equalsIgnoreCase(D.getContractInfo().getPriviousContractID() + ""))
								.collect(Collectors.toList()).get(0);

						tempL.add(linkedcontract);
						doneList.add(linkedcontract);
						doneList.add(D);

						tempL.stream().forEach(item -> {
							SXSSFRow row = sheet.createRow(rno++);
							row.setRowStyle(style);
							row.createCell(0).setCellValue(item.getContractID());
							row.createCell(1).setCellValue(item.getContractInfo().getAgreementActivationStatus());
							row.createCell(2).setCellValue(item.getBranchID());
							row.createCell(3).setCellValue(item.getContractInfo().getLesseeBranchName());
							row.createCell(4).setCellValue(item.getContractInfo().getLessorName());
							row.createCell(5).setCellValue(item.getContractInfo().getLessorBankName());
							row.createCell(6).setCellValue(item.getContractInfo().getLessorIfscNumber());
							row.createCell(7).setCellValue(item.getContractInfo().getLessorAccountNumber());
							row.createCell(8).setCellValue(item.getContractInfo().getRentStartDate() + "");
							row.createCell(9).setCellValue(item.getContractInfo().getRentEndDate() + "");
							row.createCell(10).setCellValue(item.getContractInfo().getLessorRentAmount());
							row.createCell(11).setCellValue(item.getMonthlyRent());
							row.createCell(12).setCellValue(item.getDue());
							row.createCell(13).setCellValue(item.getProvision());
							row.createCell(14).setCellValue(item.getGross());
							row.createCell(15).setCellValue(item.getTds());
							row.createCell(16).setCellValue(item.getNet());
							row.createCell(17).setCellValue(item.getGST());
							SXSSFCell actualcell = row.createCell(18);
							if (item.isRedflag()) {
//								actualcell.setCellStyle(cellstyle);
							}
							actualcell.setCellValue(item.getActualAmount());
						});
						SXSSFRow row = sheet.createRow(rno++);
						row.setRowStyle(style1);
						row.createCell(0).setCellValue(linkedcontract.getContractID() + "|" + D.getContractID());
						row.createCell(1).setCellValue(D.getContractInfo().getAgreementActivationStatus());
						row.createCell(2).setCellValue(D.getBranchID());
						row.createCell(3).setCellValue(D.getContractInfo().getLesseeBranchName());
						row.createCell(4).setCellValue(D.getContractInfo().getLessorName());
						row.createCell(5).setCellValue(D.getContractInfo().getLessorBankName());
						row.createCell(6).setCellValue(D.getContractInfo().getLessorIfscNumber());
						row.createCell(7).setCellValue(D.getContractInfo().getLessorAccountNumber());
						row.createCell(8).setCellValue(linkedcontract.getContractInfo().getAgreementStartDate() + "|"
								+ D.getContractInfo().getRentStartDate());
						row.createCell(9).setCellValue(linkedcontract.getContractInfo().getAgreementEndDate() + "|"
								+ D.getContractInfo().getRentEndDate());
						row.createCell(10).setCellValue(linkedcontract.getContractInfo().getLessorRentAmount() + "|"
								+ D.getContractInfo().getLessorRentAmount());
						row.createCell(11).setCellValue(linkedcontract.getMonthlyRent() + "|" + D.getMonthlyRent());
						row.createCell(12).setCellValue(linkedcontract.getDue() + D.getDue());
						row.createCell(13).setCellValue(linkedcontract.getProvision() + D.getProvision());
						row.createCell(14).setCellValue(linkedcontract.getGross() + D.getGross());
						row.createCell(15).setCellValue(linkedcontract.getTds() + D.getTds());
						row.createCell(16).setCellValue(linkedcontract.getNet() + D.getNet());
						row.createCell(17).setCellValue(linkedcontract.getGST() + D.getGST());
						SXSSFCell actualcell = row.createCell(18);
						if (linkedcontract.isRedflag() || D.isRedflag()) {
//							actualcell.setCellStyle(cellstyle);
						}
						try {
							actualcell.setCellValue(Double.parseDouble(linkedcontract.getActualAmount())
									+ Double.parseDouble(D.getActualAmount()));
						} catch (Exception e) {
							actualcell.setCellValue("--");
						}
					} else {
						if (!doneList.contains(D)) {
							doneList.add(D);
							SXSSFRow row = sheet.createRow(rno++);
							row.createCell(0).setCellValue(D.getContractID());
							row.createCell(1).setCellValue(D.getContractInfo().getAgreementActivationStatus());
							row.createCell(2).setCellValue(D.getBranchID());
							row.createCell(3).setCellValue(D.getContractInfo().getLesseeBranchName());
							row.createCell(4).setCellValue(D.getContractInfo().getLessorName());
							row.createCell(5).setCellValue(D.getContractInfo().getLessorBankName());
							row.createCell(6).setCellValue(D.getContractInfo().getLessorIfscNumber());
							row.createCell(7).setCellValue(D.getContractInfo().getLessorAccountNumber());
							row.createCell(8).setCellValue(D.getContractInfo().getRentStartDate() + "");
							row.createCell(9).setCellValue(D.getContractInfo().getRentEndDate() + "");
							row.createCell(10).setCellValue(D.getContractInfo().getLessorRentAmount());
							row.createCell(11).setCellValue(D.getMonthlyRent());
							row.createCell(12).setCellValue(D.getDue());
							row.createCell(13).setCellValue(D.getProvision());
							row.createCell(14).setCellValue(D.getGross());
							row.createCell(15).setCellValue(D.getTds());
							row.createCell(16).setCellValue(D.getNet());
							row.createCell(17).setCellValue(D.getGST());
							SXSSFCell actualcell = row.createCell(18);
							if (D.isRedflag()) {
//								actualcell.setCellStyle(cellstyle);
							}
							actualcell.setCellValue(D.getActualAmount());
						}
					}
				}
			});
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workBook.write(outputStream);
			workBook.dispose();
			byte[] byteArray = outputStream.toByteArray();
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
			InputStreamResource inputStreamResource = new InputStreamResource(byteArrayInputStream);
			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=Data.xlsx")
					.contentType(MediaType.parseMediaType("application/vnd.ms-excel")).body(inputStreamResource);
		}
		return null;
	}

}
