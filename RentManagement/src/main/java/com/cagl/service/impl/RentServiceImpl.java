package com.cagl.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.cagl.dto.MakeActualDto;
import com.cagl.dto.PaymentReportDto;
import com.cagl.dto.RentContractDto;
import com.cagl.dto.Responce;
import com.cagl.dto.provisionDto;
import com.cagl.entity.ApiCallRecords;
import com.cagl.entity.PaymentReport;
import com.cagl.entity.RentContract;
import com.cagl.entity.SDRecords;
import com.cagl.entity.Tds;
import com.cagl.entity.Variance;
import com.cagl.entity.provision;
import com.cagl.entity.rentActual;
import com.cagl.repository.BranchDetailRepository;
import com.cagl.repository.PaymentReportRepository;
import com.cagl.repository.RentActualRepository;
import com.cagl.repository.RentContractRepository;
import com.cagl.repository.RfBrachRepository;
import com.cagl.repository.SDRecoardRepository;
import com.cagl.repository.TdsRepository;
import com.cagl.repository.ifscMasterRepository;
import com.cagl.repository.provisionRepository;
import com.cagl.repository.rentDueRepository;
import com.cagl.repository.varianceRepository;
import com.cagl.service.RentService;

import lombok.experimental.var;

@Service
public class RentServiceImpl implements RentService {
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
	varianceRepository varianceRepository;

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate1;

	@Override
	public Responce getPaymentReport(String contractID, String month, String year) {
		if (contractID.equalsIgnoreCase("all")) {
			List<PaymentReport> data = paymentReportRepository.findByMonthAndYear(month, year);
			List<PaymentReportDto> prDto = new ArrayList<>();
			if (data != null) {
				data.stream().forEach(e -> {
					RentContractDto rentContractDto = new RentContractDto();
					BeanUtils.copyProperties(e.getContractInfo(), rentContractDto);
					prDto.add(PaymentReportDto.builder().actualAmount(e.getActualAmount()).due(e.getDue())
							.Gross(e.getGross()).gstamt(e.getGST()).Info(rentContractDto).monthRent(e.getMonthlyRent())
							.monthYear(e.getMonth() + "/" + e.getYear()).net(e.getNet()).provision(e.getProvision())
							.reporttds(e.getTds()).build());
				});
			}
			return Responce.builder().data(prDto).error(Boolean.FALSE).msg("Payment Report Data..!").build();
		} else {
			PaymentReportDto generatereport = generatePaymentreport(contractID, month, year);
			// Here we are saving(Generated Payment Report) Data for audit purpose.
			paymentReportRepository.save(PaymentReport.builder().branchID(generatereport.getInfo().getBranchID())
					.contractInfo(rentContractRepository.findById(Integer.parseInt(contractID)).get())
					.contractID(contractID).due(generatereport.getDue()).Gross(generatereport.getGross())
					.ID(contractID + "-" + generatereport.getMonthYear()).month(month)
					.monthlyRent(generatereport.getMonthRent()).net(generatereport.getNet())
					.provision(generatereport.getProvision()).ActualAmount(generatereport.getActualAmount())
					.tds(generatereport.getReporttds()).GST(generatereport.getGstamt()).year(year).build());
			return Responce.builder().data(generatereport).error(Boolean.FALSE).msg("Payment Report Data..!").build();
		}
	}

	@Autowired
	TdsRepository tdsRepository;

	@Override
	public Map<String, String> makeactual(List<MakeActualDto> actualDto) {
		Map<String, String> responce = new HashMap<>();// use for response
		if (!actualDto.isEmpty() & actualDto != null) {
			// Get on by one Actual Data
			actualDto.stream().forEach(Data -> {
				String ID = Data.getContractID() + "-" + Data.getYear();
				Optional<rentActual> actualData = actualRepository.findById(ID);
				// --------------FOR ACTUAL---------
				if (!actualData.isPresent()) {
					actualRepository.save(rentActual.builder().january(0).february(0).march(0).april(0).may(0).june(0)
							.july(0).august(0).september(0).october(0).november(0).december(0)
							.ContractID(Data.getContractID()).BranchID(Data.getBranchID()).endDate(Data.getEndDate())
							.startDate(Data.getStartDate()).year(Data.getYear()).rentActualID(ID).build());
				}
				// ---------------FOR TDS------------
				Optional<Tds> tdsData = tdsRepository.findById(ID);
				if (!tdsData.isPresent()) {
					tdsRepository.save(Tds.builder().january(0).february(0).march(0).april(0).may(0).june(0).july(0)
							.august(0).september(0).october(0).november(0).december(0).ContractID(Data.getContractID())
							.branchID(Data.getBranchID()).year(Data.getYear()).rentTdsID(ID).build());
				}
				jdbcTemplate1.execute("SET SQL_SAFE_UPDATES = 0");
				String tdsQuery = "update tds set " + Data.getMonth() + "=" + Data.getTdsAmount()
						+ " where rent_tdsid='" + ID + "'";
				String actualQuery = "update rent_actual set " + Data.getMonth() + "=" + Data.getAmount()
						+ " where rent_actualid='" + ID + "'";
				int tdsQueryResponce = jdbcTemplate1.update(tdsQuery);
				int actualQueryResponce = jdbcTemplate1.update(actualQuery);
				if (actualQueryResponce == 0 & tdsQueryResponce == 0)
					responce.put(Data.getContractID() + "", "NOT PAID");
				else {
					String paymentReport_Update_Query = "update payment_report set actual_amount=" + Data.getAmount()
							+ ",tds=" + Data.getTdsAmount() + " where id='" + Data.getContractID() + "-"
							+ Data.getMonth() + "/" + Data.getYear() + "'";
					jdbcTemplate1.update(paymentReport_Update_Query);

					// ----Modify Variance-----{BASE ON ACTUALAMOUNT)
					Optional<Variance> optationaVariance = varianceRepository
							.findById(Data.getContractID() + "-" + Data.getMonth() + "-" + Data.getYear());
					if (optationaVariance.isPresent())
						varianceRepository.delete(optationaVariance.get());

					if ((Data.getMonthRent() - Data.getAmount()) != 0.0) {
						try {
							varianceRepository.save(Variance.builder().branchID(Data.getBranchID())
									.contractID(Data.getContractID() + "")
									.contractInfo(rentContractRepository.findById(Data.getContractID()).get())
									.dateTime(LocalDate.now())
									.flag(getFlagDate(Data.getMonth(), Data.getYear(), "start")).month(Data.getMonth())
									.remark(null).varianceAmount(Data.getMonthRent() - Data.getAmount())
									.varianceID(Data.getContractID() + "-" + Data.getMonth() + "-" + Data.getYear())
									.year(Data.getYear()).build());
							// Payment Report Generated -> update Table
							getPaymentReport(Data.getContractID() + "", Data.getMonth(), Data.getYear() + "");
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
					// -------------------
					responce.put(Data.getContractID() + "",
							"PAID:-[Actual:" + Data.getAmount() + "|TDS:" + Data.getTdsAmount() + "]");
				}
			});
		}
		return responce;
	}

	@Override
	public provisionDto addprovision(String provisionType, provisionDto provisionDto) {

		provision provision = new provision();
		BeanUtils.copyProperties(provisionDto, provision);
		if (provisionType.equalsIgnoreCase("Reverse"))
			provision.setProvisionAmount(-provisionDto.getProvisionAmount());
		provision.setDateTime(LocalDate.now());
		provision.setProvisiontype(provisionType);
		provision.setProvisionID(
				provisionDto.getContractID() + "-" + provisionDto.getMonth() + "-" + provisionDto.getYear());
		// Flag Value is use only for to generate Payment report.
		try {
			provision.setFlag(getFlagDate(provisionDto.getMonth(), provisionDto.getYear(), "start"));
		} catch (Exception e) {
			System.out.println(e);
		} // setting some Date base on Month&Year
		provision save = provisionRepository.save(provision);
		if (save != null) {
			// once provision make Payment data updated if Exist or else its create new one.
			getPaymentReport(provisionDto.getContractID(), provisionDto.getMonth(), provisionDto.getYear() + "");
		}
		BeanUtils.copyProperties(save, provisionDto);

		return provisionDto;

	}

	@Override
	public Responce getprovision(String flag, String year) {
		List<provisionDto> allprovisionDto = new ArrayList<>();
		if (flag.equalsIgnoreCase("all")) {
			List<provision> allprovision = provisionRepository.findAll();
			allprovisionDto = allprovision.stream().filter(e -> e.getYear() == Integer.parseInt(year)).sorted(Comparator
					.comparing(provision::getContractID).thenComparing(Comparator.comparing(provision::getFlag)))
					.map(e -> {
						RentContractDto contractdto = new RentContractDto();
						Optional<RentContract> optionalContract = rentContractRepository
								.findById(Integer.parseInt(e.getContractID()));
						if (optionalContract.isPresent())
							BeanUtils.copyProperties(optionalContract.get(), contractdto);
						else
							System.out.println(e.getContractID() + "-> NOT FOUND IN CONTRACT TABLE");
						provisionDto dto = new provisionDto();
						BeanUtils.copyProperties(e, dto);
						dto.setInfo(contractdto);
						return dto;
					}).collect(Collectors.toList());

			return Responce.builder().data(allprovisionDto).error(Boolean.FALSE).msg("All provision").build();
		} else {// HERE WE ARE NOT USEING YEAR FIELD...!
			List<provision> allprovision = provisionRepository.findByContractID(flag);
			allprovisionDto = allprovision.stream().sorted(Comparator.comparing(provision::getFlag)).map(e -> {
				provisionDto dto = new provisionDto();
				BeanUtils.copyProperties(e, dto);
				RentContractDto contractDto = new RentContractDto();
				BeanUtils.copyProperties(rentContractRepository.findById(Integer.parseInt(e.getContractID())).get(),
						contractDto);
				dto.setInfo(contractDto);
				return dto;
			}).collect(Collectors.toList());

			return Responce.builder().error(Boolean.FALSE).data(allprovisionDto).msg("provision Base on BranchID")
					.build();
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

	// -----------------------------

	/**
	 * @API -> To Generate Payment Report
	 * @return Report DtoObject.
	 */
	public PaymentReportDto generatePaymentreport(String contractID, String month, String year) {

		// check particular contract is applicable or not for payment report-> To avoid
		// go inside...!
		double MonthRent = 0.0;
		String SqlQuery = "SELECT " + month + " FROM rent_due e where e.contractid='" + contractID + "' and e.year='"
				+ year + "'";
		List<String> getvalue = getvalue(SqlQuery);
		if (getvalue.isEmpty() || getvalue == null)
			return null;

		MonthRent = Double.parseDouble(getvalue.get(0));

		// ---Variable Creation---
		double tds = 0.0;
		double DueValue = 0.0;
		double gross = 0.0;
		double Net = 0.0;
		double provision = 0.0;
		double Gst = 0.0;
		double ActualAmount = 0.0;
		double sdAmount = 0.0;

		RentContractDto info = new RentContractDto();
		RentContract rentContract = null;

		try {
			// ---------Contract Info---------------
			rentContract = rentContractRepository.findById(Integer.parseInt(contractID)).get();
			BeanUtils.copyProperties(rentContract, info);

			// -------Calculate DUE-----------------
			LocalDate flagDate = getFlagDate(month, Integer.parseInt(year), "start");
			String overallprovisioin = provisionRepository.getoverallprovisioin(contractID, flagDate + "");

			// Query ->To fetch RentDue Value..!
			/*
			 * @To get Monthly Rent Logic has Written At starting of method -> for Handle
			 * Error!
			 */
			if (overallprovisioin != null) {
				// -----------provision value Initiate-----------------overall active base on
				// overall active base on date provision sum ...!
				provision += Double.parseDouble(overallprovisioin);
			}
			DueValue += MonthRent;
			// ----------initiate Variance on DueValue---------
			String overAllVariance = varianceRepository.getoverallvariance(contractID, flagDate + "");
			if (overAllVariance != null) {
				DueValue += Double.parseDouble(overAllVariance);
			}
			// ----------Gross Value initiate---------
			gross = DueValue - provision;

			// ---------- SD Value initiate ---------
//			Optional<SDRecords> sdOptional = sdRepository.findById(contractID + "-" + month + "/" + year);
//			if (sdOptional != null & sdOptional.isPresent()) {
//				gross = gross - sdOptional.get().getSdAmount();
//				sdAmount = sdOptional.get().getSdAmount();
//			}

			// ----------TDS Value initiate---------
			String tdsQuery = "SELECT " + month + " FROM tds e where e.contractid='" + contractID + "' and e.year='"
					+ year + "'";
			List<String> tdsValue = getvalue(tdsQuery);
			if (!tdsValue.isEmpty() & tdsValue != null)
				tds = Double.parseDouble(getvalue(tdsQuery).get(0));
			// ----------GST Value initiate-----------
			Gst += 0.0;
			// ----------Net Value initiate-----------
			Net = gross - tds + Gst;
			// ----------Rent Actual initiate-----------
			String actualQuery = "SELECT " + month + " FROM rent_actual e where e.contractid='" + contractID
					+ "' and e.year='" + year + "'";
			List<String> actualValue = getvalue(actualQuery);
			if (!actualValue.isEmpty() & actualValue != null)
				ActualAmount = Double.parseDouble(actualValue.get(0));
		} catch (

		Exception e) {
			System.out.println(e + "---------Exception---------");
		}
		return PaymentReportDto.builder().due(DueValue).Gross(gross).Info(info).monthRent(MonthRent).net(Net)
				.provision(provision).reporttds(tds).sdAmount(sdAmount).actualAmount(ActualAmount).gstamt(Gst)
				.monthYear(month + "/" + year).build();
	}

	// =======================GENERATE FLAG DATE ===========================

	public LocalDate getFlagDate(String month, int year, String startORend) throws ParseException {
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
		else
			return LocalDate.parse(year + "-" + monthValueString + "-31");
	}

}
