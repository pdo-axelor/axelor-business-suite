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
package com.axelor.apps.base.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axelor.apps.base.db.App;
import com.axelor.apps.base.db.repo.AppRepository;
import com.axelor.apps.base.exceptions.IAppExceptionMessages;
import com.axelor.apps.base.service.app.AppService;
import com.axelor.common.Inflector;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Inject;

public class AppController {
	
	@Inject
	private AppService appService;
	
	@Inject
	private AppRepository appRepo;

	@Inject
	private MetaViewRepository metaViewRepo;
	
	public void importDataDemo(ActionRequest request, ActionResponse response) {
		
		App app = request.getContext().asType(App.class);
		app = appRepo.find(app.getId());
		
		response.setFlash(appService.importDataDemo(app));
		
		response.setReload(true);
	}
	
	public void installApp(ActionRequest request, ActionResponse response) {
		
		App app = request.getContext().asType(App.class);
		app = appRepo.find(app.getId());
		
		appService.installApp(app, false);
		
		response.setReload(true);
	}
	
	public void configure(ActionRequest request, ActionResponse response) {
		
		App app = request.getContext().asType(App.class);
		
		String code = app.getCode();
		String appName = Inflector.getInstance().camelize(code);
		String viewName = "app-" + code + "-config-form";
		
		if (metaViewRepo.findByName(viewName) == null) {
			response.setFlash(I18n.get(IAppExceptionMessages.NO_CONFIG_REQUIRED));
		}
		else {
			response.setView(ActionView.define(I18n.get("Configure") + ": " + app.getName())
				.add("form", viewName)
				.model("com.axelor.apps.base.db.App" + appName)
				.context("_showRecord", app.getId())
				.map());
		}
	}
	
	public void uninstallApp(ActionRequest request, ActionResponse response) throws AxelorException {
		
		App app = request.getContext().asType(App.class);
		app = appRepo.find(app.getId());
		
		appService.unInstallApp(app);
		
		response.setReload(true);
	}
	
	public void bulkInstall(ActionRequest request, ActionResponse response) {
		
		Context context = request.getContext();
		
		Set<Map<String,Object>> apps =  new HashSet<Map<String,Object>>();
		Collection<Map<String,Object>> appsSet = (Collection<Map<String,Object>>)context.get("appsSet");
		if (appsSet != null) {
			apps.addAll(appsSet);
		}

		Boolean importDemo = (Boolean) context.get("importDemoData");
		
		String language = (String) context.get("languageSelect");
		
		List<App> appList = new ArrayList<App>();
		for (Map<String,Object> appData : apps) {
			App app = appRepo.find(Long.parseLong(appData.get("id").toString()));
			app = appService.updateLanguage(app, language);
			appList.add(app);
		}
		
		appList = appService.sortApps(appList);
		
		for (App app : appList) {
			app = appRepo.find(app.getId());
			app = appService.installApp(app, importDemo);
		}
		
		response.setFlash(I18n.get(IAppExceptionMessages.BULK_INSTALL_SUCCESS));
		response.setSignal("refresh-app", true);
	}
	
	public void refreshApp(ActionRequest request, ActionResponse response) {
		 
		try {
			appService.refreshApp();
			response.setNotify(I18n.get(IAppExceptionMessages.REFRESH_APP_SUCCESS));
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			response.setNotify(I18n.get(IAppExceptionMessages.REFRESH_APP_ERROR));
		}
 
    }

}
