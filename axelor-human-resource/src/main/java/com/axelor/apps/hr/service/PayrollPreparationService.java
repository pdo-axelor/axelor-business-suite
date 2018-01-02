/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2017 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.hr.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.axelor.app.AppSettings;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.base.service.weeklyplanning.WeeklyPlanningService;
import com.axelor.apps.hr.db.*;
import com.axelor.apps.hr.db.repo.*;
import com.axelor.apps.hr.exception.IExceptionMessage;
import com.axelor.apps.hr.service.config.HRConfigService;
import com.axelor.apps.hr.service.leave.LeaveService;
import com.axelor.apps.tool.file.CsvTool;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class PayrollPreparationService {
	
	protected LeaveService leaveService;
	protected LeaveRequestRepository leaveRequestRepo;
	protected WeeklyPlanningService weeklyPlanningService;
	
	@Inject
	protected PayrollPreparationRepository payrollPreparationRepo;
	
	@Inject
	protected AppBaseService appBaseService;
	
	@Inject
	HRConfigService hrConfigService;
	
	@Inject
	public PayrollPreparationService(LeaveService leaveService, LeaveRequestRepository leaveRequestRepo, WeeklyPlanningService weeklyPlanningService){
		
		this.leaveService = leaveService;
		this.leaveRequestRepo = leaveRequestRepo;
		this.weeklyPlanningService = weeklyPlanningService;
	}
	
	public PayrollPreparation generateFromEmploymentContract(PayrollPreparation payrollPreparation, EmploymentContract employmentContract){
		if(payrollPreparation.getEmployee() == null){
			payrollPreparation.setEmployee(employmentContract.getEmployee());
		}
		if(payrollPreparation.getCompany() == null){
			payrollPreparation.setCompany(employmentContract.getPayCompany());
		}
		if(payrollPreparation.getEmploymentContract() == null){
			payrollPreparation.setEmploymentContract(employmentContract);
		}

		payrollPreparation.setOtherCostsEmployeeSet(employmentContract.getOtherCostsEmployeeSet());
		payrollPreparation.setAnnualGrossSalary(employmentContract.getAnnualGrossSalary());
		return payrollPreparation;
	}
	
	
	public List<PayrollLeave> fillInPayrollPreparation(PayrollPreparation payrollPreparation) throws AxelorException{
		
		List<PayrollLeave> payrollLeaveList = fillInLeaves(payrollPreparation);
		
		payrollPreparation.setDuration(this.computeWorkingDaysNumber(payrollPreparation,payrollLeaveList));
		
		payrollPreparation.setExpenseAmount(this.computeExpenseAmount(payrollPreparation));
		payrollPreparation.setLunchVoucherNumber(this.computeLunchVoucherNumber(payrollPreparation));
		payrollPreparation.setEmployeeBonusAmount( computeEmployeeBonusAmount(payrollPreparation) );
		payrollPreparation.setExtraHoursNumber( computeExtraHoursNumber(payrollPreparation) );
		
		return payrollLeaveList;
	}
	
	
	public List<PayrollLeave> fillInLeaves(PayrollPreparation payrollPreparation) throws AxelorException{
		
		List<PayrollLeave> payrollLeaveList = new ArrayList<PayrollLeave>();
		LocalDate fromDate = payrollPreparation.getPeriod().getFromDate();
		LocalDate toDate = payrollPreparation.getPeriod().getToDate();
		Employee employee = payrollPreparation.getEmployee();
		
		if(employee.getPublicHolidayPlanning() == null){
			throw new AxelorException(payrollPreparation, IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.EMPLOYEE_PUBLIC_HOLIDAY),employee.getName());
		}
		if(employee.getPlanning()== null){
			throw new AxelorException(payrollPreparation, IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.EMPLOYEE_PLANNING),employee.getName());
		}
		
		List<LeaveRequest> leaveRequestList = leaveRequestRepo.all().filter("self.statusSelect = ?4 AND self.user.employee = ?3 AND self.fromDate <= ?1 AND self.toDate >= ?2",toDate, fromDate,employee, LeaveRequestRepository.STATUS_VALIDATED).fetch();
		
		for (LeaveRequest leaveRequest : leaveRequestList) {
			
			PayrollLeave payrollLeave = new PayrollLeave();
			
			if(leaveRequest.getFromDate().isBefore(fromDate)) { 
				payrollLeave.setFromDate(fromDate); 
			}else{ 
				payrollLeave.setFromDate(leaveRequest.getFromDate()); 
			}
			
			if(leaveRequest.getToDate().isAfter(toDate)){
				payrollLeave.setToDate(toDate);
			}else{
				payrollLeave.setToDate(leaveRequest.getToDate());
			}
			
			payrollLeave.setDuration(leaveService.computeLeaveDaysByLeaveRequest(fromDate, toDate, leaveRequest, employee));
			payrollLeave.setLeaveReason(leaveRequest.getLeaveLine().getLeaveReason());
			payrollLeave.setLeaveRequest(leaveRequest);
			payrollLeaveList.add(payrollLeave);
		}
		return payrollLeaveList;
	}
	
	
	public BigDecimal computeWorkingDaysNumber(PayrollPreparation payrollPreparation, List<PayrollLeave> payrollLeaveList){
		LocalDate fromDate = payrollPreparation.getPeriod().getFromDate();
		LocalDate toDate = payrollPreparation.getPeriod().getToDate();
		LocalDate itDate = LocalDate.parse(fromDate.toString(), DateTimeFormatter.ISO_DATE);
		BigDecimal workingDays = BigDecimal.ZERO;
		BigDecimal leaveDays = BigDecimal.ZERO;
		while(!itDate.isAfter(toDate)){
			workingDays = workingDays.add(new BigDecimal(weeklyPlanningService.workingDayValue(payrollPreparation.getEmployee().getPlanning(), itDate)));
			itDate = itDate.plusDays(1);
		}
		if(payrollLeaveList != null){
			for (PayrollLeave payrollLeave : payrollLeaveList) {
				workingDays = workingDays.subtract(payrollLeave.getDuration());
				leaveDays = leaveDays.add(payrollLeave.getDuration());
			}
		}
		payrollPreparation.setLeaveDuration(leaveDays);
		return workingDays;
	}
	
	
	public BigDecimal computeExtraHoursNumber(PayrollPreparation payrollPreparation){
		LocalDate fromDate = payrollPreparation.getPeriod().getFromDate();
		LocalDate toDate = payrollPreparation.getPeriod().getToDate();
		BigDecimal extraHoursNumber = BigDecimal.ZERO;
		for(ExtraHoursLine extraHoursLine : Beans.get(ExtraHoursLineRepository.class).all().filter("self.user.employee = ?1 AND self.extraHours.statusSelect = 3 AND self.date BETWEEN ?2 AND ?3 AND (self.payrollPreparation = null OR self.payrollPreparation.id = ?4)", payrollPreparation.getEmployee(), fromDate, toDate, payrollPreparation.getId()).fetch()){
			payrollPreparation.addExtraHoursLineListItem(extraHoursLine);
			extraHoursNumber = extraHoursNumber.add( extraHoursLine.getQty() );
		}
		return extraHoursNumber;
	}
	
	public BigDecimal computeExpenseAmount(PayrollPreparation payrollPreparation){
		BigDecimal expenseAmount = BigDecimal.ZERO;
        List<Expense> expenseList = Beans.get(ExpenseRepository.class).all()
                .filter("self.user.employee = ?1 "
                        + "AND self.statusSelect = ?2 "
                        + "AND (self.payrollPreparation IS NULL OR self.payrollPreparation.id = ?3) "
                        + "AND self.companyCbSelect = ?4 "
                        + "AND self.validationDate BETWEEN ?5 AND ?6",
                        payrollPreparation.getEmployee(),
                        ExpenseRepository.STATUS_VALIDATED,
                        payrollPreparation.getId(),
                        ExpenseRepository.COMPANY_CB_PAYMENT_NO,
                        payrollPreparation.getPeriod().getFromDate(),
                        payrollPreparation.getPeriod().getToDate())
                .fetch();
		for (Expense expense : expenseList) {
			expenseAmount = expenseAmount.add(expense.getInTaxTotal());
			payrollPreparation.addExpenseListItem(expense);
		}
		return expenseAmount;
	}
	
	public BigDecimal computeLunchVoucherNumber(PayrollPreparation payrollPreparation){
		BigDecimal lunchVoucherNumber = BigDecimal.ZERO;
		List<LunchVoucherMgtLine> lunchVoucherList = Beans.get(LunchVoucherMgtLineRepository.class).all().filter("self.employee = ?1 AND self.lunchVoucherMgt.statusSelect = 3 AND (self.payrollPreparation = null OR self.payrollPreparation.id = ?2) AND self.lunchVoucherMgt.payPeriod = ?3", payrollPreparation.getEmployee(), payrollPreparation.getId(), payrollPreparation.getPeriod()).fetch();
		for (LunchVoucherMgtLine lunchVoucherMgtLine : lunchVoucherList) {
			lunchVoucherNumber = lunchVoucherNumber.add(new BigDecimal(lunchVoucherMgtLine.getLunchVoucherNumber()) );
			lunchVoucherNumber = lunchVoucherNumber.add(new BigDecimal(lunchVoucherMgtLine.getInAdvanceNbr()));
			payrollPreparation.addLunchVoucherMgtLineListItem(lunchVoucherMgtLine);
		}
		return lunchVoucherNumber;
	}
	
	public BigDecimal computeEmployeeBonusAmount(PayrollPreparation payrollPreparation){
		BigDecimal employeeBonusAmount = BigDecimal.ZERO;
		List<EmployeeBonusMgtLine> employeeBonusList =
				Beans.get(EmployeeBonusMgtLineRepository.class).all()
						.filter("self.employee = ?1" +
								" AND self.employeeBonusMgt.statusSelect = ?4" +
								" AND (self.payrollPreparation = null" +
									" OR self.payrollPreparation.id = ?2)" +
								" AND self.employeeBonusMgt.payPeriod = ?3",
								payrollPreparation.getEmployee(),
								payrollPreparation.getId(),
								payrollPreparation.getPeriod(),
								EmployeeBonusMgtRepository.STATUS_CALCULATED)
						.fetch();
		for (EmployeeBonusMgtLine employeeBonusMgtLine : employeeBonusList) {
			payrollPreparation.addEmployeeBonusMgtLineListItem(employeeBonusMgtLine);
			employeeBonusAmount = employeeBonusAmount.add( employeeBonusMgtLine.getAmount() );
		}
		return employeeBonusAmount;
	}
	
	@Transactional
	public String exportSinglePayrollPreparation(PayrollPreparation payrollPreparation) throws IOException, AxelorException{
		
		List<String[]> list = new ArrayList<String[]>();
		
		String item[] = new String[5];
		item[0] = payrollPreparation.getEmployee().getName();
		item[1] = payrollPreparation.getDuration().toString();
		item[2] = payrollPreparation.getLunchVoucherNumber().toString();
		item[3] = payrollPreparation.getEmployeeBonusAmount().toString();
		item[4] = payrollPreparation.getExtraHoursNumber().toString();
		list.add(item);
		
		String fileName = this.getPayrollPreparationExportName();
		String filePath = AppSettings.get().get("file.upload.dir");
		
		
		new File(filePath).mkdirs();
		CsvTool.csvWriter(filePath, fileName, ';', getPayrollPreparationExportHeader(), list);
		
		payrollPreparation.setExported(true);
		payrollPreparation.setExportDate(Beans.get(AppBaseService.class).getTodayDate());
		
		payrollPreparationRepo.save(payrollPreparation);
		
		Path path = Paths.get(filePath + System.getProperty("file.separator") +fileName);
		
		try (InputStream is = new FileInputStream(path.toFile())) {
			Beans.get(MetaFiles.class).attach(is, fileName, payrollPreparation);
		}
		
		return filePath + System.getProperty("file.separator") +fileName;
	}
	
	
	public String[] createExportFileLine(PayrollPreparation payrollPreparation){
		
		String item[] = new String[7];
		item[0] = payrollPreparation.getEmployee().getExportCode();
		item[1] = payrollPreparation.getEmployee().getContactPartner().getName();
		item[2] = payrollPreparation.getEmployee().getContactPartner().getFirstName();
		return item;
	}
	
	public String exportMeilleureGestionPayrollPreparation(PayrollPreparation payrollPreparation) throws AxelorException, IOException{
		
		List<String[]> list = new ArrayList<String[]>();
		
		exportMeilleureGestion(payrollPreparation, list);
		
		
		String fileName = this.getPayrollPreparationExportName();
		String filePath = AppSettings.get().get("file.upload.dir");
		new File(filePath).mkdirs();
		
		CsvTool.csvWriter(filePath, fileName, ';', getPayrollPreparationMeilleurGestionExportHeader(), list);
		
		Path path = Paths.get(filePath + System.getProperty("file.separator") +fileName);
		
		try (InputStream is = new FileInputStream(path.toFile())) {
			Beans.get(MetaFiles.class).attach(is, fileName, payrollPreparation);
		}
		
		return filePath + System.getProperty("file.separator") +fileName;
	}
	
	@Transactional
	public void exportMeilleureGestion(PayrollPreparation payrollPreparation, List<String[]> list ) throws AxelorException{
		
		HRConfig hrConfig = hrConfigService.getHRConfig(payrollPreparation.getCompany());
		
		
		// LEAVES
		if (payrollPreparation.getLeaveDuration().compareTo( BigDecimal.ZERO ) > 0 ) {
			List<PayrollLeave> payrollLeaveList = fillInLeaves(payrollPreparation);
			for (PayrollLeave payrollLeave : payrollLeaveList) {
				if (payrollLeave.getLeaveReason().getPayrollPreprationExport()){
					String leaveLine[] = createExportFileLine(payrollPreparation);
					leaveLine[3] = payrollLeave.getLeaveReason().getExportCode();
					leaveLine[4] = payrollLeave.getFromDate().format(DateTimeFormatter.ofPattern("dd/MM/YYYY"));
					leaveLine[5] = payrollLeave.getToDate().format(DateTimeFormatter.ofPattern("dd/MM/YYYY"));
					leaveLine[6] = payrollLeave.getDuration().toString();
					list.add(leaveLine);
				}
			}
		}
		
		// LUNCH VOUCHER MANAGEMENT
		if (payrollPreparation.getLunchVoucherNumber().compareTo(BigDecimal.ZERO) > 0){
			String lunchVoucherLine[] = createExportFileLine(payrollPreparation);
			lunchVoucherLine[3] = hrConfig.getExportCodeForLunchVoucherManagement();
			lunchVoucherLine[6] = payrollPreparation.getLunchVoucherNumber().toString();
			list.add(lunchVoucherLine);
		}
		
		
		// EMPLOYEE BONUS MANAGEMENT
		if (payrollPreparation.getEmployeeBonusAmount().compareTo(BigDecimal.ZERO) > 0){
			Map<String, BigDecimal> map = new HashMap<String, BigDecimal>();
			for (EmployeeBonusMgtLine bonus : payrollPreparation.getEmployeeBonusMgtLineList() ) {
				if (bonus.getEmployeeBonusMgt().getEmployeeBonusType().getPayrollPreparationExport()){
						if ( map.containsKey(bonus.getEmployeeBonusMgt().getEmployeeBonusType().getExportCode()) ){
							map.put(bonus.getEmployeeBonusMgt().getEmployeeBonusType().getExportCode(), bonus.getAmount().add(map.get(bonus.getEmployeeBonusMgt().getEmployeeBonusType().getExportCode())) ); 
					}else{
						map.put(bonus.getEmployeeBonusMgt().getEmployeeBonusType().getExportCode(), bonus.getAmount()); 
					}
				}
			}
			for ( Map.Entry<String, BigDecimal> entry : map.entrySet() ) {
				String employeeBonusLine[] = createExportFileLine(payrollPreparation);
				employeeBonusLine[3] = entry.getKey();
				employeeBonusLine[6] = entry.getValue().toString();
				list.add(employeeBonusLine);
			}
			
		}
		
		//EXTRA HOURS 
		if ( payrollPreparation.getExtraHoursNumber().compareTo( BigDecimal.ZERO ) > 0 ){
			String extraHourLine[] = createExportFileLine(payrollPreparation);
			extraHourLine[3] = hrConfig.getExportCodeForLunchVoucherManagement();
			extraHourLine[6] = payrollPreparation.getExtraHoursNumber().toString();
			list.add(extraHourLine);
		}
		
		payrollPreparation.setExported(true);
		payrollPreparation.setExportDate(appBaseService.getTodayDate());
		payrollPreparation.setExportTypeSelect(HrBatchRepository.EXPORT_TYPE_MEILLEURE_GESTION);
		payrollPreparationRepo.save(payrollPreparation);
	}
	
	
	public String getPayrollPreparationExportName(){
		return I18n.get("Payroll preparation") + " - " + Beans.get(AppBaseService.class).getTodayDateTime().toString() + ".csv";
	}
	
	public String[] getPayrollPreparationExportHeader(){
		
		String headers[] = new String[5];
		headers[0] = I18n.get("Employee");
		headers[1] = I18n.get("Working days' number");
		headers[2] = I18n.get("Lunch vouchers' number");
		headers[3] = I18n.get("Employee bonus amount");
		headers[4] = I18n.get("Extra hours' number");
		return headers;
	}
	
	
	public String[] getPayrollPreparationMeilleurGestionExportHeader(){
		String headers[] = new String[7];
		headers[0] = I18n.get("Registration number");
		headers[1] = I18n.get("Employee lastname");
		headers[2] = I18n.get("Employee firstname");
		headers[3] = I18n.get("Code");
		headers[4] = I18n.get("Start date");
		headers[5] = I18n.get("End date");
		headers[6] = I18n.get("Value");
		return headers;
	}
	
}
