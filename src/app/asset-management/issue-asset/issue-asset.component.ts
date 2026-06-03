import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { AssetService } from '../../services/asset.service';

export interface IssueRecord {
  assetName:    string;
  assetCategory: string;
  issueDate:    string;
  receiverName: string;
  receiverType: string;
}

const PAGE_SIZE = 10;

@Component({
  selector: 'app-issue-asset',
  templateUrl: './issue-asset.component.html',
  styleUrls: ['./issue-asset.component.scss'],
})
export class IssueAssetComponent implements OnInit, OnDestroy {

  // ── Asset info from query params ─────────────────────────────────────────
  assetId       = '';
  assetName     = '';
  assetTag      = '';
  assetModel    = '';
  assetCategory = '';

  // ── Form fields ──────────────────────────────────────────────────────────
  model              = '';
  assetNameInput     = '';
  status             = '';
  issueTo            = 'User';
  selectedUser       = '';
  issueDate          = '';
  expectedReturn     = '';
  expectedReturnTime = '';
  notes              = '';

  // ── Dropdown options ─────────────────────────────────────────────────────
  statuses = ['Ready to Deploy', 'Deployed', 'Under Maintenance', 'Retired'];
  users    = ['Amal Martin', 'Kurian George', 'Melbin Joseph', 'Mariyan', 'Joyal Saji', 'Dewang', 'Riya Thomas'];

  // ── UI state ─────────────────────────────────────────────────────────────
  statusOpen   = false;
  userOpen     = false;
  showSuccess  = false;
  submitted    = false;
  sidebarOpen  = false;

  // ── Issue log ────────────────────────────────────────────────────────────
  issueLog:      IssueRecord[] = [];
  isLoadingLog   = true;
  logError:      string | null = null;
  searchQuery    = '';
  currentPage    = 1;
  totalPages     = 1;
  totalRecords   = 0;

  get filteredLog(): IssueRecord[] {
    if (!this.searchQuery.trim()) return this.issueLog;
    const q = this.searchQuery.toLowerCase();
    return this.issueLog.filter(r =>
      r.assetName.toLowerCase().includes(q)
      || r.assetCategory.toLowerCase().includes(q)
      || r.receiverName.toLowerCase().includes(q)
      || r.receiverType.toLowerCase().includes(q)
    );
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    for (let i = 1; i <= this.totalPages; i++) {
      if (i === 1 || i === this.totalPages || Math.abs(i - this.currentPage) <= 1) pages.push(i);
      else if (pages[pages.length - 1] !== -1) pages.push(-1);
    }
    return pages;
  }

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private assetService: AssetService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params: Record<string, string>) => {
      if (params['assetId'])       this.assetId       = params['assetId'];
      if (params['assetName'])     this.assetName     = params['assetName'];
      if (params['assetTag'])      this.assetTag      = params['assetTag'];
      if (params['assetModel'])    this.assetModel    = params['assetModel'];
      if (params['assetCategory']) this.assetCategory = params['assetCategory'];
    });

    this.loadIssueLog();
  }

  ngOnDestroy(): void {}

  private loadIssueLog(): void {
    this.isLoadingLog = true;
    this.logError     = null;

    this.assetService.getIssuedAssets(this.currentPage, PAGE_SIZE).subscribe({
      next: (response: any) => {
        const data = response?.responseData?.data ?? {};
        const raw: any[] = data.assets ?? [];

        this.totalRecords = data.totalRecords ?? raw.length;
        this.totalPages   = data.totalPages   ?? 1;
        this.currentPage  = data.currentPage  ?? this.currentPage;

        this.issueLog = raw.map(item => ({
          assetName:    item.assetName     ?? '—',
          assetCategory: item.assetCategory ?? '—',
          issueDate:    item.issueDate
            ? new Date(item.issueDate).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
            : '—',
          receiverName: item.receiverName  ?? '—',
          receiverType: item.receiverType  ?? '—',
        }));

        this.isLoadingLog = false;
      },
      error: (err: any) => {
        console.error('Failed to load issue log:', err);
        this.logError = 'Could not load issue log from server.';
        this.isLoadingLog = false;
      }
    });
  }

  prevPage(): void { if (this.currentPage > 1) { this.currentPage--; this.loadIssueLog(); } }
  nextPage(): void { if (this.currentPage < this.totalPages) { this.currentPage++; this.loadIssueLog(); } }
  goToPage(p: number): void { if (p !== this.currentPage) { this.currentPage = p; this.loadIssueLog(); } }

  getReceiverTypeClass(type: string): string {
    const map: Record<string, string> = {
      'Location': 'bg-blue-50 text-blue-700',
      'Asset':    'bg-purple-50 text-purple-700',
      'Person':   'bg-green-50 text-green-700',
    };
    return map[type] ?? 'bg-slate-50 text-slate-600';
  }

  @HostListener('document:click')
  onDocumentClick(): void { this.sidebarOpen = false; this.statusOpen = false; this.userOpen = false; }

  goBack(): void         { this.router.navigate(['/assets/view']); }
  goToDashboard(): void  { this.router.navigate(['/']); }
  goToViewAssets(): void { this.router.navigate(['/assets/view']); }
  goToIssueLog(): void   { this.router.navigate(['/assets/issue-log']); }
  goToReturnLog(): void  { this.router.navigate(['/assets/return-log']); }
  goToReports(): void    { this.router.navigate(['/assets/reports']); }

  onSubmit(): void {
    this.submitted   = true;
    this.showSuccess = true;
  }

  issueAnother(): void {
    this.showSuccess        = false;
    this.submitted          = false;
    this.model              = '';
    this.assetNameInput     = '';
    this.status             = '';
    this.selectedUser       = '';
    this.issueDate          = '';
    this.expectedReturn     = '';
    this.expectedReturnTime = '';
    this.notes              = '';
  }
}
