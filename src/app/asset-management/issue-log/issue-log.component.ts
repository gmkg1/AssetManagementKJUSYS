import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

// ── Generic tab item (IT, Sound, Stationery, Housekeeping, Furnitures) ────────
export interface IssueLogItem {
  id: string;
  name: string;
  assetId: string;
  imageUrl: string;
  classification: 'Asset' | 'Component' | 'Consumable' | 'Accessory';
  total: number;
  issueType: string;
  issuedTo: string;
  issuedDate: string;
  selected: boolean;
}

// ── Electrical tab item ───────────────────────────────────────────────────────
export interface ElectricalIssueItem {
  id: string;
  assetName: string;
  assetTag: string;
  issuedTo: string;
  department: string;
  dateIssued: string;
  dueDate: string;
  status: 'Active' | 'Overdue' | 'Returned';
  condition: 'Good' | 'Fair' | 'Poor';
  selected: boolean;
}

// ── Generic tab data ──────────────────────────────────────────────────────────
const TAB_DATA: Record<string, IssueLogItem[]> = {
  IT: [
    { id: '1', name: 'Laptops',     assetId: '1210073015', imageUrl: 'assets/images/laptop.png',    classification: 'Asset',      total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '2', name: 'Desktops',    assetId: '1210073015', imageUrl: 'assets/images/desktop.png',   classification: 'Asset',      total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '3', name: 'VOIP Phones', assetId: '1210073015', imageUrl: 'assets/images/voip.png',      classification: 'Component',  total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '4', name: 'Displays',    assetId: '1210073015', imageUrl: 'assets/images/display.png',   classification: 'Consumable', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '5', name: 'Tablets',     assetId: '1210073015', imageUrl: 'assets/images/tablet.png',    classification: 'Accessory',  total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '6', name: 'CCTV',        assetId: '1210073015', imageUrl: 'assets/images/cctv.png',      classification: 'Consumable', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '7', name: 'Keyboards',   assetId: '1210073015', imageUrl: 'assets/images/keyboard.png',  classification: 'Consumable', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '8', name: 'Mouse',       assetId: '1210073015', imageUrl: 'assets/images/mouse.png',     classification: 'Consumable', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
  ],
  Sound: [
    { id: '1', name: 'Speakers',    assetId: '1210073015', imageUrl: '', classification: 'Asset',      total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '2', name: 'Microphones', assetId: '1210073015', imageUrl: '', classification: 'Asset',      total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '3', name: 'Amplifiers',  assetId: '1210073015', imageUrl: '', classification: 'Asset',      total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '4', name: 'Mixers',      assetId: '1210073015', imageUrl: '', classification: 'Component',  total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '5', name: 'Cables',      assetId: '1210073015', imageUrl: '', classification: 'Consumable', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
  ],
  Stationery: [
    { id: '1', name: 'Pens',        assetId: '1210073015', imageUrl: '', classification: 'Consumable', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '2', name: 'Notebooks',   assetId: '1210073015', imageUrl: '', classification: 'Consumable', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '3', name: 'Staplers',    assetId: '1210073015', imageUrl: '', classification: 'Asset',      total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '4', name: 'Markers',     assetId: '1210073015', imageUrl: '', classification: 'Consumable', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '5', name: 'Files',       assetId: '1210073015', imageUrl: '', classification: 'Consumable', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
  ],
  Housekeeping: [
    { id: '1', name: 'Mops',           assetId: '1210073015', imageUrl: '', classification: 'Consumable', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '2', name: 'Brooms',         assetId: '1210073015', imageUrl: '', classification: 'Consumable', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '3', name: 'Vacuum Cleaner', assetId: '1210073015', imageUrl: '', classification: 'Asset',      total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '4', name: 'Dustbins',       assetId: '1210073015', imageUrl: '', classification: 'Asset',      total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
  ],
  Furnitures: [
    { id: '1', name: 'Chairs',   assetId: '1210073015', imageUrl: '', classification: 'Asset', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '2', name: 'Tables',   assetId: '1210073015', imageUrl: '', classification: 'Asset', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '3', name: 'Shelves',  assetId: '1210073015', imageUrl: '', classification: 'Asset', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '4', name: 'Cabinets', assetId: '1210073015', imageUrl: '', classification: 'Asset', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
    { id: '5', name: 'Sofas',    assetId: '1210073015', imageUrl: '', classification: 'Asset', total: 320, issueType: '', issuedTo: '', issuedDate: '', selected: false },
  ],
};

// ── Electrical tab data ───────────────────────────────────────────────────────
const ELECTRICAL_DATA: ElectricalIssueItem[] = [
  {
    id: '1', assetName: 'Projector Epson EB-X41', assetTag: 'EL-1001',
    issuedTo: 'Alice Johnson', department: 'Science Dept.',
    dateIssued: 'Jan 10, 2025', dueDate: 'Feb 10, 2025',
    status: 'Active', condition: 'Good', selected: false,
  },
  {
    id: '2', assetName: 'Extension Cord 10m', assetTag: 'EL-1002',
    issuedTo: 'Brian Mensah', department: 'Admin',
    dateIssued: 'Dec 5, 2024', dueDate: 'Jan 5, 2025',
    status: 'Overdue', condition: 'Fair', selected: false,
  },
  {
    id: '3', assetName: 'UPS APC 1500VA', assetTag: 'EL-1003',
    issuedTo: 'Clara Owusu', department: 'I.T Dept.',
    dateIssued: 'Nov 20, 2024', dueDate: 'Dec 20, 2024',
    status: 'Returned', condition: 'Good', selected: false,
  },
  {
    id: '4', assetName: 'LED Desk Lamp', assetTag: 'EL-1004',
    issuedTo: 'David Asante', department: 'Library',
    dateIssued: 'Jan 15, 2025', dueDate: 'Mar 15, 2025',
    status: 'Active', condition: 'Good', selected: false,
  },
  {
    id: '5', assetName: 'Electric Kettle', assetTag: 'EL-1005',
    issuedTo: 'Evelyn Tetteh', department: 'Cafeteria',
    dateIssued: 'Oct 1, 2024', dueDate: 'Nov 1, 2024',
    status: 'Overdue', condition: 'Poor', selected: false,
  },
  {
    id: '6', assetName: 'Air Conditioner 1.5HP', assetTag: 'EL-1006',
    issuedTo: 'Frank Boateng', department: 'Principal Office',
    dateIssued: 'Jan 3, 2025', dueDate: 'Jul 3, 2025',
    status: 'Active', condition: 'Good', selected: false,
  },
  {
    id: '7', assetName: 'Ceiling Fan 56"', assetTag: 'EL-1007',
    issuedTo: 'Grace Acheampong', department: 'Classroom B2',
    dateIssued: 'Sep 12, 2024', dueDate: 'Dec 12, 2024',
    status: 'Returned', condition: 'Fair', selected: false,
  },
  {
    id: '8', assetName: 'Microwave Oven', assetTag: 'EL-1008',
    issuedTo: 'Henry Darko', department: 'Staff Room',
    dateIssued: 'Jan 20, 2025', dueDate: 'Apr 20, 2025',
    status: 'Active', condition: 'Good', selected: false,
  },
  {
    id: '9', assetName: 'Electric Iron', assetTag: 'EL-1009',
    issuedTo: 'Irene Quaye', department: 'Housekeeping',
    dateIssued: 'Dec 18, 2024', dueDate: 'Jan 18, 2025',
    status: 'Overdue', condition: 'Fair', selected: false,
  },
  {
    id: '10', assetName: 'Smart TV 55"', assetTag: 'EL-1010',
    issuedTo: 'James Ofori', department: 'Conference Room',
    dateIssued: 'Jan 8, 2025', dueDate: 'Jun 8, 2025',
    status: 'Active', condition: 'Good', selected: false,
  },
];

@Component({
  selector: 'app-issue-log',
  templateUrl: './issue-log.component.html',
  styleUrls: ['./issue-log.component.scss'],
})
export class IssueLogComponent implements OnInit {
  activeTab: string = 'IT';
  tabs = ['IT', 'Electrical', 'Sound', 'Stationery', 'Housekeeping', 'Furnitures'];

  searchQuery: string = '';
  selectAll: boolean = false;

  currentPage: number = 1;
  pageSize: number = 8;

  /** Asset context passed from the Assets page via query params */
  incomingAssetName: string = '';
  incomingAssetId: string = '';

  constructor(private route: ActivatedRoute, public router: Router) {}

  // ── Generic tab getters ───────────────────────────────────────────────────
  get allItems(): IssueLogItem[] {
    return TAB_DATA[this.activeTab] ?? [];
  }

  get filteredItems(): IssueLogItem[] {
    if (!this.searchQuery.trim()) return this.allItems;
    const q = this.searchQuery.toLowerCase();
    return this.allItems.filter(
      (item) =>
        item.name.toLowerCase().includes(q) ||
        item.assetId.toLowerCase().includes(q) ||
        item.classification.toLowerCase().includes(q)
    );
  }

  get pagedItems(): IssueLogItem[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredItems.slice(start, start + this.pageSize);
  }

  // ── Electrical tab getters ────────────────────────────────────────────────
  get allElectricalItems(): ElectricalIssueItem[] {
    return ELECTRICAL_DATA;
  }

  get filteredElectricalItems(): ElectricalIssueItem[] {
    if (!this.searchQuery.trim()) return this.allElectricalItems;
    const q = this.searchQuery.toLowerCase();
    return this.allElectricalItems.filter(
      (i) =>
        i.assetName.toLowerCase().includes(q) ||
        i.assetTag.toLowerCase().includes(q) ||
        i.issuedTo.toLowerCase().includes(q) ||
        i.department.toLowerCase().includes(q)
    );
  }

  get pagedElectricalItems(): ElectricalIssueItem[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredElectricalItems.slice(start, start + this.pageSize);
  }

  // ── Shared pagination ─────────────────────────────────────────────────────
  get totalPages(): number {
    const total =
      this.activeTab === 'Electrical'
        ? this.filteredElectricalItems.length
        : this.filteredItems.length;
    return Math.max(1, Math.ceil(total / this.pageSize));
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['category']) {
        const match = this.tabs.find(
          (t) => t.toLowerCase() === params['category'].toLowerCase()
        );
        if (match) this.activeTab = match;
      }
      if (params['assetName']) {
        this.incomingAssetName = params['assetName'];
        this.searchQuery = params['assetName'];
      }
      if (params['assetId']) {
        this.incomingAssetId = params['assetId'];
      }
    });
  }

  setTab(tab: string): void {
    this.activeTab = tab;
    this.currentPage = 1;
    this.selectAll = false;
    this.searchQuery = '';
  }

  // ── Select all ────────────────────────────────────────────────────────────
  toggleSelectAll(): void {
    this.selectAll = !this.selectAll;
    if (this.activeTab === 'Electrical') {
      this.pagedElectricalItems.forEach((item) => (item.selected = this.selectAll));
    } else {
      this.pagedItems.forEach((item) => (item.selected = this.selectAll));
    }
  }

  toggleSelect(item: IssueLogItem | ElectricalIssueItem): void {
    item.selected = !item.selected;
    if (this.activeTab === 'Electrical') {
      this.selectAll = this.pagedElectricalItems.every((i) => i.selected);
    } else {
      this.selectAll = this.pagedItems.every((i) => i.selected);
    }
  }

  onSearch(event: Event): void {
    this.searchQuery = (event.target as HTMLInputElement).value;
    this.currentPage = 1;
    this.selectAll = false;
  }

  onIssueAsset(): void {
    this.router.navigate(['/assets/issue']);
  }

  goToDashboard(): void { this.router.navigate(['/']); }

  onExport(): void {
    if (this.activeTab === 'Electrical') {
      const headers = ['Asset Name', 'Asset Tag', 'Issued To', 'Department', 'Date Issued', 'Due Date', 'Status', 'Condition'];
      const rows = this.filteredElectricalItems.map((i) => [
        i.assetName, i.assetTag, i.issuedTo, i.department,
        i.dateIssued, i.dueDate, i.status, i.condition,
      ]);
      this._downloadCsv([headers, ...rows], 'electrical-issue-log.csv');
    } else {
      const headers = ['Name', 'Asset ID', 'Classification', 'Total', 'Issue Type', 'Issued To', 'Issued Date'];
      const rows = this.filteredItems.map((i) => [
        i.name, i.assetId, i.classification, i.total, i.issueType, i.issuedTo, i.issuedDate,
      ]);
      this._downloadCsv([headers, ...rows], `${this.activeTab.toLowerCase()}-issue-log.csv`);
    }
  }

  private _downloadCsv(data: (string | number)[][], filename: string): void {
    const csv  = data.map((r) => r.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  onFilter(): void {
    console.log('Filter triggered');
  }

  onRowMenu(item: ElectricalIssueItem): void {
    console.log('Row menu for', item.assetName);
  }

  onPageChange(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.selectAll = false;
    }
  }

  getPages(): (number | string)[] {
    const pages: (number | string)[] = [];
    if (this.totalPages <= 7) {
      for (let i = 1; i <= this.totalPages; i++) pages.push(i);
    } else {
      pages.push(1);
      if (this.currentPage > 3) pages.push('...');
      const start = Math.max(2, this.currentPage - 1);
      const end   = Math.min(this.totalPages - 1, this.currentPage + 1);
      for (let i = start; i <= end; i++) pages.push(i);
      if (this.currentPage < this.totalPages - 2) pages.push('...');
      pages.push(this.totalPages);
    }
    return pages;
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }
}
