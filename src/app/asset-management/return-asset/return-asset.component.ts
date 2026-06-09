import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AssetService } from '../../services/asset.service';

interface IssueOption {
  value: string;
  label: string;
  assetId: string;
  assetName: string;
  receiverType: string;
  receiverName: string;
  locationId: string | null;
  personId: string | null;
  issuedToAssetId: string | null;
}

interface ModuleTab {
  id: string;
  label: string;
  subtitle: string;
}

@Component({
  selector: 'app-return-asset',
  templateUrl: './return-asset.component.html',
  styleUrls: ['./return-asset.component.css']
})
export class ReturnAssetComponent implements OnInit {
  public issueDropdownOpen = false;
  public selectedIssueId = '';
  public issueIdOptions: IssueOption[] = [];

  public selectedAssetId = '';
  public selectedLocationId: string | null = null;
  public selectedPersonId: string | null = null;
  public selectedReturnedToAssetId: string | null = null;

  public assetName = '';
  public returnType = '';
  public expectedReturnDate = '';
  public returnDate = '';
  public notes = '';

  public activeModuleTabId = 'return-asset';
  public moduleTabs: ModuleTab[] = [
    { id: 'dashboard', label: 'Dashboard', subtitle: 'Overview & Summary' },
    { id: 'view-assets', label: 'View Assets', subtitle: 'Browse all assets' },
    { id: 'issue-asset', label: 'Issue Asset', subtitle: 'Assign to a user' },
    { id: 'issue-log', label: 'Issue Log', subtitle: 'Track issued assets' },
    { id: 'return-log', label: 'Return Log', subtitle: 'Track returns' },
    { id: 'return-asset', label: 'Return Asset', subtitle: 'Process asset returns' },
    { id: 'reports', label: 'Reports', subtitle: 'Asset analytics' }
  ];

  constructor(public router: Router, private assetService: AssetService) {}

  ngOnInit(): void {
    this.returnDate = new Date().toISOString().slice(0, 10);
    this.loadIssuedAssetsOptions();
  }

  private loadIssuedAssetsOptions(): void {
    this.assetService.getIssuedAssets({ page: 1, pageSize: 1000 }).subscribe({
      next: (response: any) => {
        const raw: any[] = response?.responseData?.data?.assets ?? [];
        this.issueIdOptions = raw.map(item => ({
          value: item._id,
          label: `${item._id} — ${item.assetName} (issued to ${item.receiverName})`,
          assetId: item.assetId,
          assetName: item.assetName,
          receiverType: item.receiverType,
          receiverName: item.receiverName,
          locationId: item.locationId,
          personId: item.personId,
          issuedToAssetId: item.issuedToAssetId
        }));
      },
      error: (err) => {
        console.error('Failed to load issued assets:', err);
      }
    });
  }

  toggleIssueDropdown(): void {
    this.issueDropdownOpen = !this.issueDropdownOpen;
  }

  goToDashboard(): void { this.router.navigate(['/kjusys/asset-dashboard']); }
  goToViewAssets(): void { this.router.navigate(['/kjusys/view-assets']); }
  goToIssueAsset(): void { this.router.navigate(['/kjusys/issue-asset']); }
  goToIssueLog(): void { this.router.navigate(['/kjusys/issue-log']); }
  goToReturnLog(): void { this.router.navigate(['/kjusys/return-log']); }
  goToReports(): void { this.router.navigate(['/kjusys/reports']); }

  onIssueSelect(option: IssueOption): void {
    this.selectedIssueId = option.value;
    this.assetName = option.assetName;
    this.returnType = option.receiverType;
    this.selectedAssetId = option.assetId;
    this.selectedLocationId = option.locationId;
    this.selectedPersonId = option.personId;
    this.selectedReturnedToAssetId = option.issuedToAssetId;
    this.issueDropdownOpen = false;
  }

  getSelectedLabel(): string {
    const selected = this.issueIdOptions.find(opt => opt.value === this.selectedIssueId);
    return selected?.label ?? 'Issue Tag';
  }

  onCancel(): void {
    this.router.navigate(['/kjusys/return-log']);
  }

  onReturnAsset(): void {
    if (!this.selectedIssueId) {
      alert('Please select an issue ID before returning the asset.');
      return;
    }

    const payload = {
      issuetoId: this.selectedIssueId,
      assetId: this.selectedAssetId,
      returnDate: this.returnDate,
      locationId: this.selectedLocationId,
      personId: this.selectedPersonId,
      returnedToAssetId: this.selectedReturnedToAssetId,
      notes: this.notes
    };

    this.assetService.createReturnAsset(payload).subscribe({
      next: () => {
        alert('Asset returned successfully!');
        this.router.navigate(['/kjusys/return-log']);
      },
      error: (err: any) => {
        console.error('Failed to return asset:', err);
        alert(err?.error?.responseData?.error ?? 'Could not return asset.');
      }
    });
  }

  onModuleTabChange(tabId: string): void {
    this.activeModuleTabId = tabId;

    switch (tabId) {
      case 'dashboard':
        this.goToDashboard();
        break;
      case 'view-assets':
        this.goToViewAssets();
        break;
      case 'issue-asset':
        this.goToIssueAsset();
        break;
      case 'issue-log':
        this.goToIssueLog();
        break;
      case 'return-log':
        this.goToReturnLog();
        break;
      case 'return-asset':
        // Already on this page.
        break;
      case 'reports':
        this.goToReports();
        break;
    }
  }
}
