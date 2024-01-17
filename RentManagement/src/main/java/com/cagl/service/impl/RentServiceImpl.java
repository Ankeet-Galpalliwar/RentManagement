package com.cagl.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.cagl.dto.MakeActualDto;
import com.cagl.dto.PaymentReportDto;
import com.cagl.dto.RentContractDto;
import com.cagl.dto.Rentduecalculation;
import com.cagl.dto.Responce;
import com.cagl.dto.provisionDto;
import com.cagl.entity.PaymentReport;
import com.cagl.entity.RentContract;
import com.cagl.entity.RentDue;
import com.cagl.entity.SDRecords;
import com.cagl.entity.provision;
import com.cagl.entity.rentActual;
import com.cagl.repository.BranchDetailRepository;
import com.cagl.repository.PaymentReportRepository;
import com.cagl.repository.RentActualRepository;
import com.cagl.repository.RentContractRepository;
import com.cagl.repository.RfBrachRepository;
import com.cagl.repository.SDRecoardRepository;
import com.cagl.repository.ifscMasterRepository;
import com.cagl.repository.provisionRepository;
import com.cagl.repository.rentDueRepository;
import com.cagl.service.RentService;

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
					BeanUtils.copyProperties(rentContractRepository.findById(Integer.parseInt(e.getContractID())).get(),
							rentContractDto);
					prDto.add(PaymentReportDto.builder().actualAmount(e.getActualAmount()).due(e.getDue())
							.Gross(e.getGross()).gstamt(e.getGST()).Info(rentContractDto)
							.monthlyRent(e.getMonthlyRent()).monthYear(e.getMonth() + "/" + e.getYear()).net(e.getNet())
							.provision(e.getProvision()).tds(e.getTds()).build());
				});
			}
			return Responce.builder().data(prDto).error(Boolean.FALSE).msg("Payment Report Data..!").build();
		} else {
			PaymentReportDto generatereport = generatePaymentreport(contractID, month, year);
			// Here we are saving(Generated Payment Report) Data for audit purpose.
			paymentReportRepository.save(PaymentReport.builder().branchID(generatereport.getInfo().getBranchID())
					.contractID(contractID).due(generatereport.getDue()).Gross(generatereport.getGross())
					.ID(contractID + "-" + generatereport.getMonthYear()).month(month)
					.monthlyRent(generatereport.getMonthlyRent()).net(generatereport.getNet())
					.provision(generatereport.getProvision()).ActualAmount(generatereport.getActualAmount())
					.tds(generatereport.getTds()).GST(generatereport.getGstamt()).year(year).build());
			return Responce.builder().data(generatereport).error(Boolean.FALSE).msg("Payment Report Data..!").build();
		}
	}

	@Override
	public Map<String, String> makeactual(List<MakeActualDto> actualDto) {
		Map<String, String> responce = new HashMap<>();
		if (!actualDto.isEmpty() & actualDto != null) {
			actualDto.stream().forEach(Data -> {
				String ActualID = Data.getContractID() + "-" + Data.getYear();
				Optional<rentActual> actualData = actualRepository.findById(ActualID);
				if (!actualData.isPresent()) {
					actualRepository.save(rentActual.builder().january(0).february(0).march(0).april(0).may(0).june(0)
							.july(0).august(0).september(0).october(0).november(0).december(0)
							.ContractID(Data.getContractID()).BranchID(Data.getBranchID()).endDate(Data.getEndDate())
							.startDate(Data.getStartDate()).year(Data.getYear()).rentActualID(ActualID).build());
				}
				String query = "update rent_actual set " + Data.getMonth() + "=" + Data.getAmount()
						+ " where rent_actualid='" + ActualID + "'";
				jdbcTemplate1.execute("SET SQL_SAFE_UPDATES = 0");
				int updateResponce = jdbcTemplate1.update(query);
				if (updateResponce == 0)
					responce.put(Data.getContractID() + "", "NOT PAID");
				else {
					String ActualUpdateQuery = "update payment_report set actual_amount=" + Data.getAmount()
							+ " where id='" + Data.getContractID() + "-" + Data.getMonth() + "/" + Data.getYear() + "'";
					jdbcTemplate1.update(ActualUpdateQuery);
					responce.put(Data.getContractID() + "", "PAID");
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
				provisionDto.getContractID() + "-" + provisionDto.getMonth() + "/" + provisionDto.getYear());
		// Flag Value is use only for to generate Payment report.
		try {
			provision.setFlag(getFlagDate(provisionDto.getMonth(), provisionDto.getYear(), "start"));
		} catch (Exception e) {
			System.out.println(e);
		} // setting some Date base on Month&Year
		provision save = provisionRepository.save(provision);
		if (save != null) {
			// once provision make Payment data updated if Exist or else its create new one.
			generatePaymentreport(provisionDto.getContractID(), provisionDto.getMonth(), provisionDto.getYear() + "");
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
		} else {
			List<provision> allprovision = provisionRepository.getprovion(flag, year);
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
			/*
			 * @To get Monthly Rent Logic has Written At starting of method -> for Handle
			 * Error!
			 */

			if (overallprovisioin != null) {
				// -----------provision value Initiate-----------------overall active base on
				// overall active base on date provision sum ...!
				provision += Double.parseDouble(overallprovisioin);

				DueValue += Double.parseDouble(overallprovisioin) + MonthRent;
			} else
				DueValue += MonthRent;

			// ----------Gross Value initiate---------
			gross = DueValue - provision;

			Optional<SDRecords> sdOptional = sdRepository.findById(contractID + "-" + month + "/" + year);
			if (sdOptional != null & sdOptional.isPresent()) {
				gross = gross - sdOptional.get().getSdAmount();
				sdAmount = sdOptional.get().getSdAmount();
			}

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
			if (overallTDSValue > 240000)// IF(TDS->right)->Here TDS Value modify Base on Gross Value..!
				tds = Math.round((gross * (10 / 100.0f)));// By Default its (0.0)
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
		} catch (Exception e) {
			System.out.println(e + "---------Exception---------");
		}
		return PaymentReportDto.builder().due(DueValue).Gross(gross).Info(info).monthlyRent(MonthRent).net(Net)
				.provision(provision).tds(tds).sdAmount(sdAmount).actualAmount(ActualAmount).gstamt(Gst)
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
