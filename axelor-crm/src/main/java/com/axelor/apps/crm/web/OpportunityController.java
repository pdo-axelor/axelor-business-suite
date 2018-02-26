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
package com.axelor.apps.crm.web;

import java.io.IOException;
import java.util.List;

import com.axelor.apps.base.service.MapService;
import com.axelor.apps.crm.db.Opportunity;
import com.axelor.apps.crm.db.repo.OpportunityRepository;
import com.axelor.apps.crm.service.OpportunityService;
import com.axelor.auth.AuthUtils;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class OpportunityController {
	
	@Inject
	private OpportunityRepository opportunityRepo;
	
	@Inject
	private OpportunityService opportunityService;
	
	@SuppressWarnings("rawtypes")
	public void assignToMe(ActionRequest request, ActionResponse response)  {
		
		if(request.getContext().get("id") != null){
			Opportunity opportunity = opportunityRepo.find((Long)request.getContext().get("id"));
			opportunity.setUser(AuthUtils.getUser());
			opportunityService.saveOpportunity(opportunity);
		}
		else if(!((List)request.getContext().get("_ids")).isEmpty()){
			for(Opportunity opportunity : opportunityRepo.all().filter("id in ?1",request.getContext().get("_ids")).fetch()){
				opportunity.setUser(AuthUtils.getUser());
				opportunityService.saveOpportunity(opportunity);
			}
		}
		response.setReload(true);
	}
	
	public void showOpportunitiesOnMap(ActionRequest request, ActionResponse response) throws IOException {
		
		Beans.get(MapService.class).showMap("opportunity", I18n.get("Opportunities"), response);
		
	}	
	
	public void createClient(ActionRequest request, ActionResponse response) throws AxelorException{
		Opportunity opportunity = request.getContext().asType(Opportunity.class);
		opportunity = opportunityRepo.find(opportunity.getId());
		opportunityService.createClientFromLead(opportunity);
		response.setReload(true);
	}
}
