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
package com.axelor.apps.base.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.Query;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.base.db.AdvancedExportLine;
import com.axelor.apps.base.db.repo.AdvancedExportLineRepository;
import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaField;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.db.repo.MetaFieldRepository;
import com.axelor.meta.db.repo.MetaModelRepository;
import com.axelor.meta.db.repo.MetaSelectRepository;
import com.axelor.meta.db.repo.MetaTranslationRepository;
import com.axelor.rpc.Context;
import com.axelor.rpc.filter.Filter;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.mysql.jdbc.StringUtils;
import com.opencsv.CSVWriter;

public class AdvancedExportServiceImpl implements AdvancedExportService {
	
	private final Logger log = LoggerFactory.getLogger(AdvancedExportServiceImpl.class);
		
	@Inject
	private MetaFieldRepository metaFieldRepo;
	
	@Inject
	private MetaModelRepository metaModelRepo;
	
	@Inject
	private MetaSelectRepository metaSelectRepo;
	
	@Inject
	private AdvancedExportLineRepository advancedExportLineRepo;
	
	@Inject
	private MetaFiles metaFiles;
	
	@Inject
	private MetaTranslationRepository metaTranslationRepo;
	
	private LinkedHashSet<String> joinFieldSet = new LinkedHashSet<String>();
	
	private List<String> selectJoinFieldList = new ArrayList<>();
	
	private LinkedHashSet<String> selectionJoinFieldSet = new LinkedHashSet<String>();
	
	private LinkedHashSet<String> selectionRelationalJoinFieldSet = new LinkedHashSet<String>();
	
	private String language = "";
	
	private String selectNormalField = "";
	
	private String selectSelectionField = "";
	
	private String temp = "";
	
	private int counter = 0;
	
	private int counter2 = 0;
	
	private int counter3 = 0;
	
	private int nbrField = 0;
	
	private boolean isSelectionField = false;
	
	private int msi = 0;
	
	private int mt = 0;
	
	private List<Object> params = null;
	
	
	@Override
	public String getTargetField(Context context, MetaField metaField, String targetField, MetaModel parentMetaModel) {
		
		if (metaField.getRelationship() == null) {
			
			targetField = (String) context.get("targetField");
			String[] splitField = targetField.split("\\.");
			
			MetaField metaField2 = this.checkLastMetaField(splitField, 0, parentMetaModel, metaField);
			
			if (metaField2.getRelationship() != null) {
				targetField += "." + metaField.getName();
				return targetField;
				
			} else {
				return targetField.replace(splitField[splitField.length - 1], metaField.getName());
			}
			
		} else {
			
			targetField = (String) context.get("targetField");
			String[] splitField = targetField.split("\\.");
			
			MetaField metaField2 = this.checkLastMetaField(splitField, 0, parentMetaModel, metaField);
			
			if (metaField2.getRelationship() != null) {
				targetField += "." + metaField.getName();
				return targetField;
				
			} else {
				return targetField.replace(splitField[splitField.length - 1], metaField.getName());
			}
		}
	}
	
	private MetaField checkLastMetaField(String[] splitField, int i, MetaModel parentMetaModel, MetaField metaField) {
		
		MetaModel metaModel = parentMetaModel;
		MetaField metaField3 = metaField;
		
		if (i <= splitField.length - 1) {
			
			if (splitField[0].equals(splitField[splitField.length - 1])) {
				
				metaField3 = metaFieldRepo.all().filter("self.name = ?1 and self.metaModel = ?2", splitField[i], parentMetaModel).fetchOne();
				
			} else {
				metaField3 = metaFieldRepo.all().filter("self.name = ?1 and self.metaModel = ?2", splitField[i], parentMetaModel).fetchOne();
				
				if (metaField3.getRelationship() != null) {
					metaModel = metaModelRepo.findByName(metaField3.getTypeName());
				}
			}
			metaField3 = checkLastMetaField(splitField, i + 1, metaModel, metaField3);
		}
		return metaField3;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public List<Map> showAdvancedExportData(List<Map<String, Object>> advancedExportLines, MetaModel metaModel, String criteria)
			throws ClassNotFoundException {

		int col = 0;
		String selectField = "";
		String selectJoinField = "";
		String joinField = "";
		String selectionField = "";
		String selectionJoinField = "";
		String selectionRelationalJoinField = "";
		String orderByCol = "";
		List<String> selectFieldList = new ArrayList<>();
		List<String> selectionFieldList = new ArrayList<>();
		List<String> orderByColumns = new ArrayList<>();
		selectJoinFieldList.clear();
		joinFieldSet.clear();
		counter = 0;
		
		language = AuthUtils.getUser().getLanguage();

		for (Map<String, Object> fieldLine : advancedExportLines) {

			AdvancedExportLine advancedExportLine = advancedExportLineRepo
					.find(Long.parseLong(fieldLine.get("id").toString()));

			String[] splitField = advancedExportLine.getTargetField().split("\\.");

			int cnt = this.getExportData(splitField, 0, metaModel);

			if (joinFieldSet.size() > 0 && (!selectNormalField.equals("") || selectNormalField.indexOf(0) == '.')) {
				selectJoinFieldList.add(temp + selectNormalField + " AS " + ("Col_" + (col)));
			}

			if (!selectSelectionField.equals("") && selectionJoinFieldSet.size() > 0) {
				selectionFieldList.add(selectSelectionField + " AS " + ("Col_" + (col)));
				
			} else if (!selectSelectionField.equals("") && selectionRelationalJoinFieldSet.size() > 0) {
				selectionFieldList.add(selectSelectionField + " AS " + ("Col_" + (col)));
				
			} else if (cnt == 0 && selectSelectionField.equals("") && !selectNormalField.equals("")) {
				selectFieldList.add("self." + advancedExportLine.getTargetField() + " AS " + ("Col_" + (col)));
			}

			if (advancedExportLine.getOrderBy()) {
				orderByColumns.add("Col_" + col);
			}
			selectNormalField = "";
			selectSelectionField = "";
			temp = "";
			cnt = 0;
			counter2 = 0;
			counter3 = 0;
			nbrField++;
			col++;
		}

		selectField = String.join(",", selectFieldList);
		joinField = String.join(" ", joinFieldSet);
		selectJoinField = String.join(",", selectJoinFieldList);
		selectionField = String.join(",", selectionFieldList);
		selectionJoinField = String.join(" ", selectionJoinFieldSet);
		selectionRelationalJoinField = String.join(" ", selectionRelationalJoinFieldSet);
		
		params = null;
		criteria = getCriteria(metaModel, criteria);
		
		if (orderByColumns.size() > 0)
			orderByCol = " ORDER BY " + String.join(",", orderByColumns);
		
		return this.createQuery(metaModel, selectField, joinField, selectJoinField, selectionField,
				selectionJoinField, selectionRelationalJoinField, criteria, orderByCol);
	}

	private String getCriteria(MetaModel metaModel, String criteria) {
		
		if (!StringUtils.isNullOrEmpty(criteria)){
			log.debug("criteria : {}", criteria.substring(1, criteria.length()-1));
			criteria = " WHERE self.id IN (" + criteria.substring(1, criteria.length()-1) + ")";
			return criteria;
		}	
		
		Filter filter = getJpaSecurityFilter(metaModel);
		
		if (filter != null) {
			String permissionFilter = filter.getQuery();
			if (StringUtils.isNullOrEmpty(criteria)) {
				criteria = " WHERE " + permissionFilter;
			}
			else {
				criteria = criteria + " AND (" + permissionFilter + ")"; 
			}
			params = filter.getParams();
		}
		
		return criteria;
	}
	
	@Override
	public Filter getJpaSecurityFilter(MetaModel metaModel) {
		
		JpaSecurity jpaSecurity = Beans.get(JpaSecurity.class);
		
		try {
			Filter filter = jpaSecurity.getFilter(JpaSecurity.CAN_EXPORT, 
					(Class<? extends Model>) Class.forName(metaModel.getFullName()), null);
			
			return filter;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<Map> createQuery(MetaModel metaModel, String selectField, String joinField,
			String selectJoinField, String selectionField, String selectionJoinField,
			String selectionRelationalJoinField, String criteria, String orderByCol) {
		
		Query query = null;

		if (!selectField.equals("") && !selectJoinField.equals("") && !selectionField.equals("") && !selectionRelationalJoinField.equals("")) {

			log.debug("query : {}", "SELECT " + selectField + "," + selectJoinField + "," + selectionField + " from " + metaModel.getName()
					+ " self " + "LEFT JOIN " + joinField + " " + selectionJoinField + " " + selectionRelationalJoinField + criteria + orderByCol);
			
			query = JPA.em().createQuery("SELECT NEW Map(" + selectField + "," + selectJoinField + "," + selectionField + ") from "
					+ metaModel.getName() + " self " + "LEFT JOIN " + joinField + " " + selectionJoinField + " "
					+ selectionRelationalJoinField + criteria + orderByCol, Map.class);
			
		} else if (!selectField.equals("") && !joinField.equals("") && !selectionField.equals("") && selectionRelationalJoinField.equals("")) {

			log.debug("query : {}", "SELECT " + selectField + "," + selectJoinField + "," + selectionField + " from " + metaModel.getName() + " self "
					+ "LEFT JOIN " + joinField + " " + selectionJoinField + criteria + orderByCol);
			
			query = JPA.em().createQuery("SELECT NEW Map(" + selectField + "," + selectJoinField + "," + selectionField + ") from "
					+ metaModel.getName() + " self " + "LEFT JOIN " + joinField + " " + selectionJoinField
					+ criteria + orderByCol, Map.class);

		} else if (selectField.equals("") && selectJoinField.equals("") && !joinField.equals("") && !selectionField.equals("") && !selectionRelationalJoinField.equals("")) {

			log.debug("query : {}", "SELECT " + selectionField + " from " + metaModel.getName() + " self "
					+ "LEFT JOIN " + joinField + " " + selectionJoinField + " " + selectionRelationalJoinField + criteria + orderByCol);
			
			query = JPA.em().createQuery("SELECT NEW Map(" + selectionField + ") from "
					+ metaModel.getName() + " self " + "LEFT JOIN " + joinField + " " + selectionJoinField + " "
					+ selectionRelationalJoinField + criteria + orderByCol, Map.class);
			
		} else if (selectField.equals("") && !selectJoinField.equals("") && !joinField.equals("") && !selectionField.equals("") && !selectionRelationalJoinField.equals("")) {

			log.debug("query : {}", "SELECT " + selectJoinField + "," + selectionField + " from " + metaModel.getName() + " self "
					+ "LEFT JOIN " + joinField + " " + selectionJoinField + " " + selectionRelationalJoinField + criteria + orderByCol);
			
			query = JPA.em().createQuery("SELECT NEW Map(" + selectJoinField + "," + selectionField + ") from "
					+ metaModel.getName() + " self " + "LEFT JOIN " + joinField + " " + selectionJoinField + " "
					+ selectionRelationalJoinField + criteria + orderByCol, Map.class);
			
		} else if (selectField.equals("") && !joinField.equals("") && !selectionField.equals("") && selectionRelationalJoinField.equals("")) {

			log.debug("query : {}", "SELECT " + selectJoinField + "," + selectionField + " from " + metaModel.getName() + " self "
					+ "LEFT JOIN " + joinField + " " + selectionJoinField + criteria + orderByCol);
			
			query = JPA.em().createQuery("SELECT NEW Map(" + selectJoinField + "," + selectionField + ") from "
					+ metaModel.getName() + " self " + "LEFT JOIN " + joinField + " " + selectionJoinField
					+ criteria + orderByCol, Map.class);
			
		} else if (selectField.equals("") && joinField.equals("") && !selectionField.equals("") && selectionRelationalJoinField.equals("")) {
			
			log.debug("query : {}", "SELECT " + selectionField + " from " + metaModel.getName() + " self "
					+ selectionJoinField + criteria + orderByCol);
			
			query = JPA.em().createQuery("SELECT NEW Map(" + selectionField + ") from " + metaModel.getName() + " self "
					+ selectionJoinField + criteria + orderByCol, Map.class);
			
		} else if (!selectField.equals("") && joinField.equals("") && !selectionField.equals("") && selectionRelationalJoinField.equals("")) {
				
				log.debug("query : {}", "SELECT " + selectField + "," + selectionField + " from " + metaModel.getName() + " self "
						+ selectionJoinField + criteria + orderByCol);
				
				query = JPA.em().createQuery("SELECT NEW Map(" + selectField + "," + selectionField + ") from " + metaModel.getName() + " self "
						+ selectionJoinField + criteria + orderByCol, Map.class);
				
		} else if (!selectField.equals("") && selectJoinField.equals("") && !selectionField.equals("") && !selectionRelationalJoinField.equals("")) {
			
			log.debug("query : {}", "SELECT " + selectField + "," + selectionField + " from " + metaModel.getName() + " self "
					+ "LEFT JOIN " + joinField + " " + selectionJoinField + " " + selectionRelationalJoinField + criteria + orderByCol);
			
			query = JPA.em().createQuery("SELECT NEW Map(" + selectField + "," + selectionField + ") from " + metaModel.getName() + " self "
					+ "LEFT JOIN " + joinField + " " + selectionJoinField + " " + selectionRelationalJoinField + criteria + orderByCol, Map.class);	
			
		} else if (selectField.equals("") && joinField.equals("") && !selectionField.equals("") && selectionJoinField.equals("") && !selectionRelationalJoinField.equals("")) {
			
			log.debug("query : {}", "SELECT " + selectionField + " from " + metaModel.getName() + " self "
					+ selectionRelationalJoinField + criteria + orderByCol);
			
			query = JPA.em().createQuery("SELECT NEW Map(" + selectionField + ") from " + metaModel.getName() + " self "
					+ selectionRelationalJoinField + criteria + orderByCol, Map.class);
			
		} else if (!selectField.equals("") && !joinField.equals("") && selectionField.equals("") && selectionRelationalJoinField.equals("")) {
			
			log.debug("query : {}", "SELECT " + selectField + "," + selectJoinField + " from " + metaModel.getName() + " self "
					 + "LEFT JOIN " + joinField + criteria + orderByCol);
			
			query = JPA.em().createQuery("SELECT NEW Map(" + selectField + "," + selectJoinField + ") from " + metaModel.getName() + " self "
					 + "LEFT JOIN " + joinField + criteria + orderByCol, Map.class);
		
		} else if (selectField.equals("") && !joinField.equals("") && selectionField.equals("") && selectionRelationalJoinField.equals("")) {
			
			log.debug("query : {}", "SELECT " + selectJoinField + " from " + metaModel.getName() + " self "
					 + "LEFT JOIN " + joinField + criteria + orderByCol);
			
			query = JPA.em().createQuery("SELECT NEW Map(" + selectJoinField + ") from " + metaModel.getName() + " self "
					 + "LEFT JOIN " + joinField + criteria + orderByCol, Map.class);
			
		} else {
			log.debug("query : {}","SELECT " + selectField + " from " + metaModel.getName() + " self" + criteria + orderByCol);
			
			query = JPA.em().createQuery("SELECT NEW Map(" + selectField + ") from " + metaModel.getName()
					+ " self" + criteria + orderByCol, Map.class);
		}
		
		if (params != null) {
			for (int i=0; i<params.size(); i++) {
				query.setParameter(i, params.get(i));
			}
		}

		return query.getResultList();
	}
	
	private void checkSelectionField(String[] fieldName, int i, MetaModel metaModel) throws ClassNotFoundException {

		Class<?> klass = Class.forName(metaModel.getFullName());
		Mapper mapper = Mapper.of(klass);
		MetaSelect metaSelect = metaSelectRepo.findByName(mapper.getProperty(fieldName[i]).getSelection());
		
		if (metaSelect != null) {
			MetaField metaField = metaFieldRepo.all().filter("self.name = ?1 AND self.metaModel = ?2", fieldName[i], metaModel).fetchOne();
			isSelectionField = true;
			msi++;
			mt++;
			
			if (counter > 0 && i != 0) {
				
				if (language.equals("fr")) {
					
					if (metaField.getTypeName().equals("String")) {
						
						selectionRelationalJoinFieldSet.add("LEFT JOIN MetaSelectItem " + ("msi_"+(msi)) + " ON " + temp
								+ "." + fieldName[i] + " = " + ("msi_"+(msi)) + ".value AND "
								+ ("msi_"+(msi)) + ".select = " + metaSelect.getId() + " LEFT JOIN MetaTranslation "
								+ ("mt_"+(mt)) + " ON " + ("msi_"+(msi)) + ".title = " + ("mt_"+(mt))
								+ ".key AND " + ("mt_"+(mt)) + ".language = \'" + language + "\'");
						
					} else {
						selectionRelationalJoinFieldSet.add("LEFT JOIN MetaSelectItem " + ("msi_"+(msi)) + " ON " + temp
								+ "." + fieldName[i] + " = CAST(" + ("msi_"+(msi)) + ".value AS int) AND "
								+ ("msi_"+(msi)) + ".select = " + metaSelect.getId() + " LEFT JOIN MetaTranslation "
								+ ("mt_"+(mt)) + " ON " + ("msi_"+(msi)) + ".title = " + ("mt_"+(mt))
								+ ".key AND " + ("mt_"+(mt)) + ".language = \'" + language + "\'");
					}
				} else {
					if (metaField.getTypeName().equals("String")) {
						
						selectionRelationalJoinFieldSet.add("LEFT JOIN MetaSelectItem " + ("msi_"+(msi)) + " ON " + temp + "."
								+ fieldName[i] + " = " + ("msi_"+(msi)) + ".value AND " + ("msi_"+(msi))
								+ ".select = " + metaSelect.getId());
						
					} else {
						selectionRelationalJoinFieldSet.add("LEFT JOIN MetaSelectItem " + ("msi_"+(msi)) + " ON " + temp + "."
								+ fieldName[i] + " = CAST(" + ("msi_"+(msi)) + ".value AS int) AND " + ("msi_"+(msi))
								+ ".select = " + metaSelect.getId());
					}
				}
			} else {
				if (language.equals("fr")) {
					
					if (metaField.getTypeName().equals("String")) {
						
						selectionJoinFieldSet.add("LEFT JOIN MetaSelectItem " + ("msi_"+(msi)) + " ON self."
								+ fieldName[i] + " = " + ("msi_"+(msi)) + ".value AND "
								+ ("msi_"+(msi)) + ".select = " + metaSelect.getId() + " LEFT JOIN MetaTranslation "
								+ ("mt_"+(mt)) + " ON " + ("msi_"+(msi)) + ".title = " + ("mt_"+(mt))
								+ ".key AND " + ("mt_"+(mt)) + ".language = \'" + language + "\'");
						
					} else {
						selectionJoinFieldSet.add("LEFT JOIN MetaSelectItem " + ("msi_"+(msi)) + " ON self."
								+ fieldName[i] + " = CAST(" + ("msi_"+(msi)) + ".value AS int) AND "
								+ ("msi_"+(msi)) + ".select = " + metaSelect.getId() + " LEFT JOIN MetaTranslation "
								+ ("mt_"+(mt)) + " ON " + ("msi_"+(msi)) + ".title = " + ("mt_"+(mt))
								+ ".key AND " + ("mt_"+(mt)) + ".language = \'" + language + "\'");
					}
				} else {
					if (metaField.getTypeName().equals("String")) {
						
						selectionJoinFieldSet.add("LEFT JOIN MetaSelectItem " + ("msi_"+(msi)) + " ON self."
								+ fieldName[i] + " = " + ("msi_"+(msi)) + ".value AND " + ("msi_"+(msi))
								+ ".select = " + metaSelect.getId());
						
					} else {
						selectionJoinFieldSet.add("LEFT JOIN MetaSelectItem " + ("msi_"+(msi)) + " ON self."
								+ fieldName[i] + " = CAST(" + ("msi_"+(msi)) + ".value AS int) AND " + ("msi_"+(msi))
								+ ".select = " + metaSelect.getId());
					}
				}
			}
		}
	}
	
	private int getExportData(String[] splitField, int i, MetaModel metaModel) throws ClassNotFoundException {
		
		if (i <= splitField.length-1) {
			MetaField relationalField = metaFieldRepo.all()
					.filter("self.name = ?1 and self.metaModel = ?2", splitField[i], metaModel)
					.fetchOne();
			
			MetaModel subMetaModel = metaModelRepo.all()
					.filter("self.name = ?1", relationalField.getTypeName()).fetchOne();
			
			if (!relationalField.getPackageName().startsWith("java")) {
				
				counter++;
				if (counter2 != 0 || counter3 != 0) {
					selectNormalField = "";
				}
				if (i != 0) {
					for (int j = 0; j <= i; j++) {
						if (j == 0) {
							if (nbrField > 0 && joinFieldSet.size() > 0) {
								joinFieldSet.add("LEFT JOIN self." + splitField[j] + " " + splitField[j]);
								temp = splitField[j];
							} else {
								joinFieldSet.add("self." + splitField[j] + " " + splitField[j]);
								temp = splitField[j];
							}
						} else {
							if (!temp.equals(splitField[i])) {
								joinFieldSet.add("LEFT JOIN " + temp + "." + splitField[j] + " " + splitField[j]);
								temp = splitField[j];
							}
						}
					}
				} else {
					if (nbrField > 0 && joinFieldSet.size() > 0) {
						if (!joinFieldSet.contains("self." + splitField[i] + " " + splitField[i])) {
							joinFieldSet.add("LEFT JOIN self." + splitField[i] + " " + splitField[i]);
							temp = splitField[i];
						} else {
							temp = splitField[i];
						}
					} else {
						joinFieldSet.add("self." + splitField[i] + " " + splitField[i]);
						temp = splitField[i];
					}
				}
					
			} else {
				
				this.checkSelectionField(splitField, i, metaModel);
				
				if (isSelectionField) {
					if (i == 0) {
						
						selectSelectionField = "";
						if (language.equals("fr")) {
							selectSelectionField += ("mt_"+(mt)) + ".message";
							
						} else {
							selectSelectionField += ("msi_"+(msi)) + ".title";
						}
					} else {
						
						if (language.equals("fr")) {
							selectSelectionField += ("mt_"+(mt)) + ".message";
							
						} else {
							selectSelectionField += ("msi_"+(msi)) + ".title";
						}
					}
					isSelectionField = false;
				} else {

					if (i == 0) {
						selectNormalField = "";
						selectNormalField += "self." + splitField[i];
					} else {
						selectNormalField += "." + splitField[i];
					}
				}
			}
			getExportData(splitField, i+1, subMetaModel);
		}
		return counter;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public MetaFile advancedExportPDF(MetaFile exportFile, List<Map<String, Object>> advancedExportLines,
			List<Map> allFieldDataList, MetaModel metaModel) throws DocumentException, IOException {

		Document document = new Document();
		
		File file = File.createTempFile("Export", ".pdf");
		FileOutputStream  outStream = new FileOutputStream(file);
		PdfWriter.getInstance(document, outStream);
		
		document.open();
		
		PdfPTable table = new PdfPTable(advancedExportLines.size());
		PdfPCell headerCell;
		PdfPCell cell;
		
		for (Map<String, Object> fieldLine : advancedExportLines) {
			
			AdvancedExportLine advancedExportLine = advancedExportLineRepo.find(Long.parseLong(fieldLine.get("id").toString()));
			
			MetaTranslation metaTranslation = metaTranslationRepo.all().filter("self.key = ?1 and self.language = ?2",advancedExportLine.getTitle(), language).fetchOne();
			if (metaTranslation != null) {
				if (!metaTranslation.getMessage().equals("")) {
					headerCell = new PdfPCell(new Phrase(metaTranslation.getMessage(), new Font(BaseFont.createFont(), 8, 0, BaseColor.WHITE)));
				} else {
					headerCell = new PdfPCell(new Phrase(advancedExportLine.getTitle(), new Font(BaseFont.createFont(), 8, 0, BaseColor.WHITE)));
				}
			} else {
				headerCell = new PdfPCell(new Phrase(advancedExportLine.getTitle(), new Font(BaseFont.createFont(), 8, 0, BaseColor.WHITE)));
			}
			
			headerCell.setBackgroundColor(BaseColor.GRAY);
			headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.addCell(headerCell);
		}
		
		Font font = new Font();
		font.setSize(7);
		
		for (Map<String, Object> field : allFieldDataList) {
			String[] allCols = field.keySet().toArray(new String[field.size()]);
			Integer[] allColIndices = new Integer[allCols.length];
			
			for (int j = 0; j < allCols.length; j++) {
				String col = allCols[j];
				allColIndices[j] = Integer.parseInt(col.replace("Col_", ""));
			}
			Arrays.sort(allColIndices);
			
			for(Integer colIndex: allColIndices) {
				String colName = "Col_" + String.valueOf(colIndex);
				Object value = field.get(colName);
				if (value == null) {
					cell = new PdfPCell(new Phrase(null, font));
				} else {
					cell = new PdfPCell(new Phrase(value.toString(), font));
				}
				table.addCell(cell);
			}
		}
		document.add(table);
		document.close();
		
		return exportPDFFile(exportFile, metaModel, file);
	}
	
	private MetaFile exportPDFFile(MetaFile inputFile, MetaModel metaModel, File file) throws DocumentException, IOException {
		
		String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy HH:mm:ss"));
		String fileName = metaModel.getName() + "-" + date + ".pdf";
		
		log.debug("File created: {}, Size: {}", file.getName(), file.getTotalSpace());
		log.debug("Meta files: {}", metaFiles);
		
		FileInputStream inStream = new FileInputStream(file);
		inputFile = metaFiles.upload(inStream, fileName);
		
		inStream.close();
		
		file.delete();
		
		return inputFile;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public MetaFile advancedExportExcel(MetaFile exportFile, MetaModel metaModel, List<Map> allFieldDataList,
			List<Map<String, Object>> advancedExportLines) throws IOException {

		Workbook workbook = new XSSFWorkbook();
		
		Sheet sheet = workbook.createSheet();
		int rowNum = 0;
		
		Row headerRow = sheet.createRow(rowNum++);
		int colHeaderNum = 0;
		
		for (Map<String, Object> fieldLine : advancedExportLines) {
			
			AdvancedExportLine advancedExportLine = advancedExportLineRepo.find(Long.parseLong(fieldLine.get("id").toString()));
			
			Cell headerCell = headerRow.createCell(colHeaderNum++);
			
			MetaTranslation metaTranslation = metaTranslationRepo.all().filter("self.key = ?1 and self.language = ?2",advancedExportLine.getTitle(), language).fetchOne();
			if (metaTranslation != null) {
				if (!metaTranslation.getMessage().equals("")) {
					headerCell.setCellValue(metaTranslation.getMessage());
				} else {
					headerCell.setCellValue(advancedExportLine.getTitle());
				}
			} else {
				headerCell.setCellValue(advancedExportLine.getTitle());
			}
		}
		
		for (Map<String, Object> field : allFieldDataList) {
			String[] allCols = field.keySet().toArray(new String[field.size()]);
			Integer[] allColIndices = new Integer[allCols.length];
			
			for (int j = 0; j < allCols.length; j++) {
				String col = allCols[j];
				allColIndices[j] = Integer.parseInt(col.replace("Col_", ""));
			}
			Arrays.sort(allColIndices);
			
			Row row = sheet.createRow(rowNum++);
			int colNum = 0;

			for (Integer colIndex : allColIndices) {
				String colName = "Col_" + String.valueOf(colIndex);
				Object value = field.get(colName);
				Cell cell = row.createCell(colNum++);
				if (value == null)
					continue;
				cell.setCellValue(value.toString());
			}
		}
		return exportExcelFile(exportFile, metaModel, workbook);
	}
	
	private MetaFile exportExcelFile(MetaFile inputFile, MetaModel metaModel, Workbook workbook) throws IOException {
		
		String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy HH:mm:ss"));
		String fileName = metaModel.getName() + "-" + date + ".xlsx";
		
		File file = File.createTempFile("Export", ".xlsx");
		FileOutputStream  outStream = new FileOutputStream(file);
		workbook.write(outStream);
		outStream.close();
		
		log.debug("File created: {}, Size: {}", file.getName(), file.getTotalSpace());
		log.debug("Meta files: {}", metaFiles);
		
		FileInputStream inStream = new FileInputStream(file);
		inputFile = metaFiles.upload(inStream, fileName);

		inStream.close();
		
		file.delete();
		
		return inputFile;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public MetaFile advancedExportCSV(MetaFile exportFile, MetaModel metaModel, List<Map> allFieldDataList,
			List<Map<String, Object>> advancedExportLines) throws IOException {
		
		File csvFile = File.createTempFile("Export", ".csv");
		CSVWriter csvWriter = new CSVWriter(new FileWriter(csvFile, true), ';');
		String[] totalCols = new String[advancedExportLines.size()];
		int i = 0;
		
		for (Map<String, Object> fieldLine : advancedExportLines) {
			AdvancedExportLine advancedExportLine = advancedExportLineRepo.find(Long.parseLong(fieldLine.get("id").toString()));
			totalCols[i++] = advancedExportLine.getTargetField();
		}
		csvWriter.writeNext(totalCols);
		
		i = 0;
		for (Map<String, Object> field : allFieldDataList) {
			String[] allCols = field.keySet().toArray(new String[field.size()]);
			Integer[] allColIndices = new Integer[allCols.length];
			
			for (int j = 0; j < allCols.length; j++) {
				String col = allCols[j];
				allColIndices[j] = Integer.parseInt(col.replace("Col_", ""));
			}
			Arrays.sort(allColIndices);
			
			for(Integer colIndex: allColIndices) {
				String colName = "Col_" + String.valueOf(colIndex);
				Object value = field.get(colName);
				if (value == null || value == "") {
					totalCols[i++] = null;
				} else {
					totalCols[i++] = value.toString();
				}
			}
			csvWriter.writeNext(totalCols);
			i = 0;
		}
		csvWriter.close();
		
		return exportCSVFile(exportFile, metaModel, csvFile);
	}
	
	private MetaFile exportCSVFile(MetaFile inputFile, MetaModel metaModel, File csvFile) throws IOException {
		
		String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy HH:mm:ss"));
		String fileName = metaModel.getName() + "-" + date + ".csv";
		
		log.debug("File created: {}, Size: {}", csvFile.getName(), csvFile.getTotalSpace());
		log.debug("Meta files: {}", metaFiles);
		
		FileInputStream inStream = new FileInputStream(csvFile);
		inputFile = metaFiles.upload(inStream, fileName);
		
		inStream.close();
		
		csvFile.delete();
		
		return inputFile;
	}
	
}
