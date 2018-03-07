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
import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.base.db.AppSupplychain;
import com.axelor.apps.base.db.CancelReason;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.repo.PriceListRepository;
import com.axelor.apps.base.service.PartnerPriceListService;
import com.axelor.apps.base.service.PartnerService;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.sale.service.saleorder.SaleOrderServiceImpl;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.apps.stock.service.StockMoveService;
import com.axelor.apps.supplychain.db.Timetable;
import com.axelor.apps.supplychain.exception.IExceptionMessage;
import com.axelor.apps.supplychain.service.app.AppSupplychainService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class SaleOrderServiceSupplychainImpl extends SaleOrderServiceImpl {
	
	private final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );
	
	protected AppSupplychain appSupplychain;

	@Inject
	public SaleOrderServiceSupplychainImpl(AppSupplychainService appSupplychainService) {
		
		this.appSupplychain = appSupplychainService.getAppSupplychain();

	}
	
	public SaleOrder getClientInformations(SaleOrder saleOrder){
		Partner client = saleOrder.getClientPartner();
		PartnerService partnerService = Beans.get(PartnerService.class);
		if(client != null){
			saleOrder.setPaymentCondition(client.getPaymentCondition());
			saleOrder.setPaymentMode(client.getInPaymentMode());
			saleOrder.setMainInvoicingAddress(partnerService.getInvoicingAddress(client));
			this.computeAddressStr(saleOrder);
			saleOrder.setDeliveryAddress(partnerService.getDeliveryAddress(client));
			saleOrder.setPriceList(Beans.get(PartnerPriceListService.class).getDefaultPriceList(client, PriceListRepository.TYPE_SALE));
		}
		return saleOrder;
	}


	public void updateAmountToBeSpreadOverTheTimetable(SaleOrder saleOrder) {
		List<Timetable> timetableList = saleOrder.getTimetableList();
		BigDecimal totalHT = saleOrder.getExTaxTotal();
		BigDecimal sumTimetableAmount = BigDecimal.ZERO;
		if (timetableList != null) {
			for (Timetable timetable : timetableList) {
				sumTimetableAmount = sumTimetableAmount.add(timetable.getAmount().multiply(timetable.getQty()));
			}
		}
		saleOrder.setAmountToBeSpreadOverTheTimetable(totalHT.subtract(sumTimetableAmount));
	}


	@Override
	@Transactional(rollbackOn = {Exception.class, AxelorException.class})
	public void enableEditOrder(SaleOrder saleOrder) throws AxelorException {
		super.enableEditOrder(saleOrder);

		List<StockMove> stockMoves = Beans.get(StockMoveRepository.class).findAllBySaleOrderAndStatus(saleOrder, StockMoveRepository.STATUS_PLANNED).fetch();
		if (!stockMoves.isEmpty()) {
			StockMoveService stockMoveService = Beans.get(StockMoveService.class);
			CancelReason cancelReason = appSupplychain.getCancelReasonOnChangingSaleOrder();
			if (cancelReason == null) {
				throw new AxelorException(appSupplychain, IException.CONFIGURATION_ERROR, IExceptionMessage.SUPPLYCHAIN_MISSING_CANCEL_REASON_ON_CHANGING_SALE_ORDER);
			}
			for (StockMove stockMove : stockMoves) {
			    stockMoveService.cancel(stockMove, cancelReason);
			}
		}
	}

    @Override
    public void validateChanges(SaleOrder saleOrder, SaleOrder saleOrderView) throws AxelorException {
        super.validateChanges(saleOrder, saleOrderView);
        if (saleOrder.getSaleOrderLineList() == null) {
            return;
        }

        for (SaleOrderLine saleOrderLine : saleOrder.getSaleOrderLineList()) {
            if (saleOrderLine.getDeliveryState() > SaleOrderRepository.STATE_NOT_DELIVERED
                    && saleOrderView.getSaleOrderLineList() == null
                    || !saleOrderView.getSaleOrderLineList().contains(saleOrderLine)) {
                throw new AxelorException(saleOrderView, IException.INCONSISTENCY,
                        I18n.get(IExceptionMessage.SO_CANT_REMOVED_DELIVERED_LINE), saleOrderLine.getFullName());

            }
        }
    }

}