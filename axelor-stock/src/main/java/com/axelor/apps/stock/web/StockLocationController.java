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
package com.axelor.apps.stock.web;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.eclipse.birt.core.exception.BirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.apps.stock.db.repo.StockLocationRepository;
import com.axelor.apps.stock.exception.IExceptionMessage;
import com.axelor.apps.stock.report.IReport;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.base.Strings;
import com.google.inject.Inject;

public class StockLocationController {

	private final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );
	
	private StockLocationRepository stockLocationRepo;

	@Inject
	public StockLocationController(StockLocationRepository stockLocationRepo) {
		this.stockLocationRepo = stockLocationRepo;
	}

	/**
	 * Method that generate inventory as a pdf
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws BirtException 
	 * @throws IOException 
	 */
	public void print(ActionRequest request, ActionResponse response) throws AxelorException {


		StockLocation stockLocation = request.getContext().asType(StockLocation.class );
		String locationIds = "";

		@SuppressWarnings("unchecked")
		List<Integer> lstSelectedLocations = (List<Integer>) request.getContext().get("_ids");
		if(lstSelectedLocations != null){
			for(Integer it : lstSelectedLocations) {
				locationIds+= it.toString()+",";
			}
		}	
			
		if(!locationIds.equals("")){
			locationIds = locationIds.substring(0, locationIds.length()-1);	
			stockLocation = stockLocationRepo.find(new Long(lstSelectedLocations.get(0)));
		}else if(stockLocation.getId() != null){
			locationIds = stockLocation.getId().toString();			
		}
		
		if(!locationIds.equals("")){
			
			User user = AuthUtils.getUser();
			Company company = stockLocation.getCompany();
			
			String language = "en";
			if(user != null && !Strings.isNullOrEmpty(user.getLanguage()))  {
				language = user.getLanguage();
			}
			else if(company != null && company.getPrintingSettings() != null && !Strings.isNullOrEmpty(company.getPrintingSettings().getLanguageSelect())) {
				language = company.getPrintingSettings().getLanguageSelect();
			}

			String title = I18n.get("Stock location");
			if(stockLocation.getName() != null)  {
				title = lstSelectedLocations == null ? I18n.get("Stock location") + " " + stockLocation.getName() : I18n.get("Stock location(s)");
			}
		
			String fileLink = ReportFactory.createReport(IReport.STOCK_LOCATION, title+"-${date}")
					.addParam("StockLocationId", locationIds)
					.addParam("Locale", language)
					.generate()
					.getFileLink();

			logger.debug("Printing "+title);
		
			response.setView(ActionView
					.define(title)
					.add("html", fileLink).map());	
				
		}else{
			response.setFlash(I18n.get(IExceptionMessage.LOCATION_2));
		}	
	}
	
}