package com.cagl.entity;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RentContract {

	@Id
	private int uniqueID;
	private int priviousContractID;
	private String remarks;
	private String joinaddress_Vendor;
	private String joinaddress_Premesis;
	private String branchID;
	// Lesser info
	private String lessorName;
	private String lessorContactNumber;
	private String lessorEmailAddress;
	private String lessorPanNumber;
	private String lessorGstNumber;
//	private String lessorTdsNumber;
	private String paymentMode;

	private String nationality;
	private String contractStatus; // new & renewal

//	private String lessorElectricityBillNumber;
//	private String lessorTaxNumber;
//	private String lessorBankPassBookNumber;
//	private String panDocumentUpload;
//	private String lessorCheuque;
	private String lessorDoorNumber;
	private String lessorFloorNumber;
	private String lessorWardNo;
	private String lessorLandMark;
	private String lessorStreet;
//	private String lessorArea;
	private String lessorCity;
//	private String lessorLocation;
	private String lessorPinCode;
	private String lessorTaluka;
	private String lessorDistrict;
	private String lessorState;

	private String lesseeBranchName;
	private String lesseeAreaName;
	private String lesseeDivision;
	private String lesseeZone;
	private String lesseeState;
	private String lesseeBranchType;
	private String lesseeApproverrenewals;
	private String lesseeApproverRelocation;
	private String lesseeEntityDetails;

	private String premesisLocation;
	private String premesisDoorNumber;
	private String premesisFloorNumber;
	private String premesisWardNo;
	private String premesisLandMark;
	private String premesisStreet;
	private String premesisCity;
	private String plotNumber;
	private String builtupArea;
	private String premesisPinCode;
	private String premesisTaluka;
	private String premesisDistrict;
//	private String premesisBranchName;
//	private String premesisAreaName;
//	private String premesisDivision;
//	private String premesisZone;
//	private String premesisState;
	private String premesisBuildingType;
	private String schedulePrimesis; // Field is use As a EscalatedMonth for Rent_Due Calculation
	private String glName;
	private String glEmpId;
	private String signedDate;
	private String northPremesis;
	private String southPremesis;
	private String eastPremesis;
	private String westPremesis;

	private LocalDate agreementSignDate;
	private String agreementTenure;
	private String agreementActivationStatus;
	private LocalDate agreementStartDate;
	private LocalDate agreementEndDate;
	private LocalDate rentStartDate;
	private LocalDate rentEndDate;
	private String maintaineneCharge;
	private String waterCharge;
	private String electricity;
	private String documentType;
	private String documentPath;

	private int securityDepositAmount;
	private LocalDate securityDepositPaymentDate;
	private String securityDepositUtr;
	private String securityDepositLockinPeriod;
	private String securityDepositnoticePeriod;
	private String securityDepositExitTerm;
	private int standardDeducition;

	// Recipiants Details
	private String lessorRecipiantsName;
	private String lessorBankName;
	private String lessorBranchName;
	private String lessorIfscNumber;
	private String lessorAccountNumber;
	private String panNo;
	private String gstNo;
	private double lessorRentAmount;

	private String escalation;
	private String tds;
	private String gst;

	private String lattitude;
	private String longitude;
	private String gpsCoordinates;

	private double monthlyRent;

	// document path
	private String lessorElectricityBillPath;
	private String lessorTaxNumberPath;
	private String lessorBankPassBookPath;
	private String panDocumentPath;
	
	//
	private String maker;
	private String mTimeZone;
	
	private String editer;
	private String eTimeZone;
	
	
	private String Checker;
	private String CTimeZone;
	
	private String contractZone;

	@OneToMany(mappedBy = "contractInfo", fetch = FetchType.EAGER)
//	@Exclude
	private List<Variance> variances;

}
