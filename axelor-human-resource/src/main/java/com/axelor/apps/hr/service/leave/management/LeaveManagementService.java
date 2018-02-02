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
package com.axelor.apps.hr.service.leave.management;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import java.time.LocalDate;

import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.hr.db.LeaveLine;
import com.axelor.apps.hr.db.LeaveManagement;
import com.axelor.auth.db.User;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class LeaveManagementService {
	
	@Inject
	private AppBaseService appBaseService;
	
	public LeaveLine computeQuantityAvailable (LeaveLine leaveLine){
		List<LeaveManagement> leaveManagementList = leaveLine.getLeaveManagementList();
		leaveLine.setTotalQuantity(BigDecimal.ZERO);
		if(leaveManagementList != null && !leaveManagementList.isEmpty()){
			for (LeaveManagement leaveManagement : leaveManagementList) {
				leaveLine.setTotalQuantity(leaveLine.getTotalQuantity().add(leaveManagement.getValue()));
			}
			leaveLine.setQuantity( leaveLine.getTotalQuantity().subtract( leaveLine.getDaysValidated()  ) );
		}
		return leaveLine;
	}
	
	
	@Transactional
	public LeaveManagement createLeaveManagement(LeaveLine leaveLine, User user, String comments, LocalDate date, LocalDate fromDate, LocalDate toDate,  BigDecimal value){
		
		LeaveManagement leaveManagement = new LeaveManagement();
		
		leaveManagement.setLeaveLine(leaveLine);
		leaveManagement.setUser(user);
		leaveManagement.setComments(comments);
		if (date == null){
			leaveManagement.setDate(appBaseService.getTodayDate());
		}else{
			leaveManagement.setDate(date);
		}
		leaveManagement.setFromDate(fromDate);
		leaveManagement.setToDate(toDate);
        leaveManagement.setValue(value);
		
		return leaveManagement;
	}

	/**
	 * Reset leave management list.
	 * 
	 * @param leaveLine
	 * @throws AxelorException
	 */
	@Transactional(rollbackOn = { AxelorException.class, Exception.class })
	public void reset(LeaveLine leaveLine) throws AxelorException {
		leaveLine.clearLeaveManagementList();
		leaveLine.setQuantity(BigDecimal.ZERO);
		leaveLine.setTotalQuantity(BigDecimal.ZERO);
	}

}
