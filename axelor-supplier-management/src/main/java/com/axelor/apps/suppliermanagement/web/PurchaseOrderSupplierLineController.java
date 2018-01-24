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
package com.axelor.apps.suppliermanagement.web;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.repo.BlockingRepository;
import com.axelor.apps.base.service.BlockingService;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.purchase.db.repo.PurchaseOrderLineRepository;
import com.axelor.apps.suppliermanagement.db.PurchaseOrderSupplierLine;
import com.axelor.apps.suppliermanagement.db.repo.PurchaseOrderSupplierLineRepository;
import com.axelor.apps.suppliermanagement.service.PurchaseOrderSupplierLineService;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.base.Strings;
import com.google.inject.Inject;

public class PurchaseOrderSupplierLineController {

	@Inject
	private PurchaseOrderSupplierLineRepository purchaseOrderSupplierLineRepo;
	
	@Inject
	private PurchaseOrderSupplierLineService purchaseOrderSupplierLineService;
	
	public void accept(ActionRequest request, ActionResponse response){
		
		PurchaseOrderSupplierLine purchaseOrderSupplierLine = purchaseOrderSupplierLineRepo.find( request.getContext().asType(PurchaseOrderSupplierLine.class).getId() );
		
		if (purchaseOrderSupplierLine.getPurchaseOrderLine() == null && request.getContext().getParentContext() != null){
			purchaseOrderSupplierLine.setPurchaseOrderLine( Beans.get(PurchaseOrderLineRepository.class).find(request.getContext().getParentContext().asType(PurchaseOrderLine.class).getId()) );
		}
		
		try {
			purchaseOrderSupplierLineService.accept(purchaseOrderSupplierLine);
			response.setReload(true);
		}
		catch (Exception e) { TraceBackService.trace(response, e); }
	}

	/**
	 * Called on supplier partner select.
	 * Set the domain for the field supplierPartner
	 * @param request
	 * @param response
	 */
	public void supplierPartnerDomain(ActionRequest request, ActionResponse response) {
		PurchaseOrderSupplierLine purchaseOrderSupplierLine = request.getContext().asType(PurchaseOrderSupplierLine.class);
		if (purchaseOrderSupplierLine.getPurchaseOrderLine() == null
				||purchaseOrderSupplierLine.getPurchaseOrderLine().getPurchaseOrder() == null ) {
			return;
		}
		Company company = purchaseOrderSupplierLine.getPurchaseOrderLine().getPurchaseOrder().getCompany();
		String domain = "self.id != " + company.getPartner().getId() + " AND self.isContact = false AND self.isSupplier = true";
		String blockedPartnerQuery = Beans.get(BlockingService.class)
				.listOfBlockedPartner(company, BlockingRepository.PURCHASE_BLOCKING);

		if (!Strings.isNullOrEmpty(blockedPartnerQuery)) {
			domain += String.format(" AND self.id NOT in (%s)", blockedPartnerQuery);
		}
		response.setAttr("supplierPartner", "domain", domain);
	}
}
