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
  searchQuery        = '';

  // ── Filter ───────────────────────────────────────────────────────────────────
  filterOpen = false;
  filterClassifications: string[] = [];
  readonly classificationFilterOptions = ['Asset', 'Component', 'Consumable', 'Accessory'];

  // ── View ─────────────────────────────────────────────────────────────────────
  selectedRecord: ReturnRecord | null = null;

  // ── Department tabs ───────────────────────────────────────────────────────────
  departments: string[] = [];
  activeDept  = '';

  // ── Server-side pagination ────────────────────────────────────────────────────
  currentPage  = 1;
  totalPagesVal = 1;   // backing value — avoid getter/property clash
  totalRecords = 0;
  pageSize     = 10;

  get totalPages(): number { return this.totalPagesVal; }

  get visiblePages(): number[] {
    const pages: number[] = [];
    for (let i = 1; i <= this.totalPagesVal; i++) {
      if (i === 1 || i === this.totalPagesVal || Math.abs(i - this.currentPage) <= 1) pages.push(i);
      else if (pages[pages.length - 1] !== -1) pages.push(-1);
    }
    return pages;
  }

  // ── Form ─────────────────────────────────────────────────────────────────────
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

  get pagedRecords(): ReturnRecord[] {
    const q = this.searchQuery.toLowerCase();
    return this.allRecords.filter(r => {
      const matchesDept = !this.activeDept || r.department === this.activeDept;
      const matchSearch = !q || r.assetName.toLowerCase().includes(q) || r.returnTo.toLowerCase().includes(q);
      const matchClass  = this.filterClassifications.length === 0 || this.filterClassifications.includes(r.classification);
      return matchesDept && matchSearch && matchClass;
    });
  }

  // keep filteredRecords alias for CSV export
  get filteredRecords(): ReturnRecord[] { return this.pagedRecords; }

  constructor(private router: Router, private assetService: AssetService) {}

  ngOnInit(): void { this.loadReturnLogs(); }
  ngOnDestroy(): void {}

  private loadReturnLogs(): void {
    this.isLoading = true;
    this.apiError  = null;

    this.assetService.getReturnLogs(this.currentPage, this.pageSize).subscribe({
      next: (response: any) => {
        const data = response?.responseData?.data ?? {};
        // return-logs uses data.data instead of data.assets
        const raw: any[] = data.data ?? data.assets ?? [];

        this.totalRecords  = data.totalRecords ?? raw.length;
        this.totalPagesVal = data.totalPages   ?? 1;
        this.currentPage   = data.currentPage  ?? this.currentPage;

        this.allRecords = raw.map(item => ({
          assetName:      item.assetName  ?? '—',
          assetTag:       item.assetName  ?? '—',
          classification: 'Asset' as const,
          total:          item.total      ?? 0,
          returnType:     item.returnType ?? '—',
          returnTo:       item.issuedFor  ?? '—',
          returnDate:     item.returnDate
            ? new Date(item.returnDate).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
            : '—',
          department: item.category ?? 'Other'
        }));

        this.departments = [...new Set(this.allRecords.map(r => r.department))];
        if (!this.activeDept || !this.departments.includes(this.activeDept)) {
          this.activeDept = this.departments[0] ?? '';
        }
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Failed to load return logs:', err);
        this.apiError = 'Could not load return log data from the server.';
        this.isLoading = false;
      }
    });
  }

  @HostListener('document:click')
  onDocumentClick(): void { this.sidebarOpen = false; this.filterOpen = false; }

  toggleFilterPanel(event: Event): void { event.stopPropagation(); this.filterOpen = !this.filterOpen; }

  toggleClassFilter(c: string): void {
    const i = this.filterClassifications.indexOf(c);
    if (i === -1) this.filterClassifications.push(c); else this.filterClassifications.splice(i, 1);
    this.currentPage = 1;
  }

  clearFilters(): void { this.filterClassifications = []; this.currentPage = 1; }

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

  setDept(dept: string): void {
    this.activeDept     = dept;
    this.searchQuery    = '';
    this.currentPage    = 1;
    this.selectedRecord = null;
  }

  prevPage(): void { if (this.currentPage > 1) { this.currentPage--; this.loadReturnLogs(); } }
  nextPage(): void { if (this.currentPage < this.totalPagesVal) { this.currentPage++; this.loadReturnLogs(); } }
  goToPage(p: number): void { if (p !== this.currentPage) { this.currentPage = p; this.loadReturnLogs(); } }

  goToDashboard(): void  { this.router.navigate(['/']); }
  goToViewAssets(): void { this.router.navigate(['/assets/view']); }
  goToIssueAsset(): void { this.router.navigate(['/assets/issue']); }
  goToReports(): void    { this.router.navigate(['/assets/reports']); }

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
