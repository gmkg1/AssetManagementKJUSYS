import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-issue-asset',
  templateUrl: './issue-asset.component.html',
  styleUrls: ['./issue-asset.component.scss'],
})
export class IssueAssetComponent implements OnInit, OnDestroy {

  // ── Asset info from query params ──────────────────────────────────────────
  assetId       = 'AST-008';
  assetName     = 'MacBook Pro';
  assetTag      = 'ITCN0038480006';
  assetModel    = 'Macbook Pro 13"';
  assetCategory = 'I.T';

  // ── Form fields ───────────────────────────────────────────────────────────
  model              = '';
  assetNameInput     = '';
  status             = '';
  issueTo            = 'User';
  selectedUser       = '';
  issueDate          = '';
  expectedReturn     = '';
  expectedReturnTime = '';
  notes              = '';

  // ── Dropdown options ──────────────────────────────────────────────────────
  statuses = ['Ready to Deploy', 'Deployed', 'Under Maintenance', 'Retired'];
  users    = ['Amal Martin', 'Kurian George', 'Melbin Joseph', 'Mariyan', 'Joyal Saji', 'Dewang', 'Riya Thomas'];

  // ── UI state ──────────────────────────────────────────────────────────────
  statusOpen   = false;
  userOpen     = false;
  showSuccess  = false;
  submitted    = false;
  assetsDropdownOpen = false;
  sidebarOpen = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['assetId'])       this.assetId       = params['assetId'];
      if (params['assetName'])     this.assetName     = params['assetName'];
      if (params['assetTag'])      this.assetTag      = params['assetTag'];
      if (params['assetModel'])    this.assetModel    = params['assetModel'];
      if (params['assetCategory']) this.assetCategory = params['assetCategory'];
    });
  }

  ngOnDestroy(): void {}

  @HostListener('document:click')
  onDocumentClick(): void { this.sidebarOpen = false; }

  goBack(): void { this.router.navigate(['/assets/view']); }
  goToDashboard(): void { this.router.navigate(['/']); }
  goToIssueLog(): void { this.router.navigate(['/assets/issue-log']); }
  goToReturnLog(): void { this.router.navigate(['/assets/return-log']); }
  goToReports(): void { this.router.navigate(['/assets/reports']); }

  onSubmit(): void {
    this.submitted = true;
    this.showSuccess = true;
  }

  issueAnother(): void {
    this.showSuccess       = false;
    this.submitted         = false;
    this.model             = '';
    this.assetNameInput    = '';
    this.status            = '';
    this.selectedUser      = '';
    this.issueDate         = '';
    this.expectedReturn    = '';
    this.expectedReturnTime = '';
    this.notes             = '';
  }
}
