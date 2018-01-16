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
package com.axelor.apps.supplychain.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.stock.db.LogisticalForm;
import com.axelor.apps.supplychain.report.IReport;
import com.axelor.auth.AuthUtils;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class LogisticalFormController {

	public void print(ActionRequest request, ActionResponse response) {
		try {
			LogisticalForm logisticalForm = request.getContext().asType(LogisticalForm.class);

			String name = String.format("%s %s", I18n.get("Packaging list"), logisticalForm.getDeliveryNumber());

			String fileLink = ReportFactory.createReport(IReport.PACKAGING_LIST, name + " - ${date}")
					.addParam("LogisticalFormId", logisticalForm.getId())
					.addParam("Locale", AuthUtils.getUser().getLanguage()).generate().getFileLink();

			response.setView(ActionView.define(name).add("html", fileLink).map());
		} catch (Exception e) {
			TraceBackService.trace(response, e);
		}
	}

}
