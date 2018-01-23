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
package com.axelor.apps.stock.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.stock.db.StockLocation;

public interface StockLocationService {

    /**
     * Get the default stock location of the given company
     * @param company
     * @return the default stock location or null
     */
    public StockLocation getDefaultStockLocation(Company company);

	public BigDecimal getQty(Long productId, Long locationId, String qtyType);
	
	public BigDecimal getRealQty(Long productId, Long locationId);
	
	public BigDecimal getFutureQty(Long productId, Long locationId);

	public void computeAvgPriceForProduct(Product product);

	public List<Long> getBadStockLocationLineId();
	
	public Set<Long> getContentStockLocationIds(StockLocation stockLocation);
	
}
