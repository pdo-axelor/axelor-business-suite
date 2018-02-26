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
package com.axelor.apps.hr.service.timesheet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import java.time.LocalDate;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.Timesheet;
import com.axelor.apps.hr.db.TimesheetLine;
import com.axelor.apps.message.db.Message;
import com.axelor.apps.project.db.Project;
import com.axelor.auth.db.User;
import com.axelor.exception.AxelorException;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.persist.Transactional;

public interface TimesheetService {
	public void getTimeFromTask(Timesheet timesheet);
	
	
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void confirm(Timesheet timesheet) throws AxelorException;
	
	public Message sendConfirmationEmail(Timesheet timesheet) throws AxelorException, ClassNotFoundException, InstantiationException, IllegalAccessException, MessagingException, IOException;
		
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void validate(Timesheet timesheet) throws AxelorException;
		
	public Message sendValidationEmail(Timesheet timesheet) throws AxelorException, ClassNotFoundException, InstantiationException, IllegalAccessException, MessagingException, IOException;
		
	
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void refuse(Timesheet timesheet) throws AxelorException;
	
	public Message sendRefusalEmail(Timesheet timesheet) throws AxelorException, ClassNotFoundException, InstantiationException, IllegalAccessException, MessagingException, IOException;

	@Transactional(rollbackOn={Exception.class})
	public void cancel(Timesheet timesheet) throws AxelorException;
	
	public Message sendCancellationEmail(Timesheet timesheet) throws AxelorException, ClassNotFoundException, InstantiationException, IllegalAccessException, MessagingException, IOException;

	public Timesheet generateLines(Timesheet timesheet, LocalDate fromGenerationDate, LocalDate toGenerationDate, BigDecimal logTime, Project project, Product product) throws AxelorException;
	public LocalDate getFromPeriodDate();
	public Timesheet getCurrentTimesheet();
	public Timesheet getCurrentOrCreateTimesheet();
	public Timesheet createTimesheet(User user, LocalDate fromDate, LocalDate toDate);
	public TimesheetLine createTimesheetLine(Project project, Product product, User user, LocalDate date, Timesheet timesheet, BigDecimal hours, String comments);
	public List<InvoiceLine> createInvoiceLines(Invoice invoice, List<TimesheetLine> timesheetLineList, int priority) throws AxelorException;
	public List<InvoiceLine> createInvoiceLine(Invoice invoice, Product product, User user, String date, BigDecimal visibleDuration, int priority) throws AxelorException;
	@Transactional
	public void computeTimeSpent(Timesheet timesheet);
	public BigDecimal computeSubTimeSpent(Project project);
	public void computeParentTimeSpent(Project project);
	public BigDecimal computeTimeSpent(Project project);
	public void getActivities(ActionRequest request, ActionResponse response);
	@Transactional
	public void insertTSLine(ActionRequest request, ActionResponse response);
	public String computeFullName(Timesheet timesheet);
	public List<TimesheetLine> computeVisibleDuration(Timesheet timesheet) throws AxelorException;

	public List<Map<String, Object>> createDefaultLines(Timesheet timesheet);
	public BigDecimal computePeriodTotal(Timesheet timesheet);
	public String getPeriodTotalConvertTitleByUserPref(User user);

	public void createValidateDomainTimesheetLine(User user, Employee employee, ActionView.ActionViewBuilder actionView);
}
