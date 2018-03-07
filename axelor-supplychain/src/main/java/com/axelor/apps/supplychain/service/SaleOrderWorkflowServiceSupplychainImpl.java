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
package com.axelor.apps.supplychain.service;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.base.db.AppSupplychain;
import com.axelor.apps.base.db.CancelReason;
import com.axelor.apps.base.db.repo.PartnerRepository;
import com.axelor.apps.base.service.administration.SequenceService;
import com.axelor.apps.base.service.user.UserService;
import com.axelor.apps.sale.db.ISaleOrder;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.sale.exception.BlockedSaleOrderException;
import com.axelor.apps.sale.service.app.AppSaleService;
import com.axelor.apps.sale.service.saleorder.SaleOrderWorkflowServiceImpl;
import com.axelor.apps.supplychain.service.app.AppSupplychainService;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class SaleOrderWorkflowServiceSupplychainImpl extends SaleOrderWorkflowServiceImpl {
	
	private final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );
	
	protected SaleOrderStockService saleOrderStockService;
	protected SaleOrderPurchaseService saleOrderPurchaseService;
	protected AppSupplychain appSupplychain;
	protected AccountingSituationSupplychainService accountingSituationSupplychainService;

	@Inject
	public SaleOrderWorkflowServiceSupplychainImpl(SequenceService sequenceService, PartnerRepository partnerRepo, 
			SaleOrderRepository saleOrderRepo, AppSaleService appSaleService, UserService userService,
			SaleOrderStockService saleOrderStockService, SaleOrderPurchaseService saleOrderPurchaseService, 
			AppSupplychainService appSupplychainService, AccountingSituationSupplychainService accountingSituationSupplychainService) {
		
		super(sequenceService, partnerRepo, saleOrderRepo, appSaleService, userService);
		
		this.saleOrderStockService = saleOrderStockService;
		this.saleOrderPurchaseService = saleOrderPurchaseService;
		this.appSupplychain = appSupplychainService.getAppSupplychain();
		this.accountingSituationSupplychainService = accountingSituationSupplychainService;

	}
	
	
	@Override
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void confirmSaleOrder(SaleOrder saleOrder) throws Exception  {

		super.confirmSaleOrder(saleOrder);
		
		if(appSupplychain.getPurchaseOrderGenerationAuto())  {
			saleOrderPurchaseService.createPurchaseOrders(saleOrder);
		}
		if(appSupplychain.getCustomerStockMoveGenerationAuto())  {
			saleOrderStockService.createStocksMovesFromSaleOrder(saleOrder);
		}
		int intercoSaleCreatingStatus = Beans.get(AppSupplychainService.class)
				.getAppSupplychain()
				.getIntercoSaleCreatingStatusSelect();
		if (saleOrder.getInterco()
				&& intercoSaleCreatingStatus == ISaleOrder.STATUS_ORDER_CONFIRMED) {
		    Beans.get(IntercoService.class)
					.generateIntercoPurchaseFromSale(saleOrder);
		}
	}

	@Override
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void cancelSaleOrder(SaleOrder saleOrder, CancelReason cancelReason, String cancelReasonStr){
		super.cancelSaleOrder(saleOrder, cancelReason, cancelReasonStr);
		try {
			accountingSituationSupplychainService.updateUsedCredit(saleOrder.getClientPartner());
		} catch (AxelorException e) {
			e.printStackTrace();
		}
	}
	

	@Override
    @Transactional(rollbackOn = { AxelorException.class, Exception.class }, ignore = {BlockedSaleOrderException.class })
	public void finalizeSaleOrder(SaleOrder saleOrder) throws Exception {
		accountingSituationSupplychainService.updateCustomerCreditFromSaleOrder(saleOrder);
		super.finalizeSaleOrder(saleOrder);
		int intercoSaleCreatingStatus = Beans.get(AppSupplychainService.class)
				.getAppSupplychain()
				.getIntercoSaleCreatingStatusSelect();
		if (saleOrder.getInterco()
				&& intercoSaleCreatingStatus == ISaleOrder.STATUS_FINALIZE) {
		    Beans.get(IntercoService.class)
					.generateIntercoPurchaseFromSale(saleOrder);
		}
	}


}