/**
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
package com.axelor.apps.hr.service.project;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.hr.db.TimesheetLine;
import com.axelor.apps.hr.service.employee.EmployeeService;
import com.axelor.apps.project.db.Project;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class ProjectServiceImpl implements ProjectService {
	
	protected AppBaseService appBaseService;
	protected EmployeeService employeeService;	
	
	@Inject
	public ProjectServiceImpl(AppBaseService appBaseService, EmployeeService employeeService){
		
		this.appBaseService = appBaseService;
		this.employeeService = employeeService;
	}
	
	@Transactional(rollbackOn={Exception.class})
	public List<TimesheetLine> computeVisibleDuration(Project project)  {
		
		List<TimesheetLine> timesheetLineList = project.getTimesheetLineList();
		
		for(TimesheetLine timesheetLine : timesheetLineList)  {

			timesheetLine.setVisibleDuration(employeeService.getUserDuration(timesheetLine.getDurationStored(), timesheetLine.getUser(), false));
		
		}

		timesheetLineList = _sortTimesheetLineByDate(timesheetLineList);

		return timesheetLineList;
	}

	public List<TimesheetLine> _sortTimesheetLineByDate(List<TimesheetLine> timesheetLineList){
	
		Collections.sort(timesheetLineList, new Comparator<TimesheetLine>() {
	
			@Override
			public int compare(TimesheetLine tsl1, TimesheetLine tsl2) {
				if(tsl1.getDate().isAfter(tsl2.getDate()))
					return 1;
				else if(tsl1.getDate().isBefore(tsl2.getDate()))
					return -1;
				else
					return 0;
			}
		});
	
		return timesheetLineList;
	}

}