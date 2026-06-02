import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AssetDashboardComponent } from './asset-management/asset-dashboard/asset-dashboard.component';
import { ViewAssetsComponent } from './asset-management/view-assets/view-assets.component';
import { IssueAssetComponent } from './asset-management/issue-asset/issue-asset.component';
import { ReturnLogComponent } from './asset-management/return-log/return-log.component';
import { ReportsComponent } from './asset-management/reports/reports.component';
import { IssueLogComponent } from './asset-management/issue-log/issue-log.component';

const routes: Routes = [
  { path: '',                  component: AssetDashboardComponent, pathMatch: 'full' },
  { path: 'assets/view',       component: ViewAssetsComponent },
  { path: 'assets/issue',      component: IssueAssetComponent },
  { path: 'assets/issue-log',  component: IssueLogComponent },
  { path: 'assets/return-log', component: ReturnLogComponent },
  { path: 'assets/reports',    component: ReportsComponent },
  { path: '**',                redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
