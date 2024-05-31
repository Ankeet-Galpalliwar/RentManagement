package com.cagl.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.cagl.Security.CustomeUserDetailService;
import com.cagl.Security.JwtAuthenticationEntryPoint;
import com.cagl.Security.JwtAutheticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	JwtAuthenticationEntryPoint authenticationEntryPoint;

	@Autowired
	JwtAutheticationFilter autheticationFilter;

	@Autowired
	CustomeUserDetailService customeUserDetailService;

	// Checker APIs
	public static final String[] checkerApis = { "/makeDue", "/changeZone", "/transferContract",
			"/ModifyPaymentReport" };
	// Maker APIs
	public static final String[] makersApis = { "/setsd", "/makeactual", "/setprovision", "/deleteProvision",
			"/BulkProvisionDelete", "/insertcontract" };
	// Checker-maker APIs
	public static final String[] CMApis = { "/getvariance", "/ifscinfo", "/getstate", "/getdistrict",
			"/filterBranchIDs", "/getBranchName", "/getprovision", "/getenure", "/generatePaymentReport",
			"/generateRawPaymentReport", "/getduereportUid", "/getduereportBid", "/getbranchids", "/getbranchdetails",
			"/renewalDetails", "/getcontracts", "/getcontractsCID", "/getcotractBranchName", "/getallcontracts",
			"/editcontracts", "/resolvealertContract", "/getLastContract", "/AlertContract", "/countprovision",
			"/getSdDetails", "/closecontract" };
	// PermitAll APi Login APi..
	public static final String[] permitAllAPIs = { "/DownloadPaymentReport", "/login", "/checkPcontract",
			"/ConvertJsontoExcel", "/countvariance", "/getnewpendingcontract", "/getupdatpendingcontract" };

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {

		auth.userDetailsService(this.customeUserDetailService).passwordEncoder(passwordEncoder());
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		// TODO Auto-generated method stub
		return super.authenticationManagerBean();
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// TODO Auto-generated method stub
		http.cors().and().csrf().disable().authorizeHttpRequests().antMatchers(permitAllAPIs).permitAll()
				.antMatchers(CMApis).hasRole("CM").antMatchers(makersApis).hasRole("MAKER").antMatchers(checkerApis)
				.hasRole("CHECKER").anyRequest().authenticated().and().exceptionHandling()
				.authenticationEntryPoint(authenticationEntryPoint).and().sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		http.addFilterBefore(this.autheticationFilter, UsernamePasswordAuthenticationFilter.class);

	}

}
