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
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import org.springframework.security.core.Authentication;
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
import com.cagl.dto.DashBoardInfo;
import com.cagl.dto.MakeActualDto;
import com.cagl.dto.PaymentReportDto;
import com.cagl.dto.RecipiantDto;
import com.cagl.dto.RentContractDto;
import com.cagl.dto.RentDueDto;
import com.cagl.dto.Rentduecalculation;
import com.cagl.dto.Responce;
import com.cagl.dto.SDRecoardDto;
import com.cagl.dto.TenureDto;
import com.cagl.dto.provisionDto;
import com.cagl.dto.varianceDto;
import com.cagl.entity.ApiCallRecords;
import com.cagl.entity.ConfirmPaymentReport;
import com.cagl.entity.IfscMaster;
import com.cagl.entity.PaymentReport;
import com.cagl.entity.RawPaymentReport;
import com.cagl.entity.RentContract;
import com.cagl.entity.RentDue;
import com.cagl.entity.StroageRentContract;
import com.cagl.entity.provision;
import com.cagl.repository.ApiCallRepository;
import com.cagl.repository.BranchDetailRepository;
import com.cagl.repository.PaymentReportRepository;
import com.cagl.repository.RawPaymentReportRepository;
import com.cagl.repository.RentActualRepository;
import com.cagl.repository.RentContractRepository;
import com.cagl.repository.RfBrachRepository;
import com.cagl.repository.SDRecoardRepository;
import com.cagl.repository.confirmPaymentRepository;
import com.cagl.repository.ifscMasterRepository;
import com.cagl.repository.provisionRepository;
import com.cagl.repository.rentDueRepository;
import com.cagl.repository.stroagecontactRepo;
import com.cagl.repository.varianceRepository;
import com.cagl.service.RentService;

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
	@Autowired
	stroagecontactRepo stroagecontactRepo;
	@Autowired
	confirmPaymentRepository confirmPaymentRepository;

	@GetMapping("/AlertContract")
	public ResponseEntity<Responce> getAlertContract() throws ParseException {
		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(rentService.getAlertContract())
				.error(Boolean.FALSE).msg("Alerts Contracts").build());
	}

	/**
	 * @Note DocumentType field use for Extended Contract.
	 */
	@GetMapping("/ExtendContract")
	public String getAllExtendedContraact(Authentication user, @RequestParam int contractID,
			@RequestParam String rentEndDate) {
		// API Call Record Save.
		apirecords.save(ApiCallRecords.builder().apiname("ExtendContract")
				.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
				.msg(contractID + "-" + user.getName()).build());

		RentContract rentContract = rentContractRepository.findById(contractID).get();
		String ExistingData = rentContract.getDocumentType();
		rentContract.setDocumentType(ExistingData + "{Contract Extend(" + rentEndDate + ")} ");
		rentContractRepository.save(rentContract);
		return "{Contract Extend(" + rentEndDate + ")";
	}

	@GetMapping("/getSdDetails")
	public ResponseEntity<Responce> getSdDeatils() {
		List<RentContractDto> contractDtos = new ArrayList<>();
		rentContractRepository.findByAgreementActivationStatus("Open").stream().forEach(e -> {
			RentContractDto contrcat = new RentContractDto();
			BeanUtils.copyProperties(e, contrcat);
			contractDtos.add(contrcat);
		});
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data(contractDtos).error(Boolean.FALSE).msg("Alerts Contracts").build());
	}

	/**
	 * @return resolved contract to make actual
	 * @throws Exception
	 */
	@GetMapping("/resolvealertContract")
	public ResponseEntity<Responce> getResolvedAlertContract() throws Exception {
		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(rentService.getResolvedAlertContract())
				.error(Boolean.FALSE).msg("Alerts Contracts").build());
	}

	// API NOT IN USE.
	@GetMapping("/countprovision")
	public int getProvisionCount(@RequestParam int year) {
		return rentContractRepository.getProvisionCount(year);
	}

	/**
	 * DashBoard API
	 * 
	 * @return
	 */
	@GetMapping("/getLastContract")
	public ResponseEntity<Responce> getLastContract() {
		RentContractDto Contract = new RentContractDto();
		BeanUtils.copyProperties(rentContractRepository.findById(rentContractRepository.getMaxID()).get(), Contract);
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data(Contract).error(Boolean.FALSE).msg("Alerts Contracts").build());
	}

	/**
	 * DashBoard API
	 * 
	 * @return
	 */
	@GetMapping("/DashBoardDetails")
	public DashBoardInfo getProvisionCount() {
		return rentService.getDashBoardDetails();
	}

	// API NOT IN USE.
	@GetMapping("/countvariance")
	public int getVarianceCount(@RequestParam int year) {
		return rentContractRepository.getVarianceCount(year);
	}

	/**
	 * @API use to Check Duplicate P_Contract IDs.
	 * @param pContractID
	 */
	@GetMapping("/checkPcontract")
	public Boolean checkEligiable(@RequestParam int pContractID) {
		if (pContractID == 0) {
			return true;
		} else {
			return !rentContractRepository.findByPriviousContractID(pContractID).stream()
					.map(e -> e.getPriviousContractID()).anyMatch(id -> id == pContractID);
		}
	}

	/**
	 * @API used For Change Contract_activation_Status Open to closed.
	 * @param contractId
	 */
	@GetMapping("/closecontract")
	public int ChangeContractStatus(Authentication user, @RequestParam String contractId) {

		if (rentContractRepository.findById(Integer.parseInt(contractId)).get().getRentEndDate()
				.isAfter(LocalDate.now())) {
			apirecords.save(ApiCallRecords.builder().apiname("closecontract")
					.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
					.msg(contractId + "-" + user.getName()).build());

			return jdbcTemplate.update(
					"update rent_contract set agreement_activation_status='Closed' where uniqueid=" + contractId + ";");
		}
		return 0;
	}

	/**
	 * @APi use to approved pending contracts
	 * @param contractID
	 */
	@GetMapping("/changeZone")
	public int ChangeContractZone(Authentication user, @RequestParam int contractID) {
		apirecords.save(ApiCallRecords.builder().apiname("changeZone")
				.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
				.msg(contractID + "-" + user.getName()).build());
		return jdbcTemplate
				.update("update rent_contract set contract_zone='APPROVED' where uniqueid=" + contractID + ";");
	}

	@GetMapping("/getvariance")
	public ResponseEntity<Responce> getvariance(@RequestParam String contractID) {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder()
						.data(rentService.getvariance(contractID).stream()
								.sorted(Comparator.comparing(varianceDto::getFlag)).collect(Collectors.toList()))
						.error(Boolean.FALSE).msg("ALL variance [" + contractID + "]").build());
	}

	@GetMapping("/ifscinfo")
	public ResponseEntity<Responce> getIfscInfo(@RequestParam String ifscNumber) {
		Optional<IfscMaster> data = ifscMasterRepository.findById(ifscNumber);
		if (data.isPresent())
			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().data(data.get()).error(Boolean.FALSE).msg("ifsc info").build());
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data(null).error(Boolean.TRUE).msg("IFSC Info Not Present in Master").build());
	}

	// Drop Down API
	@GetMapping("/getstate")
	public ResponseEntity<Responce> getState() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce
						.builder().data(rentContractRepository.findAll().stream().map(e -> e.getLesseeState()).sorted()
								.distinct().collect(Collectors.toList()))
						.error(Boolean.FALSE).msg("Get All State").build());
	}

	// Drop Down API
	@GetMapping("/getdistrict")
	public ResponseEntity<Responce> getDistrictBaseonState(@RequestParam String state) {
		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder()
				.data(rentContractRepository.getdistrict(state).stream().distinct().collect(Collectors.toList()))
				.error(Boolean.FALSE).msg("Get District").build());
	}

	// Drop Down API
	@GetMapping("/filterBranchIDs")
	public ResponseEntity<Responce> getBranchIdsforFilter() {
		return ResponseEntity.status(HttpStatus.OK).body(Responce.builder()
				.data(rentContractRepository.getbranchIds().stream().sorted().distinct().collect(Collectors.toList()))
				.error(Boolean.FALSE).msg("Get All Branch IDS base on contract master..!").build());
	}

	@GetMapping("/getBranchName")
	public ResponseEntity<Responce> getBranchNames() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder()
						.data(rentContractRepository.getbranchNames().stream().sorted().collect(Collectors.toList()))
						.error(Boolean.FALSE).msg("All Brannches").build());
	}

	// Drop Down API
	@GetMapping("/getAreaName")
	public ResponseEntity<Responce> getAreaName() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder()
						.data(rentContractRepository.getAreaName().stream().sorted().collect(Collectors.toList()))
						.error(Boolean.FALSE).msg("All Areas").build());
	}

	@GetMapping("/getZoneName")
	public ResponseEntity<Responce> getZoneName() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder()
						.data(rentContractRepository.getZoneName().stream().sorted().collect(Collectors.toList()))
						.error(Boolean.FALSE).msg("All Zones").build());
	}

	@GetMapping("/getDivisionName")
	public ResponseEntity<Responce> getDivisionName() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder()
						.data(rentContractRepository.getDivisionName().stream().sorted().collect(Collectors.toList()))
						.error(Boolean.FALSE).msg("All Division").build());
	}

	/**
	 * @param sdrecord
	 */
	@PostMapping("/setsd")
	public ResponseEntity<Responce> MakeSd(Authentication authentication, @RequestBody SDRecoardDto sdrecord)
			throws ParseException {
//		Optional<provision> optionalProvision = provisionRepository
//				.findById(sdrecord.getContractID() + "-" + sdrecord.getMonth() + "-" + sdrecord.getYear());
//		if (optionalProvision.isPresent())
//			return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(optionalProvision.get())
//					.error(Boolean.TRUE).msg(" Cant't make SD provision Already Exist").build());
		RentContract rentContract = rentContractRepository.findById(sdrecord.getContractID()).get();
		// If month Year not match Send conflict Error in ResponSe
		if (LocalDate.now().isAfter(rentContract.getRentEndDate())) {
			ResponseEntity<Responce> responce = addprovison(authentication, "REVERSED", // SD reversed always PAID.
					provisionDto.builder().branchID(rentContract.getBranchID()).paymentFlag("PAID")
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

	@PostMapping("/makeactual")
	public ResponseEntity<Responce> makeActual(@RequestParam String Status,
			@RequestBody List<MakeActualDto> ActualDto) {
		if (Status.equalsIgnoreCase("CONFIRM")) {

//			List<PaymentReport> Paymentdata = paymentReportRepository.findByMonthAndYear(ActualDto.get(0).getMonth(),
//					ActualDto.get(0).getYear() + "");
//			Paymentdata.stream().forEach(e->{
//				ConfirmPaymentReport.builder().ActualAmount(e.getActualAmount()).branchID(e.getBranchID()).contractID(e.getContractID()+"").due(e.getDue()).Gross(e.getGross()).GST(e.getGST()).ID(null).month(e.getMonth()).year(e.getYear()).
//				confirmPaymentRepository.save(null);			
//			});
			return null;
		}
		Map<String, String> responce = rentService.makeactual(Status, ActualDto);
		// ---API CALL RECORD SAVE---
		if (!Status.equalsIgnoreCase("CONFIRM")) {
			try {
				apirecords.save(ApiCallRecords.builder().apiname("makeactual")
						.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
						.msg(ActualDto.toString() + "Responce=>" + responce.toString()).build());
			} catch (Exception e) {
				// --> IF MSG Field Large.
				apirecords.save(ApiCallRecords.builder().apiname("makeactual")
						.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
						.msg("NO_Recoards_Found..!").build());
			}
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data(responce).error(Boolean.FALSE).msg("Actual Done").build());
	}

	/**
	 * @API -> TO make Provision or Reverse Provision
	 */
	@PostMapping("/setprovision")
	public ResponseEntity<Responce> addprovison(Authentication authentication, @RequestParam String provisionType,
			@RequestBody provisionDto provisionDto) throws ParseException {
		/**
		 * As per new Requirement(IN PROD) Same month multiple provision required(for
		 * unique (currentDate We are using at end of primary_Key)
		 * 
		 * @BUT WE can't make 2 provision reversed with different
		 *      payment_flag(PAID_or_NOT PAID)
		 */
		if (provisionType.equalsIgnoreCase("REVERSED")) {
			String ExistPaymentFlag = provisionRepository.getReversedProvisionPaymentFlag(
					provisionDto.getContractID() + "-" + provisionDto.getMonth() + "-" + provisionDto.getYear());
			if (ExistPaymentFlag != null & !provisionDto.getPaymentFlag().equalsIgnoreCase(ExistPaymentFlag)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Responce.builder().data("PAID & NOT PAID CONFLICT").error(Boolean.TRUE)
								.msg("SAME_MONTH_YOU CANT MAKE BOTH").build());
			}
		}
		provisionDto.setMakerID(authentication.getName());
		provisionDto.setMakerTimeZone(LocalDate.now().toString());
		rentService.addprovision(provisionType, provisionDto);
		// ---API CALL RECORD SAVE---
		apirecords.save(ApiCallRecords.builder().apiname("setprovision")
				.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
				.msg(provisionDto.getContractID() + "/" + provisionDto.getProvisionAmount()).build());

		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data("provision Done").error(Boolean.FALSE).msg("provision Added").build());
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
	@DeleteMapping("/deleteProvision")

	public String deleteProvision(@RequestParam String contractID, @RequestParam int year, @RequestParam String month) {
		try {
			if (!(LocalDate.now().getMonth() + "").equalsIgnoreCase(month) && LocalDate.now().getYear() != year)
				throw new RuntimeException("ALLOWED ONLY FOR CURRENT MONTH&YEAR");
			provision provision = provisionRepository.findByContractIDAndYearAndMonth(contractID, year, month);
			provisionRepository.delete(provision);
			// -----------------------------------------
			// ---API CALL RECORD SAVE---
			try {
				apirecords.save(ApiCallRecords.builder().apiname("deleteProvision")
						.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
						.msg(contractID + " " + month + year + " -" + provision.getProvisionAmount()).build());
			} catch (Exception e) {
			}

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
			makeActual("NOTCONFIRM", dto);
			// ----------Generate Payment Report-----
			generatePaymentReport(contractID, month, year + "", "make");
			return "PROVISION DELETION DONE " + contractID + "-" + month + "/" + year;
		} catch (Exception e) {
			return "PROVISION DELETION FAILED" + contractID + "-" + month + "/" + year + "[ " + e.getMessage() + " ]";
		}
	}

	/**
	 * API NOT USED.
	 */
//	@PostMapping("/BulkProvisionDelete")
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

	// NOT IN USE
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

	@GetMapping("/getduereportUid") // Base on UniqueID
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

	@GetMapping("/getduereportBid") // Base on BranchID
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

	/**
	 * @Use While adding new Contract(DropDown)
	 * @param BranchID
	 * @return
	 */
	@GetMapping("/getbranchids")
	public List<String> getBranchIDs(@RequestParam String type) {
		if (type.toUpperCase().startsWith("RF")) {
			return rentContractRepository.getbranchIDs("RF").stream().distinct().collect(Collectors.toList());
		} else if (type.toUpperCase().startsWith("GL")) {
			return rentContractRepository.getbranchIDs("GL").stream().distinct().collect(Collectors.toList());
		} else {
			return new ArrayList<>();
		}
	}

	/**
	 * @Use While adding new Contract
	 * @param BranchID
	 * @return
	 */
	@GetMapping("/getbranchdetails")
	public ResponseEntity<Responce> getBranchDetails(@RequestParam String BranchID) {

		List<RentContract> contractData = rentContractRepository.findByBranchID(BranchID);

		if (contractData != null) {

			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().error(Boolean.FALSE)
							.data(contractData.stream().sorted(Comparator.comparing(RentContract::getUniqueID))
									.reduce((first, second) -> second).map(e -> {
										return BranchDto.builder().branchID(e.getBranchID())
												.branchName(e.getLesseeBranchName()).areaName(e.getLesseeAreaName())
												.region(e.getLesseeDivision()).state(e.getLesseeState())
												.zone(e.getLesseeZone()).build();
									}).get())
							.msg("Data present..!").build());

		} else {
			return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(BranchDto.builder().build())
					.msg("Branch Data Not Exist...!").error(Boolean.TRUE).build());
		}

//		Optional<RfBranchMaster> rfdata = rfBrachRepository.findById(BranchID);
//		if (rfdata.isPresent()) {
//			RfBranchMaster rfBranchMaster = rfdata.get();
//			BranchDto build = BranchDto.builder().areaName(rfBranchMaster.getAreaName())
//					.branchID(rfBranchMaster.getRfBranchID()).branchName(rfBranchMaster.getBranchName())
//					.region(rfBranchMaster.getRegion()).build();
////			RfBranchmasterDto build = RfBranchmasterDto.builder().amContactNumber(rfBranchMaster.getAmContactNumber())
////					.areaName(rfBranchMaster.getAreaName()).branchName(rfBranchMaster.getBranchName())
////					.region(rfBranchMaster.getRegion()).rfBranchID(rfBranchMaster.getRfBranchID()).build();
//			return ResponseEntity.status(HttpStatus.OK)
//					.body(Responce.builder().error(Boolean.FALSE).data(build).msg("Data present..!").build());
//		} else {
//			Optional<BranchDetail> glData = branchDetailRepository.findById(BranchID);
//			if (glData.isPresent()) {
//				BranchDetail branchDetail = glData.get();
//				BranchDto build = BranchDto.builder().areaName(branchDetail.getAreaName())
//						.branchID(branchDetail.getBranchID()).branchName(branchDetail.getBranchName())
//						.region(branchDetail.getRegion()).state(branchDetail.getState()).zone(branchDetail.getZone())
//						.build();
//				return ResponseEntity.status(HttpStatus.OK)
//						.body(Responce.builder().data(build).msg("Branch Data Exist...!").error(Boolean.FALSE).build());
//			}
//			return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().data(BranchDto.builder().build())
//					.msg("Branch Data Not Exist...!").error(Boolean.TRUE).build());
//		}
	}

	/**
	 * @DropDown API
	 * @param branchName & branch Type 
	 * @return BranchIds Base on Branch Name.
	 */
	@GetMapping("/getBranchsByBranchName")//2
	public List<String> getbranchID(@RequestParam String branchName,@RequestParam String branchtype) {
		return rentContractRepository.getbranchIDsByBranchName(branchName,branchtype);
	}
	
	@GetMapping("/getBranchType")//1
	public List<String> getbranchType(@RequestParam String branchName){
		return rentContractRepository.getBranchTypeBaseOnBranchName(branchName);
		
	}

	@GetMapping("/renewalDetails")
	public ResponseEntity<Responce> getinfo(@RequestParam String branchID,@RequestParam String branchtype) {

		List<RentContract> contractData = rentContractRepository.findByBranchIDAndLesseeBranchType(branchID,branchtype);
		if (contractData != null)
			return ResponseEntity.status(HttpStatus.OK)
					.body(Responce.builder().error(Boolean.FALSE)
							.data(contractData.stream().sorted(Comparator.comparing(RentContract::getUniqueID))
									.reduce((first, second) -> second).map(e -> {
										RentContractDto responceData = new RentContractDto();
										BeanUtils.copyProperties(e, responceData);
										responceData.setRentStartDate(e.getRentEndDate().plusDays(1));
										responceData.setRentEndDate(e.getRentEndDate().plusMonths(11));
										responceData.setSchedulePrimesis(e.getSchedulePrimesis());
										responceData.setPriviousContractID(0);
										responceData.setContractStatus("Renewal");
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
	public ResponseEntity<Responce> insertRentContract(Authentication authentication,
			@RequestBody RentContractDto rentContractDto) {
		List<Object> responceData = new ArrayList<>();
		rentContractDto.getRecipiants().stream().forEach(data -> {
			RentContract rentContract = new RentContract();
			BeanUtils.copyProperties(rentContractDto, rentContract);
			List<Integer> ids = rentContractRepository.getids().stream().sorted().collect(Collectors.toList());
			if (ids.isEmpty())
				rentContract.setUniqueID(1); // Input value type is int
			else
				rentContract.setUniqueID((ids.get(ids.size() - 1) + 1)); // Input value type is Integer.

			rentContract.setMaker(authentication.getName());
			rentContract.setMTimeZone(LocalDate.now().toString());
			rentContract.setContractZone("PENDING");
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

			if (save != null) {
				responceData.add(save);
				try {
					apirecords.save(
							ApiCallRecords.builder().apiname("insertcontract").timeZone(LocalDate.now().toString())
									.msg(save.getUniqueID() + "||" + save.getBranchID()).build());
				} catch (Exception e) {
					System.out.println("Errors Occer In apiRecord Data");
				}
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
	public ResponseEntity<Responce> editContracts(Authentication authentication, @RequestParam int uniqueID,
			@RequestBody RentContractDto contractDto) {
		RentContract rentContract = rentContractRepository.findById(uniqueID).get();
		/**
		 * Save API Record
		 */
		apirecords.save(ApiCallRecords.builder().apiname("editcontracts")
				.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
				.msg(contractDto.getUniqueID() + "/").build());

		boolean flagCheck = false; // its false don't calculate Due..!
		if (!rentContract.getRentStartDate().toString().equalsIgnoreCase(contractDto.getRentStartDate().toString())
				|| !rentContract.getRentEndDate().toString().equalsIgnoreCase(contractDto.getRentEndDate().toString())
				|| contractDto.getLessorRentAmount() != rentContract.getLessorRentAmount()
				|| !rentContract.getSchedulePrimesis().equalsIgnoreCase(contractDto.getSchedulePrimesis())) {
			/*
			 * @NOTE:->In above condition [SchedulePrimesis] field is use As a Escalated
			 * Month for Rent_Due Calculation
			 */
			if (uniqueID > 10390 || rentContract.getDocumentType() != null) {// till 10390 we have old records so we
																				// can't change rent due for old
																				// contract.
				List<RentDue> unusedDueData = dueRepository.getUnusedDueData(uniqueID + "");
				unusedDueData.stream().forEach(due -> {
					dueRepository.delete(due);
				});
				flagCheck = true;// if (True) Changes done in RentDue orElse no need to change any thing
			}
		}

		RentContract editcontract = new RentContract();
		BeanUtils.copyProperties(contractDto, editcontract);

		editcontract.setUniqueID(uniqueID);
		editcontract.setContractZone("PENDING");
		editcontract.setEditer(authentication.getName());
		editcontract.setMaker(rentContract.getMaker());
		editcontract.setMTimeZone(rentContract.getMTimeZone());
		editcontract.setChecker(rentContract.getChecker());
		editcontract.setCTimeZone(rentContract.getCTimeZone());
		editcontract.setDocumentType(rentContract.getDocumentType());
		if (rentContract.getContractZone().equalsIgnoreCase("PENDING")) {// Its use to Separate pending Screen
			// Contract(setETimeZone)-> null value used in logic.
			editcontract.setETimeZone(rentContract.getETimeZone());
		} else {// if approved change ETimeZone
			editcontract.setETimeZone(LocalDate.now().toString());
		}
		RentContract save = rentContractRepository.save(editcontract);
		if (save != null & flagCheck) {
			// ---API CALL RECORD SAVE---
			apirecords.save(ApiCallRecords.builder().apiname("modifyRentDue while editing contract")
					.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
					.msg(save.getUniqueID() + "/" + save.getMonthlyRent()).build());
			try {
				createRentdue(Rentduecalculation.builder().branchID(save.getBranchID())
						.escalatedMonth(Double.parseDouble(save.getSchedulePrimesis())).contractID(save.getUniqueID())
						.escalation(save.getEscalation()).lesseeBranchType(save.getLesseeBranchType())
						.monthlyRent(save.getLessorRentAmount()).renewalTenure(save.getAgreementTenure())
						.rentEndDate(save.getRentEndDate()).rentStartDate(save.getRentStartDate()).build());
			} catch (Exception e) {
				// TODO: handle exception
				apirecords.save(ApiCallRecords.builder().apiname("Error Rent Due calculatation")
						.timeZone(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()))
						.msg(save.getUniqueID() + "/" + save.getMonthlyRent()).build());

			}
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().error(Boolean.FALSE).msg("Edit Sucessfully..!").data(contractDto).build());
	}

	/**
	 * @ Newly added Pending Contract API (Checker Screen)
	 */
	@GetMapping("/getnewpendingcontract")
	public ResponseEntity<Responce> getNewPendingContract() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data(rentContractRepository.getNewPendingContract().stream().map(e -> {
					RentContractDto Dto = new RentContractDto();
					BeanUtils.copyProperties(e, Dto);
					return Dto;
				}).collect(Collectors.toList())).error(Boolean.FALSE)

						.msg("Pending Contract").build());
	}

	/**
	 * @ Updated Pending Contract API (Checker Screen)
	 */
	@GetMapping("/getupdatpendingcontract")
	public ResponseEntity<Responce> getUpdatedPendingContract() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().data(rentContractRepository.getUpdatedPendingContract().stream().map(e -> {
					RentContractDto Dto = new RentContractDto();
					BeanUtils.copyProperties(e, Dto);
					return Dto;
				}).collect(Collectors.toList())).error(Boolean.FALSE)

						.msg("Pending Contract").build());
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
			allContractDetalis.stream().filter(data -> !data.getContractZone().equalsIgnoreCase("PENDING"))
					.forEach(contractInfo -> {
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
			allContractDetalis.stream().filter(data -> !data.getContractZone().equalsIgnoreCase("PENDING"))
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
	@GetMapping("/getAllContractsByDistrict")
	public ResponseEntity<Responce> getAllContracts(@RequestParam String district) {
		List<RentContract> allContractDetalis = rentContractRepository.findByPremesisDistrictAndContractZone(district,
				"Approved");
		if (!allContractDetalis.isEmpty()) {
			return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().error(Boolean.FALSE)
					.msg("All Contracts Details fetch..!").data(allContractDetalis.stream().map(contractInfo -> {

						RentContractDto contractDto = new RentContractDto();
						BeanUtils.copyProperties(contractInfo, contractDto);
						return contractDto;
					}).sorted(Comparator.comparing(RentContractDto::getUniqueID)).collect(Collectors.toList()))
					.build());
		}
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().error(Boolean.TRUE).msg("Contracts Data Not present..!").data(null).build());
	}

	@GetMapping("/getAllContractByState")
	public ResponseEntity<Responce> getallContractBaseonState(@RequestParam String State) {
		List<RentContract> contract = rentContractRepository.findByLesseeState(State);
		if (contract != null) {
			return ResponseEntity.status(HttpStatus.OK).body(Responce.builder().error(Boolean.FALSE)
					.msg("All Contracts Details fetch..!")
					.data(contract.stream().filter(e->
				         e.getContractZone().equalsIgnoreCase("Approved")
					).map(e -> {
						RentContractDto dto = new RentContractDto();
						BeanUtils.copyProperties(e, dto);
						return dto;
					}).collect(Collectors.toList())).build());
		} else {
			return ResponseEntity.status(HttpStatus.OK).body(
					Responce.builder().error(Boolean.TRUE).msg("Contracts Data Not present..!").data(null).build());
		}
	}

	@GetMapping("/getOpenContract")
	public ResponseEntity<Responce> getOpenContract() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().error(Boolean.FALSE).msg("All Contracts Details fetch..!")
						.data(rentContractRepository.findByAgreementActivationStatus("Open").stream().map(e -> {
							RentContractDto dto = new RentContractDto();
							BeanUtils.copyProperties(e, dto);
							return dto;
						}).collect(Collectors.toList())).build());

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
	@GetMapping("/makeDue")
	public Boolean generateRentDue() {
		// Get all pending Due Calculation ContractIDs
		List<RentContract> allcontract = rentContractRepository.getduemakerIDs();
//		List<RentContract> allcontract = new ArrayList<>();
//		allcontract.add(rentContractRepository.findById(10104).get());
		allcontract.stream().map(e -> {
			return Rentduecalculation.builder().branchID(e.getBranchID())
					.escalatedMonth((int) (Double.parseDouble(e.getSchedulePrimesis()))).contractID(e.getUniqueID())
					.escalation(e.getEscalation()).lesseeBranchType(e.getLesseeBranchType())
					.monthlyRent(e.getMonthlyRent()).renewalTenure(e.getAgreementTenure())
					.rentEndDate(e.getRentEndDate()).rentStartDate(e.getRentStartDate()).build();
		}).collect(Collectors.toList()).stream().forEach(data -> {
			List<RentDue> unusedDueData = dueRepository.getUnusedDueData(data.getContractID() + "");
			unusedDueData.stream().forEach(due -> {
				dueRepository.delete(due);
			});
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
				System.out.println(cID + "-&-?>");
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

//			List<Integer> collect2 = collect.stream().map(data-> Integer.parseInt(data.getContractID())).sorted(Comparator.reverseOrder())
//					.collect(Collectors.toList());

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

			SXSSFCell branchType = header.createCell(4);
			branchType.setCellStyle(cellStyle);
			branchType.setCellValue("Branch_Type");

			SXSSFCell zonecell = header.createCell(5);
			zonecell.setCellStyle(cellStyle);
			zonecell.setCellValue("Zone_Name");

			SXSSFCell regioncell = header.createCell(6);
			regioncell.setCellStyle(cellStyle);
			regioncell.setCellValue("Region_Name");

			SXSSFCell statecell = header.createCell(7);
			statecell.setCellStyle(cellStyle);
			statecell.setCellValue("State");

			SXSSFCell cell3 = header.createCell(8);
			cell3.setCellStyle(cellStyle);
			cell3.setCellValue("lessor_Name");

			SXSSFCell cell4 = header.createCell(9);
			cell4.setCellStyle(cellStyle);
			cell4.setCellValue("Bank_Name");

			SXSSFCell cell5 = header.createCell(10);
			cell5.setCellStyle(cellStyle);
			cell5.setCellValue("IFSC");

			SXSSFCell cell6 = header.createCell(11);
			cell6.setCellStyle(cellStyle);
			cell6.setCellValue("Account_Number");

			SXSSFCell cell7 = header.createCell(12);
			cell7.setCellStyle(cellStyle);
			cell7.setCellValue("Rent_Start_Date");

			SXSSFCell cell8 = header.createCell(13);
			cell8.setCellStyle(cellStyle);
			cell8.setCellValue("Rent_End_Date");

			SXSSFCell cell9 = header.createCell(14);
			cell9.setCellStyle(cellStyle);
			cell9.setCellValue("Initial_MonthRent");

			SXSSFCell cell10 = header.createCell(15);
			cell10.setCellStyle(cellStyle);
			cell10.setCellValue("Current_MonthRent");

			SXSSFCell cell11 = header.createCell(16);
			cell11.setCellStyle(cellStyle);
			cell11.setCellValue("Due");

			SXSSFCell cell12 = header.createCell(17);
			cell12.setCellStyle(cellStyle);
			cell12.setCellValue("Provision");

			SXSSFCell cell13 = header.createCell(18);
			cell13.setCellStyle(cellStyle);
			cell13.setCellValue("Gross");

			SXSSFCell cell14 = header.createCell(19);
			cell14.setCellStyle(cellStyle);
			cell14.setCellValue("TDS");

			SXSSFCell cell15 = header.createCell(20);
			cell15.setCellStyle(cellStyle);
			cell15.setCellValue("NET");

			SXSSFCell cell16 = header.createCell(21);
			cell16.setCellStyle(cellStyle);
			cell16.setCellValue("GST");

			SXSSFCell cell17 = header.createCell(22);
			cell17.setCellStyle(cellStyle);
			cell17.setCellValue("Actual Amount");

			SXSSFCell cell18 = header.createCell(23);
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

					if (data.stream()
							.anyMatch(obj -> obj.getContractID() == D.getContractInfo().getPriviousContractID())) {

						tempL.add(D);
						PaymentReport linkedcontract = data.stream()
								.filter(id -> id.getContractID() == D.getContractInfo().getPriviousContractID())
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

							row.createCell(4).setCellValue(item.getContractInfo().getLesseeBranchType());
							row.createCell(5).setCellValue(item.getContractInfo().getLesseeZone());
							row.createCell(6).setCellValue(item.getContractInfo().getLesseeDivision());
							row.createCell(7).setCellValue(item.getContractInfo().getLesseeState());

							row.createCell(8).setCellValue(item.getContractInfo().getLessorName());
							row.createCell(9).setCellValue(item.getContractInfo().getLessorBankName());
							row.createCell(10).setCellValue(item.getContractInfo().getLessorIfscNumber());
							row.createCell(11).setCellValue(item.getContractInfo().getLessorAccountNumber());
							row.createCell(12).setCellValue(item.getContractInfo().getRentStartDate() + "");
							row.createCell(13).setCellValue(item.getContractInfo().getRentEndDate() + "");
							row.createCell(14).setCellValue(item.getContractInfo().getLessorRentAmount());
							row.createCell(15).setCellValue(item.getMonthlyRent());
							row.createCell(16).setCellValue(item.getDue());
							row.createCell(17).setCellValue(item.getProvision());
							row.createCell(18).setCellValue(item.getGross());
							row.createCell(19).setCellValue(item.getTds());
							row.createCell(20).setCellValue(item.getNet());
							row.createCell(21).setCellValue(item.getGST());
							SXSSFCell actualcell = row.createCell(22);
							if (!item.isRedflag()) {
								actualcell.setCellStyle(cellstyle);
							}
							actualcell.setCellValue(item.getActualAmount());
						});
						SXSSFRow row = sheet.createRow(rno++);
						row.setRowStyle(style1);
						row.createCell(0).setCellValue(linkedcontract.getContractID() + "|" + D.getContractID());
						row.createCell(1).setCellValue(D.getContractInfo().getAgreementActivationStatus());
						row.createCell(2).setCellValue(D.getBranchID());
						row.createCell(3).setCellValue(D.getContractInfo().getLesseeBranchName());

						row.createCell(4).setCellValue(D.getContractInfo().getLesseeBranchType());
						row.createCell(5).setCellValue(D.getContractInfo().getLesseeZone());
						row.createCell(6).setCellValue(D.getContractInfo().getLesseeDivision());
						row.createCell(7).setCellValue(D.getContractInfo().getLesseeState());

						row.createCell(8).setCellValue(D.getContractInfo().getLessorName());
						row.createCell(9).setCellValue(D.getContractInfo().getLessorBankName());
						row.createCell(10).setCellValue(D.getContractInfo().getLessorIfscNumber());
						row.createCell(11).setCellValue(D.getContractInfo().getLessorAccountNumber());
						row.createCell(12).setCellValue(linkedcontract.getContractInfo().getAgreementStartDate() + "|"
								+ D.getContractInfo().getRentStartDate());
						row.createCell(13).setCellValue(linkedcontract.getContractInfo().getAgreementEndDate() + "|"
								+ D.getContractInfo().getRentEndDate());
						row.createCell(14).setCellValue(linkedcontract.getContractInfo().getLessorRentAmount() + "|"
								+ D.getContractInfo().getLessorRentAmount());
						row.createCell(15).setCellValue(linkedcontract.getMonthlyRent() + "|" + D.getMonthlyRent());
						row.createCell(16).setCellValue(linkedcontract.getDue() + D.getDue());
						row.createCell(17).setCellValue(linkedcontract.getProvision() + D.getProvision());
						row.createCell(18).setCellValue(linkedcontract.getGross() + D.getGross());
						row.createCell(19).setCellValue(linkedcontract.getTds() + D.getTds());
						row.createCell(20).setCellValue(linkedcontract.getNet() + D.getNet());
						row.createCell(21).setCellValue(linkedcontract.getGST() + D.getGST());
						SXSSFCell actualcell = row.createCell(22);
						if (!linkedcontract.isRedflag() || !D.isRedflag()) {
							actualcell.setCellStyle(cellstyle);
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

							row.createCell(4).setCellValue(D.getContractInfo().getLesseeBranchType());
							row.createCell(5).setCellValue(D.getContractInfo().getLesseeZone());
							row.createCell(6).setCellValue(D.getContractInfo().getLesseeDivision());
							row.createCell(7).setCellValue(D.getContractInfo().getLesseeState());

							row.createCell(8).setCellValue(D.getContractInfo().getLessorName());
							row.createCell(9).setCellValue(D.getContractInfo().getLessorBankName());
							row.createCell(10).setCellValue(D.getContractInfo().getLessorIfscNumber());
							row.createCell(11).setCellValue(D.getContractInfo().getLessorAccountNumber());
							row.createCell(12).setCellValue(D.getContractInfo().getRentStartDate() + "");
							row.createCell(13).setCellValue(D.getContractInfo().getRentEndDate() + "");
							row.createCell(14).setCellValue(D.getContractInfo().getLessorRentAmount());
							row.createCell(15).setCellValue(D.getMonthlyRent());
							row.createCell(16).setCellValue(D.getDue());
							row.createCell(17).setCellValue(D.getProvision());
							row.createCell(18).setCellValue(D.getGross());
							row.createCell(19).setCellValue(D.getTds());
							row.createCell(20).setCellValue(D.getNet());
							row.createCell(21).setCellValue(D.getGST());
							SXSSFCell actualcell = row.createCell(22);
							if (!D.isRedflag()) {
								actualcell.setCellStyle(cellstyle);
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

//=============================================================================================	
	/**
	 * @APi use only for Add Contract from back-end.
	 * @StorageRentContract Entity (Table) used.
	 * 
	 * @return
	 */
	@GetMapping("/transferContract")
	public LinkedHashMap insertData() {

		LinkedHashMap<String, String> map = new LinkedHashMap<>();

		List<StroageRentContract> collect = stroagecontactRepo.findAll().stream()
				.sorted(Comparator.comparing(StroageRentContract::getUniqueID)).collect(Collectors.toList());
		collect.stream().forEach(e -> {
			map.put(e.getUniqueID() + "", "");
			Optional<RentContract> findById = rentContractRepository.findById(e.getUniqueID());
			if (!findById.isPresent()) {
				RentContract newContract = new RentContract();
				BeanUtils.copyProperties(e, newContract);
//				RentContract Data = rentContractRepository.findByBranchID(e.getBranchID()).get(0);
				List<RentContract> findByBranchID = rentContractRepository.findByBranchID(e.getBranchID());
				if (findByBranchID.size() > 0) {
					RentContract Data = findByBranchID.get(0);
					newContract.setPremesisDoorNumber(Data.getPremesisDoorNumber());
					newContract.setPremesisFloorNumber(Data.getPremesisFloorNumber());
					newContract.setPremesisLandMark(Data.getPremesisLandMark());
					newContract.setPremesisStreet(Data.getPremesisStreet());
					newContract.setPremesisWardNo(Data.getPremesisWardNo());
					newContract.setPremesisCity(Data.getPremesisCity());
					newContract.setPremesisPinCode(Data.getPremesisPinCode());
					newContract.setPremesisTaluka(Data.getPremesisTaluka());
					newContract.setPremesisDistrict(Data.getPremesisDistrict());
					newContract.setJoinaddress_Premesis(Data.getJoinaddress_Premesis());
					newContract.setPlotNumber(Data.getPlotNumber());
					newContract.setBuiltupArea(Data.getBuiltupArea());
					newContract.setLattitude(Data.getLattitude());
					newContract.setLongitude(Data.getLongitude());
					newContract.setGpsCoordinates(Data.getGpsCoordinates());
					newContract.setLessorPanNumber(Data.getLessorPanNumber());
					newContract.setLessorGstNumber(Data.getLessorGstNumber());
					newContract.setJoinaddress_Vendor(Data.getJoinaddress_Vendor());
					newContract.setGstNo(Data.getGstNo());
					newContract.setPanNo(Data.getPanNo());
				}

				RentContract save = rentContractRepository.save(newContract);
				if (save != null) {
					try {
						createRentdue(Rentduecalculation.builder()
								.escalatedMonth(Double.parseDouble(save.getSchedulePrimesis()))
								.branchID(save.getBranchID()).contractID(save.getUniqueID())
								.escalation(save.getEscalation()).lesseeBranchType(save.getLesseeBranchType())
								.monthlyRent(save.getLessorRentAmount()).renewalTenure(save.getAgreementTenure())
								.rentEndDate(save.getRentEndDate()).rentStartDate(save.getRentStartDate()).build());
						map.put(save.getUniqueID() + "", "save");
					} catch (Exception x) {
						map.put(save.getUniqueID() + "", "Due Error");
					}
				} else {
					map.put(save.getUniqueID() + "", "Not save");
				}
			} else {
				map.put(e.getUniqueID() + "", "Already Exist");
			}
		});
		return map;

	}

}
