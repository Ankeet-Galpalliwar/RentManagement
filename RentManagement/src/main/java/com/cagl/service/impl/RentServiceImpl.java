package com.cagl.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
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
import com.cagl.entity.PaymentReport;
import com.cagl.entity.RawPaymentReport;
import com.cagl.entity.RentContract;
import com.cagl.entity.Tds;
import com.cagl.entity.Variance;
import com.cagl.entity.provision;
import com.cagl.entity.rentActual;
import com.cagl.repository.BranchDetailRepository;
import com.cagl.repository.PaymentReportRepository;
import com.cagl.repository.RawPaymentReportRepository;
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

@Service
public class RentServiceImpl implements RentService {
	@Autowired
	PaymentReportRepository paymentReportRepository;

	@Autowired
	RawPaymentReportRepository rawPaymentReportRepository;

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
	public Responce getPaymentReport(String contractID, String month, String year,String purpose) {
		if (contractID.equalsIgnoreCase("all")) {
			List<PaymentReport> data = paymentReportRepository.findByMonthAndYear(month, year);
			List<PaymentReportDto> prDto = new ArrayList<>();
			if (data != null) {

				data.stream().forEach(e -> {
					RentContractDto rentContractDto = new RentContractDto();
					BeanUtils.copyProperties(e.getContractInfo(), rentContractDto);
					PaymentReportDto PRDTo = PaymentReportDto.builder().actualAmount(e.getActualAmount())
							.due(e.getDue()).Gross(e.getGross()).gstamt(e.getGST() + "").Info(rentContractDto)
							.monthRent(e.getMonthlyRent()).monthYear(e.getMonth() + "/" + e.getYear())
							.net(e.getNet() + "").provision(e.getProvision() + "").reporttds(e.getTds() + "").build();
					if (e.isRedflag())
						PRDTo.setPaymentFlag(true);
					else
						PRDTo.setPaymentFlag(false);
					prDto.add(PRDTo);
				});
			}
			return Responce.builder().data(prDto).error(Boolean.FALSE).msg("Payment Report Data..!").build();
		} else {
			PaymentReportDto rawgeneratereport = generatePaymentreport(contractID, month, year, "Raw");

			// =========make Auto Actual====================
			ArrayList<MakeActualDto> actualDto = new ArrayList<>();
			MakeActualDto build = MakeActualDto.builder().branchID(rawgeneratereport.getInfo().getBranchID())
					.contractID(rawgeneratereport.getInfo().getUniqueID())
					.endDate(rawgeneratereport.getInfo().getRentEndDate()).month(month)
					.monthRent(rawgeneratereport.getMonthRent())
					.startDate(rawgeneratereport.getInfo().getRentStartDate())
					.tdsAmount(Double.parseDouble(rawgeneratereport.getReporttds())).year(Integer.parseInt(year))
					.build();
			if (rawgeneratereport.getActualAmount().equalsIgnoreCase("--")) {
				build.setAmount(rawgeneratereport.getGross() + "");
				actualDto.add(build);
				makeactual(actualDto);
				rawgeneratereport.setActualAmount(rawgeneratereport.getGross() + "");
//				System.out.println("make Actual hit sucessfully..!");

			} else {
				build.setAmount(rawgeneratereport.getActualAmount());
				actualDto.add(build);
				makeactual(actualDto);
//				System.out.println("make Actual hit sucessfully..!");

			}

//			PaymentReportDto generatereport = generatePaymentreport(contractID, month, year, "show");
//			// Here we are saving(Generated Payment Report) Data for audit purpose.
//			paymentReportRepository.save(PaymentReport.builder().branchID(generatereport.getInfo().getBranchID())
//					.contractInfo(rentContractRepository.findById(Integer.parseInt(contractID)).get())
//					.contractID(contractID).due(generatereport.getDue()).Gross(generatereport.getGross())
//					.ID(contractID + "-" + generatereport.getMonthYear()).month(month)
//					.monthlyRent(generatereport.getMonthRent()).net(Double.parseDouble(generatereport.getNet()))
//					.provision(Double.parseDouble(generatereport.getProvision()))
//					.ActualAmount(generatereport.getActualAmount())
//					.tds(Double.parseDouble(generatereport.getReporttds()))
//					.GST(Double.parseDouble(generatereport.getGstamt())).year(year).build());

			// Here we are saving(Generated RawPayment Report) Data for audit purpose.
//			rawPaymentReportRepository.save(RawPaymentReport.builder()
//					.branchID(rawgeneratereport.getInfo().getBranchID())
//					.contractInfo(rentContractRepository.findById(Integer.parseInt(contractID)).get())
//					.contractID(contractID).due(rawgeneratereport.getDue()).Gross(rawgeneratereport.getGross())
//					.ID(contractID + "-" + rawgeneratereport.getMonthYear()).month(month)
//					.monthlyRent(rawgeneratereport.getMonthRent()).net(Double.parseDouble(rawgeneratereport.getNet()))
//					.provision(Double.parseDouble(rawgeneratereport.getProvision()))
//					.ActualAmount(rawgeneratereport.getActualAmount())
//					.tds(Double.parseDouble(rawgeneratereport.getReporttds()))
//					.GST(Double.parseDouble(rawgeneratereport.getGstamt())).year(year).build());

			return Responce.builder().data(rawgeneratereport).error(Boolean.FALSE).msg("Payment Report Data..!")
					.build();
		}
	}

	@Autowired
	TdsRepository tdsRepository;

	@Override
	public Map<String, String> makeactual(List<MakeActualDto> actualDto) {
		Map<String, String> responce = new HashMap<>();// use for response
		if (!actualDto.isEmpty() & actualDto != null) {
			// Get on by one Actual Data
			LocalDate now = LocalDate.now();

			actualDto.stream().filter(e -> {
				boolean pCondition = false;//
				LocalDate flagDate = null;
				try {
					flagDate = getFlagDate(e.getMonth(), e.getYear(), "start");
				} catch (ParseException e1) {
				}

				// ====Get Previous Month Value =======
				/**
				 * @check ->IF previous Month Actual not done then not allowed to make Actual(If
				 *        rent_start Date then allowed.
				 */
				String Pmonth = flagDate.minusMonths(1).getMonth().toString();
				int Pyear = flagDate.minusMonths(1).getYear();
				List<String> getvalue = getvalue("SELECT " + Pmonth + " FROM rent_actual where year=" + Pyear
						+ " and contractid=" + e.getContractID() + "");
				if (((getvalue.isEmpty() || getvalue == null))) {
//					if (!(((e.getStartDate().getMonth().toString()).equalsIgnoreCase(e.getMonth()))
//							& (e.getStartDate().getYear() == e.getYear()))) {
//					
//						pCondition = true;
//					}
				} else if (getvalue.get(0).equalsIgnoreCase("--")) {
					pCondition = true;
				}
				if (flagDate.isAfter(now) || pCondition) {
					responce.put("--", e.getContractID() + "NOT PAID Not Eligible");
					return false;
				} else {
					return true;
				}
			}).forEach(Data -> {

				try {
					
					Double.parseDouble(Data.getAmount());// To avoid Alphanumeric and Execute Catch Block

					String ID = Data.getContractID() + "-" + Data.getYear();
					Optional<rentActual> actualData = actualRepository.findById(ID);
					// --------------FOR ACTUAL---------
					if (!actualData.isPresent()) {
						actualRepository.save(rentActual.builder().january("--").february("--").march("--").april("--")
								.may("--").june("--").july("--").august("--").september("--").october("--")
								.november("--").december("--").ContractID(Data.getContractID())
								.BranchID(Data.getBranchID()).endDate(Data.getEndDate()).startDate(Data.getStartDate())
								.year(Data.getYear()).rentActualID(ID).build());
					}
					// ---------------FOR TDS------------
					jdbcTemplate1.execute("SET SQL_SAFE_UPDATES = 0");
					Optional<Tds> tdsData = tdsRepository.findById(ID);
					if (true) {// Data.getTdsAmount() != 0
						if (!tdsData.isPresent()) {
							tdsRepository.save(Tds.builder().january(0).february(0).march(0).april(0).may(0).june(0)
									.july(0).august(0).september(0).october(0).november(0).december(0)
									.ContractID(Data.getContractID()).branchID(Data.getBranchID()).year(Data.getYear())
									.rentTdsID(ID).build());
						}
						String tdsQuery = "update tds set " + Data.getMonth() + "=" + Data.getTdsAmount()
								+ " where rent_tdsid='" + ID + "'";
						int update = jdbcTemplate1.update(tdsQuery);
					}

					String actualQuery = "update rent_actual set " + Data.getMonth() + "='" + Data.getAmount()
							+ "' where rent_actualid='" + ID + "'";
					int actualQueryResponce = jdbcTemplate1.update(actualQuery);
					if (actualQueryResponce == 0)
						responce.put("--", Data.getContractID() + "NOT PAID");
					else {
						// ----Modify Variance-----{BASE ON ACTUALAMOUNT)
						Optional<Variance> optationaVariance = varianceRepository
								.findById(Data.getContractID() + "-" + Data.getMonth() + "-" + Data.getYear());
						if (optationaVariance.isPresent())
							varianceRepository.delete(optationaVariance.get());

						if ((Data.getMonthRent() - Double.parseDouble(Data.getAmount())) != 0.0) {
							try {
								varianceRepository.save(Variance.builder().branchID(Data.getBranchID())
										.contractID(Data.getContractID() + "")
										.contractInfo(rentContractRepository.findById(Data.getContractID()).get())
										.dateTime(LocalDate.now())
										.flag(getFlagDate(Data.getMonth(), Data.getYear(), "start"))
										.month(Data.getMonth()).remark(null)
										.varianceAmount(Data.getMonthRent() - Double.parseDouble(Data.getAmount()))
										.varianceID(Data.getContractID() + "-" + Data.getMonth() + "-" + Data.getYear())
										.year(Data.getYear()).build());

							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
						// ------------(Erase Next Month Actuals And Variance)----------------
//						LocalDate nextMonthFlagDate = null;
//						try {
//							nextMonthFlagDate = getFlagDate(Data.getMonth(), Data.getYear(), "start").plusMonths(1);
//						} catch (ParseException e1) {
//						}
//						ID = Data.getContractID() + "-" + nextMonthFlagDate.getYear();
//						try {
//							String nextactualQuery = "update rent_actual set " + nextMonthFlagDate.getMonth() + "='"
//									+ "--" + "' where rent_actualid='" + ID + "'";
//							jdbcTemplate1.update(nextactualQuery);
//							Optional<Variance> nextMonthOptationaVariance = varianceRepository
//									.findById(Data.getContractID() + "-" + nextMonthFlagDate.getMonth() + "-"
//											+ nextMonthFlagDate.getYear());
//							if (nextMonthOptationaVariance.isPresent())
//								varianceRepository.delete(nextMonthOptationaVariance.get());
//							Optional<RawPaymentReport> rawData = rawPaymentReportRepository
//									.findById(Data.getContractID() + "-" + nextMonthFlagDate.getMonth() + "/"
//											+ nextMonthFlagDate.getYear());
//							if (rawData.isPresent())
//								rawPaymentReportRepository.delete(rawData.get());
//							Optional<PaymentReport> paymentreportData = paymentReportRepository
//									.findById(Data.getContractID() + "-" + nextMonthFlagDate.getMonth() + "/"
//											+ nextMonthFlagDate.getYear());
//							if (paymentreportData.isPresent())
//								paymentReportRepository.delete(paymentreportData.get());
//						} catch (Exception e) {
//							// TODO: handle exception
//						}

						// ========== Payment Report Generated -> update Table=====================

						PaymentReportDto rawgeneratereport = generatePaymentreport(Data.getContractID() + "",
								Data.getMonth() + "", Data.getYear() + "", "Raw");
						// Here we are saving(Generated Payment Report) Data for audit purpose.
						rawPaymentReportRepository
								.save(RawPaymentReport.builder().branchID(rawgeneratereport.getInfo().getBranchID())
										.contractInfo(rentContractRepository.findById(Data.getContractID()).get())
										.contractID(Data.getContractID() + "").due(rawgeneratereport.getDue())
										.Gross(rawgeneratereport.getGross())
										.ID(Data.getContractID() + "-" + rawgeneratereport.getMonthYear())
										.month(Data.getMonth() + "").monthlyRent(rawgeneratereport.getMonthRent())
										.net(Double.parseDouble(rawgeneratereport.getNet()))
										.provision(Double.parseDouble(rawgeneratereport.getProvision()))
										.ActualAmount(rawgeneratereport.getActualAmount())
										.tds(Double.parseDouble(rawgeneratereport.getReporttds()))
										.GST(Double.parseDouble(rawgeneratereport.getGstamt()))
										.year(Data.getYear() + "").build());
						// ---AND--
						PaymentReportDto generatereport = generatePaymentreport(Data.getContractID() + "",
								Data.getMonth() + "", Data.getYear() + "", "show");

						// Here we are saving(Generated Payment Report) Data for audit purpose.
						paymentReportRepository.save(PaymentReport.builder()
								.Redflag(Double.parseDouble(rawgeneratereport.getActualAmount()) == rawgeneratereport
										.getGross())
								.branchID(generatereport.getInfo().getBranchID())
								.contractInfo(rentContractRepository.findById(Data.getContractID()).get())
								.contractID(Data.getContractID() + "").due(generatereport.getDue())
								.Gross(generatereport.getGross())
								.ID(Data.getContractID() + "" + "-" + generatereport.getMonthYear())
								.month(Data.getMonth()).monthlyRent(generatereport.getMonthRent())
								.net(Double.parseDouble(generatereport.getNet()))
								.provision(Double.parseDouble(generatereport.getProvision()))
								.ActualAmount(generatereport.getActualAmount())
								.tds(Double.parseDouble(generatereport.getReporttds()))
								.GST(Double.parseDouble(generatereport.getGstamt())).year(Data.getYear() + "").build());
//						getPaymentReport(Data.getContractID() + "", Data.getMonth(), Data.getYear() + "");
						// -------------------
						responce.put(Data.getAmount(), "PAID:-" + Data.getContractID() + "[Actual:" + Data.getAmount()
								+ "|TDS:" + Data.getTdsAmount() + "]");

					}
				} catch (Exception e) {
					responce.put("--", Data.getContractID() + "NOT PAID Invalid_Actual_Amount");
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

						provisionDto dto = new provisionDto();
						BeanUtils.copyProperties(e, dto);
						dto.setInfo(contractdto);
						// Allow Delete only for Cr.Month and Cr.year(if true Allow delete )
						if ((LocalDate.now().getMonth() + "").equalsIgnoreCase(e.getMonth())
								&& LocalDate.now().getYear() == e.getYear())
							dto.setDeleteFlag(true);
						else
							dto.setDeleteFlag(false);

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
				if ((LocalDate.now().getMonth() + "").equalsIgnoreCase(e.getMonth())
						&& LocalDate.now().getYear() == e.getYear())
					dto.setDeleteFlag(true);
				else
					dto.setDeleteFlag(false);

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
			return null;
//			return "0.0";
		});
	}

	// -----------------------------

	/**
	 * @API -> To Generate Payment Report
	 * @return Report DtoObject.
	 */
	public PaymentReportDto generatePaymentreport(String contractID, String month, String year, String reportType) {

		// check particular contract is applicable or not for payment report-> To avoid
		// go inside...!
		double MonthRent = 0.0;
		String SqlQuery = "SELECT " + month + " FROM rent_due e where e.contractid='" + contractID + "' and e.year='"
				+ year + "'";
		List<String> getvalue = getvalue(SqlQuery);
		if (getvalue.isEmpty() || getvalue == null)
			MonthRent = 0.0;
		else
			MonthRent = Double.parseDouble(getvalue.get(0));

		// ---Variable Creation---
		double tds = 0.0;
		double DueValue = 0.0;
		double gross = 0.0;
		double Net = 0.0;
		double provision = 0.0;
		double Gst = 0.0;
		String ActualAmount = "";
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
			if (overallprovisioin != null) {// & !overallprovisioin.isEmpty()
				// -----------provision value Initiate-----------------overall active base on
				// overall active base on date provision sum ...!
				provision += Double.parseDouble(overallprovisioin);
			}
			DueValue += MonthRent;
			// ----------initiate Variance on DueValue---------

			if (reportType.equalsIgnoreCase("Raw")) {

				String overAllVariance = varianceRepository.getoverallvariance(contractID, flagDate + "");
				if (overAllVariance != null) {// & !overAllVariance.isEmpty()
					DueValue += Double.parseDouble(overAllVariance);
				}
			} else {
				String overAllVariance = varianceRepository.getoverallvarianceforpaymentReport(contractID,
						flagDate + "");
				if (overAllVariance != null) {// & !overAllVariance.isEmpty()
					DueValue += Double.parseDouble(overAllVariance);
				}
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
//			String tdsQuery = "SELECT " + month + " FROM tds e where e.contractid='" + contractID + "' and e.year='"
//					+ year + "'";
//			List<String> tdsValue = getvalue(tdsQuery);
//			if (!tdsValue.isEmpty() & tdsValue != null) {
//				String temptds = getvalue(tdsQuery).get(0);
//				if (reportType.equalsIgnoreCase("Raw")) {
//					tds = Double.parseDouble(temptds);
//				} else {
//					if (Double.parseDouble(temptds)> 0) {//
//						tds = Math.round(((10 / 100.0f) * gross));
//					}
//				}
//			}
			// --------------------------------------

			if (Double.parseDouble(rentContract.getTds()) > 0)
				tds = Math.round(((Double.parseDouble(rentContract.getTds()) / 100.0f) * gross));

			// ----------GST Value initiate-----------
			double gstpercent = Double.parseDouble(rentContract.getGst());

			Gst += Math.round(((gstpercent / 100.0f) * gross));
			// ----------Net Value initiate-----------
			Net = gross - tds + Gst;
			// ----------Rent Actual initiate-----------
			String actualQuery = "SELECT " + month + " FROM rent_actual e where e.contractid='" + contractID
					+ "' and e.year='" + year + "'";
			List<String> actualValue = getvalue(actualQuery);
			if (!actualValue.isEmpty() & actualValue != null)
				ActualAmount = actualValue.get(0);
			else
				ActualAmount = "--";
		} catch (

		Exception e) {
			System.out.println(e + "---------Exception---------");
		}
//		System.out.println(PaymentReportDto.builder().due(DueValue).Gross(gross).Info(info).monthRent(MonthRent)
//				.net(Net + "").provision(provision + "").reporttds(tds + "").sdAmount(sdAmount)
//				.actualAmount(ActualAmount).gstamt(Gst + "").monthYear(month + "/" + year).build());
		return PaymentReportDto.builder().due(DueValue).Gross(gross).Info(info).monthRent(MonthRent).net(Net + "")
				.provision(provision + "").reporttds(tds + "").sdAmount(sdAmount).actualAmount(ActualAmount)
				.gstamt(Gst + "").monthYear(month + "/" + year).build();
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
