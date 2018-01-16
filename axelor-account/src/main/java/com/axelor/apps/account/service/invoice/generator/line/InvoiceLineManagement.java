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
package com.axelor.apps.account.service.invoice.generator.line;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.base.db.IAdministration;
import com.axelor.exception.AxelorException;

public abstract class InvoiceLineManagement {

	private static final Logger LOG = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );
	
	abstract public List<?> creates() throws AxelorException ;
	
	/**
	 * Compute the quantity per the unit price
	 *  
	 * @param quantity
	 * @param price
	 *          The unit price.
	 * 
	 * @return 
	 * 			The Excluded tax total amount.
	 */
	public static BigDecimal computeAmount(BigDecimal quantity, BigDecimal price) {

		BigDecimal amount = quantity.multiply(price).setScale(IAdministration.DEFAULT_NB_DECIMAL_DIGITS, RoundingMode.HALF_EVEN);

		LOG.debug("Calcul du montant HT avec une quantité de {} pour {} : {}", new Object[] { quantity, price, amount });

		return amount;
	}
	
	/**
	 * Compute the quantity per the unit price
	 *  
	 * @param quantity
	 * @param price
	 *          The unit price.
	 *  @param scale
	 *  		Scale to apply on the result
	 * 
	 * @return 
	 * 			The Excluded tax total amount.
	 */
	public static BigDecimal computeAmount(BigDecimal quantity, BigDecimal price, int scale) {

		BigDecimal amount = quantity.multiply(price).setScale(scale, RoundingMode.HALF_EVEN);

		LOG.debug("Calcul du montant HT avec une quantité de {} pour {} : {}", new Object[] { quantity, price, amount });

		return amount;
	}
}
