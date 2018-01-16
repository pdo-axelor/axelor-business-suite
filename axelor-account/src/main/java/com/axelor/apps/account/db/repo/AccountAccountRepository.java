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

import com.axelor.apps.account.db.Account;
import com.axelor.db.JPA;
import com.axelor.inject.Beans;
import com.google.inject.persist.Transactional;

import javax.persistence.PersistenceException;
import java.util.Set;

public class AccountAccountRepository extends AccountRepository {

    @Override
    public Account save(Account account) {
        try {
            if (account.getId() == null) {
                return super.save(account);
            }

            if (account.getReconcileOk()) {
                Set<Account> accountList = account.getCompatibleAccountSet();

                for (Account acc : accountList) {
                    acc.setReconcileOk(true);
                    acc.addCompatibleAccountSetItem(account);
                    JPA.save(acc);
                }
            }

            return super.save(account);
        } catch (Exception e) {
            throw new PersistenceException(e.getLocalizedMessage());
        }
    }
}
