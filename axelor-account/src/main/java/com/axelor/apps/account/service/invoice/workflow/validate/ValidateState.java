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
package com.axelor.apps.account.service.invoice.workflow.validate;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.db.repo.PaymentModeRepository;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.account.service.invoice.InvoiceToolService;
import com.axelor.apps.account.service.invoice.workflow.WorkflowInvoice;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;

public class ValidateState extends WorkflowInvoice {

	protected User user;

	public ValidateState() {
		super();
		this.user = AuthUtils.getUser();

	}

	public void init(Invoice invoice){
		this.invoice = invoice;
	}

	@Override
	public void process( ) throws AxelorException {

		if ((InvoiceToolService.isOutPayment(invoice) && (invoice.getPaymentMode().getInOutSelect() == PaymentModeRepository.IN))
		 || (!InvoiceToolService.isOutPayment(invoice) && (invoice.getPaymentMode().getInOutSelect() == PaymentModeRepository.OUT))) {
			throw new AxelorException(IException.INCONSISTENCY, I18n.get(IExceptionMessage.INVOICE_VALIDATE_1));
		}

		invoice.setStatusSelect(InvoiceRepository.STATUS_VALIDATED);
		invoice.setValidatedByUser( user );

		Beans.get(WorkflowValidationService.class).afterValidation(invoice);

	}

}