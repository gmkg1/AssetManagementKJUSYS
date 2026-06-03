import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { AssetService } from '../../services/asset.service';

export interface ReportAsset {
  id: string;
  name: string;
  category: string;
  type: 'Asset';
  total: number;
  readyToDeploy: number;
  deployed: number;
  deadStock: number;
  underService: number;
  damaged: number;
  checked: boolean;
}

const PAGE_SIZE = 10;

@Component({
  selector: 'app-reports',
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss']
})
export class ReportsComponent implements OnInit, OnDestroy {

  // ── state ────────────────────────────────────────────────────────────────────
  departments: string[] = [];
  // Fixed full category list — always available regardless of current page
  readonly allCategories = ['IT', 'Electrical', 'Sound', 'Stationery', 'Housekeeping', 'Furniture'];
  activeDept  = '';
  searchQuery = '';
  currentPage = 1;
  allChecked  = false;
  sidebarOpen = false;
  isLoading   = true;
  apiError: string | null = null;

  // ── filter ───────────────────────────────────────────────────────────────────
  filterOpen   = false;
  filterTypes: string[] = [];
  readonly typeFilterOptions = ['Asset', 'Component', 'Consumable', 'Accessory'];

  toggleTypeFilter(t: string): void {
    const i = this.filterTypes.indexOf(t);
    if (i === -1) this.filterTypes.push(t); else this.filterTypes.splice(i, 1);
    this.currentPage = 1;
  }

  // ── data — ALL loaded upfront, paginated client-side per dept ─────────────────
  private allAssets: Record<string, ReportAsset[]> = {};
  private allPagesLoaded = false;

  // ── computed ─────────────────────────────────────────────────────────────────
  get assets(): ReportAsset[] {
    const list = this.allAssets[this.activeDept] ?? [];
    const q = this.searchQuery.toLowerCase().trim();
    return list.filter(a => !q || a.name.toLowerCase().includes(q));
  }

  get pagedAssets(): ReportAsset[] {
    const start = (this.currentPage - 1) * PAGE_SIZE;
    return this.assets.slice(start, start + PAGE_SIZE);
  }

  get totalPages(): number { return Math.max(1, Math.ceil(this.assets.length / PAGE_SIZE)); }
  get pageNumbers(): number[] {
    const pages: number[] = [];
    for (let i = 1; i <= this.totalPages; i++) {
      if (i === 1 || i === this.totalPages || Math.abs(i - this.currentPage) <= 1) pages.push(i);
      else if (pages[pages.length - 1] !== -1) pages.push(-1);
    }
    return pages;
  }
  get selectedCount(): number { return this.pagedAssets.filter(a => a.checked).length; }

  // summary totals for active dept
  get deptTotal():        number { return this.assets.reduce((s, a) => s + a.total, 0); }
  get deptReady():        number { return this.assets.reduce((s, a) => s + a.readyToDeploy, 0); }
  get deptDeployed():     number { return this.assets.reduce((s, a) => s + a.deployed, 0); }
  get deptDeadStock():    number { return this.assets.reduce((s, a) => s + a.deadStock, 0); }
  get deptUnderService(): number { return this.assets.reduce((s, a) => s + a.underService, 0); }
  get deptDamaged():      number { return this.assets.reduce((s, a) => s + a.damaged, 0); }

  constructor(private router: Router, private assetService: AssetService) {}

  ngOnInit(): void {
    this.loadAllReportData();
  }

  ngOnDestroy(): void {}

  /** Load ALL pages from the server once, then paginate client-side per dept tab */
  private loadAllReportData(): void {
    this.isLoading = true;
    this.apiError  = null;
    this.allAssets = {};
    this.allPagesLoaded = false;

    // First call to get totalPages
    this.assetService.getAssetStatusSummary(1, PAGE_SIZE).subscribe({
      next: (response: any) => {
        const data       = response?.responseData?.data ?? {};
        const firstBatch = data.assets ?? [];
        const totalPages = data.totalPages ?? 1;

        this.mergeIntoAllAssets(firstBatch);

        if (totalPages <= 1) {
          this.finaliseData();
          return;
        }

        // Fetch remaining pages in parallel
        const remaining = Array.from({ length: totalPages - 1 }, (_, i) =>
          this.assetService.getAssetStatusSummary(i + 2, PAGE_SIZE)
        );

        let completed = 0;
        remaining.forEach(obs => {
          obs.subscribe({
            next: (r: any) => {
              this.mergeIntoAllAssets(r?.responseData?.data?.assets ?? []);
              completed++;
              if (completed === remaining.length) this.finaliseData();
            },
            error: () => { completed++; if (completed === remaining.length) this.finaliseData(); }
          });
        });
      },
      error: (err: any) => {
        console.error('Failed to load asset status summary:', err);
        this.apiError = 'Could not load report data from the server.';
        this.isLoading = false;
      }
    });
  }

  private mergeIntoAllAssets(raw: any[]): void {
    raw.forEach((item, index) => {
      const cat = item.category ?? 'Other';
      if (!this.allAssets[cat]) this.allAssets[cat] = [];
      this.allAssets[cat].push({
        id:            item.assetTagName ?? `TAG-${index + 1}`,
        name:          item.assetTagName ?? '—',
        category:      cat,
        type:          'Asset',
        total:         item.totalAssets      ?? 0,
        readyToDeploy: item.ready            ?? 0,
        deployed:      item.deployed         ?? 0,
        deadStock:     item.deadStock        ?? 0,
        underService:  item.underMaintenance ?? 0,
        damaged:       item.damaged          ?? 0,
        checked:       false
      });
    });
  }

  private finaliseData(): void {
    // Build dept tabs from actual data, merged with known categories
    const fromData = Object.keys(this.allAssets);
    // Show all known categories as tabs; empty ones will show 0 rows
    this.departments = [...new Set([...this.allCategories, ...fromData])];
    if (!this.activeDept || !this.departments.includes(this.activeDept)) {
      this.activeDept = this.departments[0] ?? '';
    }
    this.allPagesLoaded = true;
    this.isLoading = false;
  }

  @HostListener('document:click')
  onDocumentClick(): void { this.sidebarOpen = false; this.filterOpen = false; }

  selectDept(dept: string): void {
    this.activeDept  = dept;
    this.currentPage = 1;
    this.allChecked  = false;
    this.searchQuery = '';
  }

  toggleFilterPanel(event: Event): void { event.stopPropagation(); this.filterOpen = !this.filterOpen; }

  clearFilters(): void { this.filterTypes = []; this.currentPage = 1; }

  exportCSV(): void {
    const headers = ['Asset Tag', 'Category', 'Total', 'Ready to Deploy', 'Deployed', 'Dead Stock', 'Under Service', 'Damaged'];
    const csv = [
      headers.join(','),
      ...this.assets.map(a => [a.name, a.category, a.total, a.readyToDeploy, a.deployed, a.deadStock, a.underService, a.damaged].join(','))
    ].join('\n');
    this.downloadCSV(csv, `report-${this.activeDept}.csv`);
  }

  private downloadCSV(content: string, filename: string): void {
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    URL.revokeObjectURL(url);
  }

  toggleAll(): void {
    this.allChecked = !this.allChecked;
    this.pagedAssets.forEach(a => a.checked = this.allChecked);
  }

  goToPage(p: number): void { if (p !== this.currentPage) { this.currentPage = p; } }
  prevPage(): void { if (this.currentPage > 1) { this.currentPage--; } }
  nextPage(): void { if (this.currentPage < this.totalPages) { this.currentPage++; } }

  bulkExport(): void {
    const rows = this.pagedAssets.filter(a => a.checked);
    const headers = ['Asset Tag', 'Category', 'Total', 'Ready to Deploy', 'Deployed', 'Dead Stock', 'Under Service', 'Damaged'];
    const csv = [headers.join(','), ...rows.map(a => [a.name, a.category, a.total, a.readyToDeploy, a.deployed, a.deadStock, a.underService, a.damaged].join(','))].join('\n');
    this.downloadCSV(csv, `report-${this.activeDept}-selected.csv`);
  }

  bulkDelete(): void {
    if (confirm(`Delete ${this.selectedCount} item(s)?`)) {
      this.allAssets[this.activeDept] = this.allAssets[this.activeDept].filter(a => !a.checked);
      this.allChecked = false;
    }
  }

  clearSelection(): void { this.pagedAssets.forEach(a => a.checked = false); this.allChecked = false; }

  getTypeClass(type: string): string {
    const map: Record<string, string> = { 'Asset': 'class-asset', 'Component': 'class-component', 'Consumable': 'class-consumable', 'Accessory': 'class-accessory' };
    return map[type] ?? '';
  }

  goToDashboard():  void { this.router.navigate(['/']); }
  goToViewAssets(): void { this.router.navigate(['/assets/view']); }
  goToIssueAsset(): void { this.router.navigate(['/assets/issue']); }
  goToReturnLog():  void { this.router.navigate(['/assets/return-log']); }
}
