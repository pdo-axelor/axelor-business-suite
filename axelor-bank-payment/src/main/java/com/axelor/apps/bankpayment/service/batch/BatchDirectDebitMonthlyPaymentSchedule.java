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
package com.axelor.apps.bankpayment.service.batch;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.account.db.AccountingBatch;
import com.axelor.apps.account.db.PaymentMode;
import com.axelor.apps.account.db.repo.PaymentScheduleRepository;
import com.axelor.apps.bankpayment.db.BankOrder;
import com.axelor.apps.bankpayment.service.PaymentScheduleLineBankPaymentService;
import com.axelor.apps.base.db.BankDetails;
import com.axelor.apps.base.db.Company;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.google.inject.persist.Transactional;

public class BatchDirectDebitMonthlyPaymentSchedule extends BatchDirectDebitPaymentSchedule {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    protected void process() {
        processPaymentScheduleLines(PaymentScheduleRepository.TYPE_MONTHLY);

        if (!batch.getPaymentScheduleLineDoneSet().isEmpty() && generateBankOrderFlag) {
            try {
                createBankOrder();
            } catch (Exception e) {
                TraceBackService.trace(e, IException.DIRECT_DEBIT, batch.getId());
                logger.error(e.getLocalizedMessage());
            }
        }
    }

    @Transactional(rollbackOn = { AxelorException.class, Exception.class })
    protected void createBankOrder()
            throws AxelorException, JAXBException, IOException, DatatypeConfigurationException {

        findBatch();
        AccountingBatch accountingBatch = batch.getAccountingBatch();
        LocalDate bankOrderDate = accountingBatch.getDueDate();
        Company senderCompany = accountingBatch.getCompany();
        BankDetails senderBankDetails = getCompanyBankDetails(accountingBatch);
        PaymentMode paymentMode = accountingBatch.getPaymentMode();

        BankOrder bankOrder = Beans.get(PaymentScheduleLineBankPaymentService.class).createBankOrder(getDoneList(),
                paymentMode, bankOrderDate, senderCompany, senderBankDetails);

        batch.setBankOrder(bankOrder);
    }

}
