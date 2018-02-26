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
package com.axelor.apps.hr.service.timesheet.timer;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.hr.db.TSTimer;
import com.axelor.apps.hr.db.Timesheet;
import com.axelor.apps.hr.db.TimesheetLine;
import com.axelor.apps.hr.db.repo.TSTimerRepository;
import com.axelor.apps.hr.db.repo.TimesheetLineRepository;
import com.axelor.apps.hr.db.repo.TimesheetRepository;
import com.axelor.apps.hr.service.timesheet.TimesheetService;
import com.axelor.auth.AuthUtils;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class TimesheetTimerServiceImpl implements TimesheetTimerService {
	
	private final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );
	
	protected AppBaseService appBaseService;
	protected TimesheetService timesheetService;
	
	@Inject 
	public TimesheetTimerServiceImpl(AppBaseService appBaseService, TimesheetService timesheetService){
		
		this.appBaseService = appBaseService;
		this.timesheetService = timesheetService;
	}
	
	@Transactional(rollbackOn = {Exception.class})
	public void pause(TSTimer timer){
		timer.setStatusSelect(TSTimerRepository.STATUS_PAUSE);
		calculateDuration(timer);
	}
	
	@Transactional(rollbackOn = {Exception.class})
	public void stop(TSTimer timer) {
		timer.setStatusSelect(TSTimerRepository.STATUS_STOP);
		calculateDuration(timer);
		if(timer.getDuration() > 59)
			generateTimesheetLine(timer);
	}
	
	@Transactional(rollbackOn = {Exception.class})
	public void calculateDuration(TSTimer timer){
		long currentDuration = timer.getDuration();
		Duration duration = computeDuration(timer.getTimerStartDateT(), appBaseService.getTodayDateTime().toLocalDateTime());
		BigDecimal secondes = BigDecimal.valueOf((getDuration(duration) + currentDuration));
		timer.setDuration(secondes.longValue());
	}

	@Transactional(rollbackOn = {Exception.class})
	public TimesheetLine generateTimesheetLine(TSTimer timer) {
		
		BigDecimal durationHours = this.convertSecondDurationInHours(timer.getDuration());
		Timesheet timesheet = timesheetService.getCurrentOrCreateTimesheet();
		TimesheetLine timesheetLine = timesheetService.createTimesheetLine(timer.getProject(), timer.getProduct(), timer.getUser(), timer.getStartDateTime().toLocalDate(), timesheet, durationHours, timer.getComments());
		
		Beans.get(TimesheetRepository.class).save(timesheet);
		Beans.get(TimesheetLineRepository.class).save(timesheetLine);
		timer.setTimesheetLine(timesheetLine);
		
		return timesheetLine;
	}
	
	public BigDecimal convertSecondDurationInHours(long durationInSeconds)   {
		logger.debug("Duration in seconds : {}", durationInSeconds);
		
		BigDecimal durationHours = new BigDecimal(durationInSeconds).divide(new BigDecimal(3600), 4, RoundingMode.HALF_EVEN);
		logger.debug("Duration in hours : {}", durationHours);
		
		return durationHours;
	}
	
	public TSTimer getCurrentTSTimer(){
		return Beans.get(TSTimerRepository.class).all().filter("self.user = ?1",AuthUtils.getUser()).fetchOne();
	}
	
	private Duration computeDuration(LocalDateTime startDateTime, LocalDateTime endDateTime)  {

		return Duration.between(startDateTime, endDateTime);

	}

	private int getDuration(Duration duration)  {

		return new Integer(new Long(duration.getSeconds()).toString());

	}
}
