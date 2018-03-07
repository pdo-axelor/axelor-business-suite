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
package com.axelor.apps.sale.service.saleorder;

import java.lang.invoke.MethodHandles;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.base.db.Blocking;
import com.axelor.apps.base.db.CancelReason;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.IAdministration;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.repo.BlockingRepository;
import com.axelor.apps.base.db.repo.PartnerRepository;
import com.axelor.apps.base.service.BlockingService;
import com.axelor.apps.base.service.administration.SequenceService;
import com.axelor.apps.base.service.user.UserService;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.apps.sale.db.ISaleOrder;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.sale.exception.BlockedSaleOrderException;
import com.axelor.apps.sale.exception.IExceptionMessage;
import com.axelor.apps.sale.report.IReport;
import com.axelor.apps.sale.service.app.AppSaleService;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class SaleOrderWorkflowServiceImpl implements SaleOrderWorkflowService {

	private final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	protected SequenceService sequenceService;
	protected PartnerRepository partnerRepo;
	protected SaleOrderRepository saleOrderRepo;
	protected AppSaleService appSaleService;
	protected User currentUser;
	

	@Inject
	public SaleOrderWorkflowServiceImpl(SequenceService sequenceService,
			PartnerRepository partnerRepo, SaleOrderRepository saleOrderRepo, AppSaleService appSaleService, UserService userService)  {
		
		this.sequenceService = sequenceService;
		this.partnerRepo = partnerRepo;
		this.saleOrderRepo = saleOrderRepo;
		this.appSaleService = appSaleService;
		this.currentUser = userService.getUser();
	}
	

	@Override
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public Partner validateCustomer(SaleOrder saleOrder)  {

		Partner clientPartner = partnerRepo.find(saleOrder.getClientPartner().getId());
		clientPartner.setIsCustomer(true);
		clientPartner.setIsProspect(false);

		return partnerRepo.save(clientPartner);
	}


	@Override
	public String getSequence(Company company) throws AxelorException  {

		String seq = sequenceService.getSequenceNumber(IAdministration.SALES_ORDER, company);
		if (seq == null) {
			throw new AxelorException(company, IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.SALES_ORDER_1),company.getName());
		}
		return seq;
	}



	@Override
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void cancelSaleOrder(SaleOrder saleOrder, CancelReason cancelReason, String cancelReasonStr){
		Query q = JPA.em().createQuery("select count(*) FROM SaleOrder as self WHERE self.statusSelect = ?1 AND self.clientPartner = ?2 ");
		q.setParameter(1, ISaleOrder.STATUS_ORDER_CONFIRMED);
		q.setParameter(2, saleOrder.getClientPartner());
		if((long) q.getSingleResult() == 1)  {
			saleOrder.getClientPartner().setIsCustomer(false);
			saleOrder.getClientPartner().setIsProspect(true);
		}
		saleOrder.setStatusSelect(ISaleOrder.STATUS_CANCELED);
		saleOrder.setCancelReason(cancelReason);
		if (Strings.isNullOrEmpty(cancelReasonStr)) {
			saleOrder.setCancelReasonStr(cancelReason.getName());
		} else {
			saleOrder.setCancelReasonStr(cancelReasonStr);
		}
		saleOrderRepo.save(saleOrder);
	}

    @Override
    @Transactional(rollbackOn = { AxelorException.class, Exception.class }, ignore = { BlockedSaleOrderException.class })
    public void finalizeSaleOrder(SaleOrder saleOrder) throws Exception {
	    Partner partner = saleOrder.getClientPartner();

	    Blocking blocking = Beans.get(BlockingService.class).getBlocking(partner, saleOrder.getCompany(), BlockingRepository.SALE_BLOCKING);

        if (blocking != null) {
            saleOrder.setBlockedOnCustCreditExceed(true);
            if (!saleOrder.getManualUnblock()) {
                saleOrderRepo.save(saleOrder);
				String reason = blocking.getBlockingReason() != null ? blocking.getBlockingReason().getName() : "";
                throw new BlockedSaleOrderException(partner, I18n.get("Client is sale blocked:") + " " + reason);
            }
        }

        saleOrder.setStatusSelect(ISaleOrder.STATUS_FINALIZE);
        saleOrderRepo.save(saleOrder);
        if (appSaleService.getAppSale().getManageSaleOrderVersion()) {
            this.saveSaleOrderPDFAsAttachment(saleOrder);
        }
        if (saleOrder.getVersionNumber() == 1) {
            saleOrder.setSaleOrderSeq(this.getSequence(saleOrder.getCompany()));
        }
    }

	@Override
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void confirmSaleOrder(SaleOrder saleOrder) throws Exception  {
		saleOrder.setStatusSelect(ISaleOrder.STATUS_ORDER_CONFIRMED);
		saleOrder.setConfirmationDate(appSaleService.getTodayDate());
		saleOrder.setConfirmedByUser(this.currentUser);
		
		this.validateCustomer(saleOrder);
		
		saleOrderRepo.save(saleOrder);
	}
	
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void finishSaleOrder(SaleOrder saleOrder) throws AxelorException {
		saleOrder.setStatusSelect(ISaleOrder.STATUS_FINISHED);

		saleOrderRepo.save(saleOrder);
	}
	

	@Override
	public void saveSaleOrderPDFAsAttachment(SaleOrder saleOrder) throws AxelorException  {
		ReportFactory.createReport(IReport.SALES_ORDER, this.getFileName(saleOrder)+"-${date}")
				.addParam("Locale", ReportSettings.getPrintingLocale(saleOrder.getClientPartner()))
				.addParam("SaleOrderId", saleOrder.getId())
				.toAttach(saleOrder)
				.generate()
				.getFileLink();
		
//		String relatedModel = generalService.getPersistentClass(saleOrder).getCanonicalName(); required ?
		
	}

	@Override
	public String getFileName(SaleOrder saleOrder)  {
		
		return I18n.get("Sale order") + " " + saleOrder.getSaleOrderSeq() + ((saleOrder.getVersionNumber() > 1) ? "-V" + saleOrder.getVersionNumber() : "");
	}
	



}
