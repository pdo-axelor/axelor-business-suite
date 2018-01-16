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
package com.axelor.apps.businessproduction.service;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.user.UserService;
import com.axelor.apps.production.db.BillOfMaterial;
import com.axelor.apps.production.db.ProductionOrder;
import com.axelor.apps.production.exceptions.IExceptionMessage;
import com.axelor.apps.production.service.ProductionOrderSaleOrderServiceImpl;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class ProductionOrderSaleOrderServiceBusinessImpl extends ProductionOrderSaleOrderServiceImpl {

	private final Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	protected ProductionOrderServiceBusinessImpl productionOrderService;

	@Inject
	public ProductionOrderSaleOrderServiceBusinessImpl(
			UserService userInfoService, ProductionOrderServiceBusinessImpl productionOrderService) {
		super(userInfoService);
		
		this.productionOrderService = productionOrderService;
	}

	@Override
	public List<Long> generateProductionOrder(SaleOrder saleOrder) throws AxelorException  {

		List<Long> productionOrderIdList = new ArrayList<Long>();
		if(saleOrder.getSaleOrderLineList() != null)  {

			ProductionOrder productionOrder = null;
			for(SaleOrderLine saleOrderLine : saleOrder.getSaleOrderLineList())  {

				productionOrder = this.generateProductionOrder(saleOrderLine);
				if (productionOrder != null){
					productionOrderIdList.add(productionOrder.getId());
				}

			}

		}

		return productionOrderIdList;

	}


	@Override
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public ProductionOrder generateProductionOrder(SaleOrderLine saleOrderLine) throws AxelorException  {

		Product product = saleOrderLine.getProduct();

		if(saleOrderLine.getSaleSupplySelect() == ProductRepository.SALE_SUPPLY_PRODUCE && product != null && product.getProductTypeSelect().equals(ProductRepository.PRODUCT_TYPE_STORABLE) )  {

			BillOfMaterial billOfMaterial = saleOrderLine.getBillOfMaterial();

			if (billOfMaterial == null) {
				billOfMaterial = product.getDefaultBillOfMaterial();
			}

			if (billOfMaterial == null && product.getParentProduct() != null) {
				billOfMaterial = product.getParentProduct().getDefaultBillOfMaterial();
			}

			if (billOfMaterial == null) {
				throw new AxelorException(saleOrderLine, IException.CONFIGURATION_ERROR, I18n.get(IExceptionMessage.PRODUCTION_ORDER_SALES_ORDER_NO_BOM), product.getName(), product.getCode());
			}
			
			Unit unit = saleOrderLine.getProduct().getUnit();
			BigDecimal qty = saleOrderLine.getQty();
			if(!unit.equals(saleOrderLine.getUnit())){
				qty = unitConversionService.convertWithProduct(saleOrderLine.getUnit(), unit, qty, saleOrderLine.getProduct());
			}
			return productionOrderRepo.save(productionOrderService.generateProductionOrder(product, billOfMaterial, qty, saleOrderLine.getSaleOrder().getProject(), LocalDateTime.now()));

		}

		return null;

	}


	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void createSaleOrder(ProductionOrder productionOrder) throws AxelorException  {

		logger.debug("Création d'un devis client pour l'ordre de production : {}",
				new Object[] { productionOrder.getProductionOrderSeq() });

		Project project = productionOrder.getProject();

		project.getClientPartner();

//		if(businessFolder.getCompany() != null)  {
//
//			SaleOrder saleOrder = saleOrderServiceStockImpl.createSaleOrder(
//					businessFolder,
//					user,
//					businessFolder.getCompany(),
//					null,
//					partner.getCurrency(),
//					null,
//					null,
//					null,
//					saleOrderServiceStockImpl.getLocation(businessProject.getCompany()),
//					today,
//					Beans.get(PriceListRepository.class).all().filter("self.partner = ?1 AND self.typeSelect = 1", partner).fetchOne(),
//					partner);
//
//			Beans.get(SaleOrderRepository.class).save(saleOrder);
//
//		}
//
//		//TODO
//
//		for(SaleOrderLine saleOrderLine : saleOrderLineList)  {
//
//			purchaseOrder.addPurchaseOrderLineListItem(purchaseOrderLineService.createPurchaseOrderLine(purchaseOrder, saleOrderLine));
//
//		}
//
//		purchaseOrderService.computePurchaseOrder(purchaseOrder);
//
//		purchaseOrder.save();
	}


}
