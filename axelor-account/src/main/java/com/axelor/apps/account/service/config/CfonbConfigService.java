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
package com.axelor.apps.account.service.config;

import com.axelor.apps.account.db.AccountConfig;
import com.axelor.apps.account.db.CfonbConfig;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.account.service.app.AppAccountServiceImpl;
import com.axelor.apps.base.db.Company;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.i18n.I18n;

public class CfonbConfigService extends AccountConfigService  {

	public CfonbConfig getCfonbConfig(AccountConfig accountConfig) throws AxelorException {
		CfonbConfig cfonbConfig = accountConfig.getCfonbConfig();
		if (cfonbConfig == null) {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_1), AppAccountServiceImpl.EXCEPTION, accountConfig.getCompany().getName());
		}
		return cfonbConfig;
	}


	public CfonbConfig getCfonbConfig(Company company) throws AxelorException  {

		AccountConfig accountConfig = super.getAccountConfig(company);

		return this.getCfonbConfig(accountConfig);

	}



	/******************************** EXPORT CFONB ********************************************/


	public String getSenderRecordCodeExportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		String senderRecordCodeExportCFONB = cfonbConfig.getSenderRecordCodeExportCFONB();
		if(senderRecordCodeExportCFONB == null || senderRecordCodeExportCFONB.isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_2), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
		return senderRecordCodeExportCFONB;
	}

	public void getSenderNumExportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getSenderNumExportCFONB() == null || cfonbConfig.getSenderNumExportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_3), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

	public void getSenderNameCodeExportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getSenderNameCodeExportCFONB() == null || cfonbConfig.getSenderNameCodeExportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_4), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

	public void getRecipientRecordCodeExportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getRecipientRecordCodeExportCFONB() == null || cfonbConfig.getRecipientRecordCodeExportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_5), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

	public void getTotalRecordCodeExportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getTotalRecordCodeExportCFONB() == null || cfonbConfig.getTotalRecordCodeExportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_6), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

	public void getTransferOperationCodeExportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getTransferOperationCodeExportCFONB() == null || cfonbConfig.getTransferOperationCodeExportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_7), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

	public void getDirectDebitOperationCodeExportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getDirectDebitOperationCodeExportCFONB() == null || cfonbConfig.getDirectDebitOperationCodeExportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_8), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

	/******************************** IMPORT CFONB ********************************************/

	public void getHeaderRecordCodeImportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getHeaderRecordCodeImportCFONB() == null || cfonbConfig.getHeaderRecordCodeImportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_9), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

	public void getDetailRecordCodeImportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getDetailRecordCodeImportCFONB() == null || cfonbConfig.getDetailRecordCodeImportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_10), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

	public void getEndingRecordCodeImportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getEndingRecordCodeImportCFONB() == null || cfonbConfig.getEndingRecordCodeImportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_11), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

	public void getTransferOperationCodeImportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getTransferOperationCodeImportCFONB() == null || cfonbConfig.getTransferOperationCodeImportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_12), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

	public void getDirectDebitOperationCodeImportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getDirectDebitOperationCodeImportCFONB() == null || cfonbConfig.getDirectDebitOperationCodeImportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_13), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

	public void getIpoRejectOperationCodeImportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getIpoRejectOperationCodeImportCFONB() == null || cfonbConfig.getIpoRejectOperationCodeImportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_14), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

	public void getIpoAndChequeOperationCodeImportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getIpoAndChequeOperationCodeImportCFONB() == null || cfonbConfig.getIpoAndChequeOperationCodeImportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_15), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

	public void getIpoOperationCodeImportCFONB(CfonbConfig cfonbConfig) throws AxelorException  {
		if(cfonbConfig.getIpoOperationCodeImportCFONB() == null || cfonbConfig.getIpoOperationCodeImportCFONB().isEmpty())  {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.CFONB_CONFIG_16), AppAccountServiceImpl.EXCEPTION, cfonbConfig.getName());
		}
	}

}
