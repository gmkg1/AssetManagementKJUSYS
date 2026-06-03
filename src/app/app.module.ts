import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AssetDashboardComponent } from './asset-management/asset-dashboard/asset-dashboard.component';
import { ViewAssetsComponent } from './asset-management/view-assets/view-assets.component';
import { IssueAssetComponent } from './asset-management/issue-asset/issue-asset.component';
import { IssueLogComponent } from './asset-management/issue-log/issue-log.component';
import { ReturnLogComponent } from './asset-management/return-log/return-log.component';
import { ReportsComponent } from './asset-management/reports/reports.component';

@NgModule({
  declarations: [
    AppComponent,
    AssetDashboardComponent,
    ViewAssetsComponent,
    IssueAssetComponent,
    IssueLogComponent,
    ReturnLogComponent,
    ReportsComponent,
  ],
  imports: [
    BrowserModule,
    CommonModule,
    FormsModule,
    HttpClientModule,
    AppRoutingModule,
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
