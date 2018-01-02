/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2017 Axelor (<http://axelor.com>).
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
package com.axelor.apps.account.service.debtrecovery;

import com.axelor.apps.account.db.DebtRecovery;
import com.axelor.apps.account.db.DebtRecoveryHistory;
import com.axelor.apps.account.db.DebtRecoveryMethodLine;
import com.axelor.apps.account.db.repo.DebtRecoveryHistoryRepository;
import com.axelor.apps.account.db.repo.DebtRecoveryRepository;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.app.AppAccountServiceImpl;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.service.user.UserService;
import com.axelor.apps.message.db.Message;
import com.axelor.apps.message.db.Template;
import com.axelor.apps.message.db.repo.MessageRepository;
import com.axelor.apps.message.service.MessageService;
import com.axelor.apps.message.service.TemplateMessageService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

public class DebtRecoveryActionService {

	private final Logger log = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	protected UserService userService;
	protected DebtRecoveryRepository debtRecoveryRepo;
	protected DebtRecoveryHistoryRepository debtRecoveryHistoryRepository;
	protected TemplateMessageService templateMessageService;

	protected LocalDate today;

	@Inject
	public DebtRecoveryActionService(UserService userService, DebtRecoveryRepository debtRecoveryRepo, DebtRecoveryHistoryRepository debtRecoveryHistoryRepository, 
			TemplateMessageService templateMessageService, AppAccountService appAccountService) {

		this.userService = userService;
		this.debtRecoveryRepo = debtRecoveryRepo;
		this.debtRecoveryHistoryRepository = debtRecoveryHistoryRepository;
		this.templateMessageService = templateMessageService;
		
		this.today = appAccountService.getTodayDate();

	}



	/**
	 * Procédure permettant de lancer l'ensemble des actions relative au niveau de relance d'un tiers
	 * @param debtRecovery
	 * 			Une relance
	 * @throws AxelorException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void runAction(DebtRecovery debtRecovery) throws AxelorException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException  {

		if (debtRecovery.getDebtRecoveryMethod() == null) {
			throw new AxelorException(debtRecovery, IException.MISSING_FIELD, "%s :\n"+I18n.get("Partner")+" %s: "+I18n.get(IExceptionMessage.DEBT_RECOVERY_ACTION_1), AppAccountServiceImpl.EXCEPTION, debtRecovery.getAccountingSituation().getPartner().getName());
		}
		if (debtRecovery.getDebtRecoveryMethodLine() == null) {
			throw new AxelorException(debtRecovery, IException.MISSING_FIELD, "%s :\n"+I18n.get("Partner")+" %s: "+I18n.get(IExceptionMessage.DEBT_RECOVERY_ACTION_2), AppAccountServiceImpl.EXCEPTION, debtRecovery.getAccountingSituation().getPartner().getName());
		}

		else  {

			//On enregistre la date de la relance
			debtRecovery.setDebtRecoveryDate(today);

			this.saveDebtRecovery(debtRecovery);

		}

	}



	/**
	 * Fonction permettant de créer un courrier à destination des tiers pour un contrat standard
	 * 
	 * @param debtRecovery
	 * @return
	 * @throws AxelorException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	public Set<Message> runStandardMessage(DebtRecovery debtRecovery) throws AxelorException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException  {

		DebtRecoveryMethodLine debtRecoveryMethodLine = debtRecovery.getDebtRecoveryMethodLine();
		Partner partner =  debtRecovery.getAccountingSituation().getPartner();

		Set<Template> templateSet = debtRecoveryMethodLine.getMessageTemplateSet();

		if (templateSet == null || templateSet.isEmpty()) {
			throw new AxelorException(IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.DEBT_RECOVERY_ACTION_3), AppAccountServiceImpl.EXCEPTION, partner.getName(), debtRecoveryMethodLine.getDebtRecoveryMethod().getName(), debtRecoveryMethodLine.getDebtRecoveryLevel().getName());
		}

		DebtRecoveryHistory debtRecoveryHistory = this.getDebtRecoveryHistory(debtRecovery);

		for (Template template : templateSet) {
			debtRecoveryHistory.addDebtRecoveryMessageSetItem(templateMessageService.generateMessage(debtRecoveryHistory, template));
		}

		return debtRecoveryHistory.getDebtRecoveryMessageSet();

	}


	public DebtRecoveryHistory getDebtRecoveryHistory(DebtRecovery detDebtRecovery)  {
		if (detDebtRecovery.getDebtRecoveryHistoryList() == null) {
			return null;
		}
	    return Collections.max(detDebtRecovery.getDebtRecoveryHistoryList(),
				Comparator.comparing(DebtRecoveryHistory::getDebtRecoveryDate));
	}



	/**
	 * Procédure permettant de lancer manuellement l'ensemble des actions relative au niveau de relance d'un tiers
	 * @param debtRecovery
	 * 			Une relance
	 * @throws AxelorException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void runManualAction(DebtRecovery debtRecovery) throws AxelorException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException  {

		log.debug("Begin runManualAction service ...");
		if (debtRecovery.getDebtRecoveryMethod() == null) {
			throw new AxelorException(IException.MISSING_FIELD, "%s :\n"+I18n.get("Partner")+" %s: "+I18n.get(IExceptionMessage.DEBT_RECOVERY_ACTION_1), AppAccountServiceImpl.EXCEPTION, debtRecovery.getAccountingSituation().getPartner().getName());
		}

		if (debtRecovery.getWaitDebtRecoveryMethodLine() == null) {
			throw new AxelorException(IException.MISSING_FIELD, "%s :\n"+I18n.get("Partner")+" %s: "+I18n.get(IExceptionMessage.DEBT_RECOVERY_ACTION_2), AppAccountServiceImpl.EXCEPTION, debtRecovery.getAccountingSituation().getPartner().getName());
		}
		else  {

			//On enregistre la date de la relance
			debtRecovery.setDebtRecoveryDate(today);
			this.debtRecoveryLevelValidate(debtRecovery);

			this.saveDebtRecovery(debtRecovery);

		}
		log.debug("End runManualAction service");
	}

	/**
	 * Generate a message from a debtRecovery, save, and send it.
	 * @param debtRecovery
	 * @throws AxelorException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void runMessage(DebtRecovery debtRecovery) throws AxelorException, ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
		Set<Message> messageSet = this.runStandardMessage(debtRecovery);
		for (Message message : messageSet) {
			Beans.get(MessageRepository.class).save(message);
			Beans.get(MessageService.class).sendMessage(message);
		}
		this.updateDebtRecoveryHistory(debtRecovery, messageSet);
	}

	/**
	 * Procédure permettant de déplacer une ligne de relance vers une ligne de relance en attente
	 * @param debtRecovery
	 * 			Une relance
	 * @param debtRecoveryMethodLine
	 * 			La ligne de relance que l'on souhaite déplacer
	 * @throws AxelorException
	 */
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void moveDebtRecoveryMethodLine(DebtRecovery debtRecovery, DebtRecoveryMethodLine debtRecoveryMethodLine) throws AxelorException  {

		debtRecovery.setWaitDebtRecoveryMethodLine(debtRecoveryMethodLine);

		debtRecoveryRepo.save(debtRecovery);

	}



	/**
	 * Fonction permettant de valider la ligne de relance en attente en la déplaçant vers la ligne de relance courante d'un tiers
	 * @param debtRecovery
	 * 			Une relance
	 * @return
	 * 			La relance
	 * @throws AxelorException
	 */
	public DebtRecovery debtRecoveryLevelValidate(DebtRecovery debtRecovery) throws AxelorException  {
		log.debug("Begin debtRecoveryLevelValidate service ...");

		debtRecovery.setDebtRecoveryMethodLine(debtRecovery.getWaitDebtRecoveryMethodLine());
		debtRecovery.setWaitDebtRecoveryMethodLine(null);

		log.debug("End debtRecoveryLevelValidate service");
		return debtRecovery;
	}

	/**
	 * Procédure permettant d'enregistrer les éléments de la relance dans l'historique des relances
	 * @param debtRecovery
	 * 			Une relance
	 */
	@Transactional
	public void saveDebtRecovery(DebtRecovery debtRecovery)  {

		DebtRecoveryHistory debtRecoveryHistory = new DebtRecoveryHistory();
		debtRecoveryHistory.setDebtRecovery(debtRecovery);
		debtRecoveryHistory.setBalanceDue(debtRecovery.getBalanceDue());
		debtRecoveryHistory.setBalanceDueDebtRecovery(debtRecovery.getBalanceDueDebtRecovery());
		debtRecoveryHistory.setDebtRecoveryDate(debtRecovery.getDebtRecoveryDate());
		debtRecoveryHistory.setDebtRecoveryMethodLine(debtRecovery.getDebtRecoveryMethodLine());
		debtRecoveryHistory.setSetToIrrecoverableOK(debtRecovery.getSetToIrrecoverableOk());
		debtRecoveryHistory.setUnknownAddressOK(debtRecovery.getUnknownAddressOk());
		debtRecoveryHistory.setReferenceDate(debtRecovery.getReferenceDate());
		debtRecoveryHistory.setDebtRecoveryMethod(debtRecovery.getDebtRecoveryMethod());

		debtRecoveryHistory.setUserDebtRecovery(userService.getUser());
		debtRecovery.addDebtRecoveryHistoryListItem(debtRecoveryHistory);
		debtRecoveryHistoryRepository.save(debtRecoveryHistory);

	}

	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void updateDebtRecoveryHistory(DebtRecovery debtRecovery, Set<Message> debtRecoveryMessageSet)  {

		if(!debtRecovery.getDebtRecoveryHistoryList().isEmpty())  {
			DebtRecoveryHistory debtRecoveryHistory = getDebtRecoveryHistory(debtRecovery);
			debtRecoveryMessageSet.forEach(debtRecoveryHistory::addDebtRecoveryMessageSetItem);
		}

	}


}