package com.axelor.apps.account.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.account.db.BankOrder;
import com.axelor.apps.account.db.EbicsUser;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.account.service.bankOrder.BankOrderService;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;

public class BankOrderController {
	
	private final Logger log = LoggerFactory.getLogger( getClass() );
	
	
	@Inject
	protected BankOrderService bankOrderService;
	
	
	public void validate(ActionRequest request, ActionResponse response ) throws AxelorException{
		
		BankOrder bankOrder = request.getContext().asType(BankOrder.class);
		
		log.debug("BANK ORDER LINES {}", bankOrder.getBankOrderLineList().size());
		log.debug("BANK ORDER TYPE {}", bankOrder.getOrderType());
		
		bankOrderService.validate(bankOrder);
		//TODO create sequence for bank order and bank order line
		//TODO validate fields
	}
	
	
	public void sign(ActionRequest request, ActionResponse response ) throws AxelorException{
		
		BankOrder bankOrder  = JPA.em().find(BankOrder.class, new Long((Integer)((Map)request.getContext().get("_contextBankOrder")).get("id")));
		log.debug("BANK ORDER {}", bankOrder);
		EbicsUser ebicsUser = new EbicsUser();
		String password = null;
		if (request.getContext().get("ebicsUser") != null){
			ebicsUser = JPA.em().find(EbicsUser.class, new Long((Integer)((Map)request.getContext().get("ebicsUser")).get("id")));
		}
		if (request.getContext().get("password") != null){
			password = (String)request.getContext().get("password");
		}
		if(!ebicsUser.getPassword().equals(password)){
			response.setValue("password", "");
			response.setError(I18n.get(IExceptionMessage.EBICS_WRONG_PASSWORD));
		}
		else{
			bankOrderService.sign(bankOrder);
		}
		
		
	}
}
