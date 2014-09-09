/*
 * Copyright (C) 2013 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.client;

import org.opendatakit.aggregate.constants.common.SubTabs;
import org.opendatakit.aggregate.constants.common.Tabs;
import org.opendatakit.common.security.common.GrantedAuthorityName;

import com.google.gwt.user.client.ui.Widget;

public class OdkTablesTabUI extends AggregateTabBase {

  public OdkTablesTabUI(AggregateUI baseUI) {
    super();

    // add the subtabs
    addSubTab(new OdkTablesCurrentTablesSubTab(), SubTabs.CURRENTTABLES);
    addSubTab(new OdkTablesViewTableSubTab(), SubTabs.VIEWTABLE);
    addSubTab(new OdkTablesManageInstanceFilesSubTab(), SubTabs.MANAGE_INSTANCE_FILES);
    addSubTab(new OdkTablesManageTableFilesSubTab(), SubTabs.MANAGE_TABLE_ID_FILES);
    addSubTab(new OdkTablesManageAppLevelFilesSubTab(), SubTabs.MANAGE_APP_LEVEL_FILES);

    updateVisibilityOdkTablesSubTabs();

    // register handler to manage tab selection change (and selecting our tab)
    registerClickHandlers(Tabs.ODKTABLES, baseUI);
  }

  public void updateVisibilityOdkTablesSubTabs() {

    /**
     * Admin tabs are still visible, but they have all
     * delete and add features disabled.
     */
    if (AggregateUI.getUI().getUserInfo().getGrantedAuthorities()
        .contains(GrantedAuthorityName.ROLE_SYNCHRONIZE_TABLES)) {
      changeVisibilityOdkTablesSyncSubTabs(true);
      changeVisibilityOdkTablesAdminSubTabs(true);
    } else {
      changeVisibilityOdkTablesSyncSubTabs(false);
      changeVisibilityOdkTablesAdminSubTabs(false);
    }

  }

  private void changeVisibilityOdkTablesSyncSubTabs(boolean outcome) {

    // hide the current files sub-tab
    {
      SubTabInterface odkTablesCurrentTables = getSubTab(SubTabs.CURRENTTABLES);
      OdkTablesCurrentTablesSubTab subTab = ((OdkTablesCurrentTablesSubTab) odkTablesCurrentTables);
      if (subTab != null) {
        subTab.setVisible(outcome);
        if ( outcome ) {
          subTab.update();
        }
      }
    }
    // hide the table data sub-tab
    {
      SubTabInterface odkTablesTableData = getSubTab(SubTabs.VIEWTABLE);
      OdkTablesViewTableSubTab subTab = ((OdkTablesViewTableSubTab) odkTablesTableData);
      if (subTab != null) {
        subTab.setVisible(outcome);
        if ( outcome ) {
          subTab.update();
        }
      }
    }
    // hide the table attachments sub-tab
    {
      SubTabInterface odkTablesTableAttachments = getSubTab(SubTabs.MANAGE_INSTANCE_FILES);
      OdkTablesManageInstanceFilesSubTab subTab = ((OdkTablesManageInstanceFilesSubTab) odkTablesTableAttachments);
      if (subTab != null) {
        subTab.setVisible(outcome);
        if ( outcome ) {
          subTab.update();
        }
      }
    }

    for (int i = 0; i < subTabPosition.size(); i++) {
      if (subTabPosition.get(i).equals(SubTabs.CURRENTTABLES) ||
          subTabPosition.get(i).equals(SubTabs.VIEWTABLE) ||
          subTabPosition.get(i).equals(SubTabs.MANAGE_INSTANCE_FILES)) {
        Widget w = ((Widget) this.getTabBar().getTab(i));
        if (w != null) {
          w.setVisible(outcome);
        }
      }
    }
  }

  private void changeVisibilityOdkTablesAdminSubTabs(boolean outcome) {

    // hide the app-level files sub-tab
    {
      SubTabInterface odkTablesAdmin = getSubTab(SubTabs.MANAGE_APP_LEVEL_FILES);
      OdkTablesManageAppLevelFilesSubTab subTab = ((OdkTablesManageAppLevelFilesSubTab) odkTablesAdmin);
      if (subTab != null) {
        subTab.setVisible(outcome);
        if ( outcome ) {
          subTab.update();
        }
      }
    }
    // hide the app-level files sub-tab
    {
      SubTabInterface odkTablesAdmin = getSubTab(SubTabs.MANAGE_TABLE_ID_FILES);
      OdkTablesManageTableFilesSubTab subTab = ((OdkTablesManageTableFilesSubTab) odkTablesAdmin);
      if (subTab != null) {
        subTab.setVisible(outcome);
        if ( outcome ) {
          subTab.update();
        }
      }
    }

    for (int i = 0; i < subTabPosition.size(); i++) {
      if (subTabPosition.get(i).equals(SubTabs.MANAGE_APP_LEVEL_FILES) ||
          subTabPosition.get(i).equals(SubTabs.MANAGE_TABLE_ID_FILES)) {
        Widget w = ((Widget) this.getTabBar().getTab(i));
        if (w != null) {
          w.setVisible(outcome);
        }
      }
    }
  }

}
