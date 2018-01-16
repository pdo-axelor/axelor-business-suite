/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2018 Axelor (<http://axelor.com>).
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
package com.axelor.apps.businessproject.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.hr.db.Timesheet;
import com.axelor.apps.hr.db.TimesheetLine;
import com.axelor.apps.hr.service.timesheet.TimesheetServiceImpl;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.project.db.repo.ProjectRepository;
import com.axelor.auth.db.User;
import com.axelor.exception.AxelorException;

public class TimesheetProjectServiceImpl extends TimesheetServiceImpl{
	@Override
	public List<InvoiceLine> createInvoiceLines(Invoice invoice, List<TimesheetLine> timesheetLineList, int priority) throws AxelorException  {

		List<InvoiceLine> invoiceLineList = new ArrayList<InvoiceLine>();
		int count = 0;
		DateTimeFormatter ddmmFormat = DateTimeFormatter.ofPattern("dd/MM");
		HashMap<String, Object[]> timeSheetInformationsMap = new HashMap<String, Object[]>();
		//Check if a consolidation by product and user must be done
		boolean consolidate = appHumanResourceService.getAppTimesheet().getConsolidateTSLine();

		for (TimesheetLine timesheetLine : timesheetLineList) {
			Object[] tabInformations = new Object[6];
			tabInformations[0] = timesheetLine.getProduct();
			tabInformations[1] = timesheetLine.getUser();
			//Start date
			tabInformations[2] = timesheetLine.getDate();
			//End date, useful only for consolidation
			tabInformations[3] = timesheetLine.getDate();
			tabInformations[4] = timesheetLine.getVisibleDuration();
			tabInformations[5] = timesheetLine.getProject();

			String key = null;
			if(consolidate){
				key = timesheetLine.getProduct().getId() + "|" + timesheetLine.getUser().getId() + "|" + timesheetLine.getProject().getId();
				if (timeSheetInformationsMap.containsKey(key)){
					tabInformations = timeSheetInformationsMap.get(key);
					//Update date
					if (timesheetLine.getDate().compareTo((LocalDate)tabInformations[2]) < 0){
						//If date is lower than start date then replace start date by this one
						tabInformations[2] = timesheetLine.getDate();
					}else if (timesheetLine.getDate().compareTo((LocalDate)tabInformations[3]) > 0){
						//If date is upper than end date then replace end date by this one
						tabInformations[3] = timesheetLine.getDate();
					}
					tabInformations[4] = ((BigDecimal)tabInformations[4]).add(timesheetLine.getVisibleDuration());
				}else{
					timeSheetInformationsMap.put(key, tabInformations);
				}
			}else{
				key = String.valueOf(timesheetLine.getId());
				timeSheetInformationsMap.put(key, tabInformations);
			}

			timesheetLine.setInvoiced(true);

		}

		for(Object[] timesheetInformations : timeSheetInformationsMap.values())  {

			String strDate = null;
			Product product = (Product)timesheetInformations[0];
			User user = (User)timesheetInformations[1];
			LocalDate startDate = (LocalDate)timesheetInformations[2];
			LocalDate endDate = (LocalDate)timesheetInformations[3];
			BigDecimal visibleDuration = (BigDecimal) timesheetInformations[4];
			Project project = (Project) timesheetInformations[5];
			if (consolidate){
				strDate = startDate.format(ddmmFormat) + " - " + endDate.format(ddmmFormat);
			}else{
				strDate = startDate.format(ddmmFormat);
			}

			invoiceLineList.addAll(this.createInvoiceLine(invoice, product, user, strDate, visibleDuration, priority*100+count));
			invoiceLineList.get(0).setProject(project);
			count++;
		}

		return invoiceLineList;

	}
	
	@Override
	public TimesheetLine createTimesheetLine(Project project, Product product, User user, LocalDate date, Timesheet timesheet, BigDecimal hours, String comments){
		TimesheetLine timesheetLine = super.createTimesheetLine(project, product, user, date, timesheet, hours, comments);
		
		if(project != null && (project.getProjInvTypeSelect() == ProjectRepository.INVOICING_TYPE_TIME_BASED || (project.getParentProject() != null && project.getParentProject().getProjInvTypeSelect() == ProjectRepository.INVOICING_TYPE_TIME_BASED)))
				timesheetLine.setToInvoice(true);
		
		return timesheetLine;
	}

}
