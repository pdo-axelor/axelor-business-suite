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
package com.axelor.apps.account.db.repo;

import javax.persistence.PersistenceException;

import com.axelor.apps.account.db.Reconcile;
import com.axelor.apps.account.service.ReconcileSequenceService;
import com.axelor.inject.Beans;
import com.google.inject.Inject;

public class ReconcileManagementRepository extends ReconcileRepository{
	
	@Inject
	protected ReconcileSequenceService reconcileSequenceService;

	@Override
	public Reconcile save(Reconcile reconcile) {
		try {

			Beans.get(ReconcileSequenceService.class).setDraftSequence(reconcile);

			return super.save(reconcile);
		} catch (Exception e) {
			throw new PersistenceException(e.getLocalizedMessage());
		}
	}
	
	@Override
	public Reconcile copy(Reconcile reconcile, boolean deep) {
		
		Reconcile copy = super.copy(reconcile, deep);
				
		copy.setCanBeZeroBalanceOk(false);
		copy.setMustBeZeroBalanceOk(false);
		copy.setReconcileSeq(null);
		copy.setStatusSelect(ReconcileRepository.STATUS_DRAFT);
		
		return copy;
	}
}
