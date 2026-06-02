import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { AssetService } from '../../services/asset.service';

export interface ReturnRecord {
  assetName:      string;
  assetTag:       string;
  classification: string;
  total:          number;
  returnType:     string;
  returnTo:       string;
  returnDate:     string;
  department:     string;
}

const PAGE_SIZE = 8;

@Component({
  selector: 'app-return-log',
  templateUrl: './return-log.component.html',
  styleUrls: ['./return-log.component.scss'],
})
export class ReturnLogComponent implements OnInit, OnDestroy {

  assetsDropdownOpen = false;
  sidebarOpen        = false;
  isLoading          = true;
  apiError: string | null = null;

  // ── Filter ───────────────────────────────────────────────────────────────────
  filterOpen = false;
  filterClassifications: string[] = [];
  readonly classificationFilterOptions = ['Asset', 'Component', 'Consumable', 'Accessory'];

  // ── View ─────────────────────────────────────────────────────────────────────
  selectedRecord: ReturnRecord | null = null;

  // ── Department tabs (built from API data) ────────────────────────────────────
  departments: string[] = [];
  activeDept  = '';

  searchQuery = '';
  currentPage = 1;

  // ── Form (detail panel) ───────────────────────────────────────────────────────
  formAssetName      = '';
  formAssetTag       = '';
  formClassification = '';
  formTotal: number | null = null;
  formReturnType     = '';
  formReturnTo       = '';
  formReturnDate     = '';

  classificationOptions = ['Asset', 'Component', 'Consumable', 'Accessory'];
  returnTypeOptions     = ['Permanent Return', 'Temporary Return', 'Damaged Return', 'Lost Report'];
  returnToOptions       = ['IT Department', 'Admin Office', 'Library', 'Lab Store', 'Principal Office'];

  // ── Data ─────────────────────────────────────────────────────────────────────
  allRecords: ReturnRecord[] = [];

  // ── Computed ─────────────────────────────────────────────────────────────────
  get filteredRecords(): ReturnRecord[] {
    const q = this.searchQuery.toLowerCase();
    return this.allRecords.filter(r => {
      const matchesDept  = !this.activeDept || r.department === this.activeDept;
      const matchSearch  = !q || r.assetName.toLowerCase().includes(q)
                              || r.assetTag.toLowerCase().includes(q)
                              || r.returnTo.toLowerCase().includes(q);
      const matchClass   = this.filterClassifications.length === 0
                        || this.filterClassifications.includes(r.classification);
      return matchesDept && matchSearch && matchClass;
    });
  }

  get totalPages(): number { return Math.max(1, Math.ceil(this.filteredRecords.length / PAGE_SIZE)); }

  get pagedRecords(): ReturnRecord[] {
    const start = (this.currentPage - 1) * PAGE_SIZE;
    return this.filteredRecords.slice(start, start + PAGE_SIZE);
  }

  get visiblePages(): number[] {
    const total = this.totalPages;
    const cur   = this.currentPage;
    if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
    const pages: number[] = [1, 2, 3];
    if (cur > 4) pages.push(-1);
    if (cur > 3 && cur < total - 2) pages.push(cur);
    pages.push(-1);
    pages.push(total - 2, total - 1, total);
    return [...new Set(pages)].filter((p, idx, arr) => {
      if (p === -1) return arr[idx - 1] !== -1;
      return true;
    });
  }

  constructor(private router: Router, private assetService: AssetService) {}

  ngOnInit(): void  { this.loadReturnLogs(); }
  ngOnDestroy(): void {}

  private loadReturnLogs(): void {
    this.isLoading = true;
    this.apiError  = null;

    this.assetService.getReturnLogs().subscribe({
      next: (response: any) => {
        // Shape: { statusCode, type, responseData: { data: { assets: [] } } }
        const raw: any[] = response?.responseData?.data?.assets ?? [];

        this.allRecords = raw.map(item => ({
          assetName:      item.assetName  ?? '—',
          assetTag:       item.assetName  ?? '—',   // no tag in response, use name as identifier
          classification: 'Asset',                   // API doesn't return this field, default to Asset
          total:          item.total      ?? 0,
          returnType:     item.returnType ?? '—',
          returnTo:       item.issuedFor  ?? '—',
          returnDate:     item.returnDate
                            ? new Date(item.returnDate).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
                            : '—',
          department:     item.category  ?? 'Other'
        }));

        // Build dept tabs dynamically from categories in data
        this.departments = [...new Set(this.allRecords.map(r => r.department))];
        this.activeDept  = this.departments[0] ?? '';
        this.isLoading   = false;
      },
      error: (err) => {
        console.error('Failed to load return logs:', err);
        this.apiError = 'Could not load return log data from the server.';
        this.isLoading = false;
      }
    });
  }

  @HostListener('document:click')
  onDocumentClick(): void { this.sidebarOpen = false; this.filterOpen = false; }

  // ── Filter ───────────────────────────────────────────────────────────────────
  toggleFilterPanel(event: Event): void { event.stopPropagation(); this.filterOpen = !this.filterOpen; }

  toggleClassFilter(c: string): void {
    const i = this.filterClassifications.indexOf(c);
    if (i === -1) this.filterClassifications.push(c);
    else this.filterClassifications.splice(i, 1);
    this.currentPage = 1;
  }

  clearFilters(): void { this.filterClassifications = []; this.currentPage = 1; }

  // ── Export ───────────────────────────────────────────────────────────────────
  exportCSV(): void {
    const headers = ['Name', 'Category', 'Total', 'Return Type', 'Returned To', 'Return Date'];
    const csv = [
      headers.join(','),
      ...this.filteredRecords.map(r =>
        [r.assetName, r.department, r.total, r.returnType, r.returnTo, r.returnDate].join(',')
      )
    ].join('\n');
    this.downloadCSV(csv, `return-log-${this.activeDept}.csv`);
  }

  private downloadCSV(content: string, filename: string): void {
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    URL.revokeObjectURL(url);
  }

  // ── Navigation ───────────────────────────────────────────────────────────────
  setDept(dept: string): void {
    this.activeDept     = dept;
    this.searchQuery    = '';
    this.currentPage    = 1;
    this.selectedRecord = null;
  }

  prevPage(): void { if (this.currentPage > 1) this.currentPage--; }
  nextPage(): void { if (this.currentPage < this.totalPages) this.currentPage++; }
  goToPage(p: number): void { this.currentPage = p; }

  goToDashboard(): void  { this.router.navigate(['/']); }
  goToViewAssets(): void { this.router.navigate(['/assets/view']); }
  goToIssueAsset(): void { this.router.navigate(['/assets/issue']); }
  goToReports(): void    { this.router.navigate(['/assets/reports']); }

  // ── Row click ─────────────────────────────────────────────────────────────────
  openDetail(row: ReturnRecord): void {
    this.selectedRecord     = row;
    this.formAssetName      = row.assetName;
    this.formAssetTag       = row.assetTag;
    this.formClassification = row.classification;
    this.formTotal          = row.total;
    this.formReturnType     = row.returnType;
    this.formReturnTo       = row.returnTo;
    this.formReturnDate     = row.returnDate;
  }

  closeDetail(): void { this.selectedRecord = null; }

  onSubmit(): void {
    if (!this.selectedRecord) return;
    this.selectedRecord.returnType = this.formReturnType;
    this.selectedRecord.returnTo   = this.formReturnTo;
    this.selectedRecord.returnDate = this.formReturnDate;
    this.closeDetail();
  }

  getClassClass(c: string): string {
    const map: Record<string, string> = {
      'Asset': 'class-asset', 'Component': 'class-component',
      'Consumable': 'class-consumable', 'Accessory': 'class-accessory'
    };
    return map[c] ?? 'class-asset';
  }
}
