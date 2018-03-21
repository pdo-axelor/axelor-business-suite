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
package com.axelor.apps.production.web;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.production.db.ManufOrder;
import com.axelor.apps.production.exceptions.IExceptionMessage;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.service.StockMoveLineService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;

public class StockMoveLineController {

    /**
     * Called from stock move line form.
     * Fill product info using the company either from the stock move line,
     * from the parent stock move or the parent manuf order.
     *
     * @param request
     * @param response
     */
    public void setProductInfo(ActionRequest request, ActionResponse response) {
        try {
            StockMoveLine stockMoveLine = request.getContext().asType(StockMoveLine.class);
            Company company;
            StockMove stockMove = stockMoveLine.getStockMove();
            if (stockMove == null) {
                Context parentContext = request.getContext().getParent();
                if (parentContext.getContextClass().equals(StockMove.class)) {
                    stockMove = parentContext.asType(StockMove.class);
                    company = stockMove.getCompany();
                } else if (parentContext.getContextClass().equals(ManufOrder.class)) {
                    ManufOrder manufOrder = parentContext.asType(ManufOrder.class);
                    company = manufOrder.getCompany();
                } else {
                    throw new AxelorException(IException.TECHNICAL,
                            IExceptionMessage.STOCK_MOVE_LINE_UNKNOWN_PARENT_CONTEXT);
                }
            } else {
                company = stockMove.getCompany();
            }

            Beans.get(StockMoveLineService.class).setProductInfo(stockMoveLine, company);
            response.setValues(stockMoveLine);
        } catch (Exception e) {
            TraceBackService.trace(response, e);
        }
    }
}
