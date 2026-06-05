import { Component, HostListener } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-create-asset',
  templateUrl: './create-asset.component.html',
  styleUrls: ['./create-asset.component.scss'],
})
export class CreateAssetComponent {

  // ── Left column fields ────────────────────────────────────────────────────
  company         = '';
  assetTag        = '';
  serial          = '';
  model           = '';
  status          = '';
  category        = 'Consumables';
  defaultLocation = '';

  // ── Right column fields ───────────────────────────────────────────────────
  assetName    = '';
  orderNumber  = '';
  warranty     = '';
  purchaseDate = '';
  eolDate      = '';
  supplier     = '';
  purchaseCost = '';
  isReturnable = true;

  // ── Dropdown options ──────────────────────────────────────────────────────
  companies  = ['Kristu Jayanti University', 'KJC Trust'];
  models     = ['MacBook Pro 13"', 'Dell Latitude 14', 'HP EliteBook', 'Lenovo ThinkPad'];
  statuses   = ['Available', 'Deployed', 'Under Maintenance', 'Retired'];
  categories = ['Consumables', 'I.T', 'Electricals', 'Sound', 'Stationery', 'Housekeeping', 'Furniture'];
  locations  = ['SDC Lab', 'Admin Block', 'Library', 'Sports Block', 'PFA Wing'];
  suppliers  = ['Dell India', 'Apple Reseller', 'HP India', 'Lenovo Store'];

  // ── UI state ──────────────────────────────────────────────────────────────
  sidebarOpen      = false;
  showSuccess      = false;
  isLoading        = false;
  errorMessage     = '';
  isDragOver       = false;
  assetImageFile   : File | null = null;
  assetImagePreview: string | null = null;
  billFile         : File | null = null;

  constructor(private router: Router) {}

  @HostListener('document:click')
  onDocumentClick(): void { this.sidebarOpen = false; }

  // ── Bill upload ───────────────────────────────────────────────────────────
  onBillSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.billFile = input.files[0];
    }
  }

  // ── Asset image upload ────────────────────────────────────────────────────
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) this.loadImageFile(input.files[0]);
  }
  onDragOver(event: DragEvent): void  { event.preventDefault(); this.isDragOver = true; }
  onDragLeave(event: DragEvent): void { event.preventDefault(); this.isDragOver = false; }
  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file && file.type.startsWith('image/')) this.loadImageFile(file);
  }
  private loadImageFile(file: File): void {
    this.assetImageFile = file;
    const reader = new FileReader();
    reader.onload = (e) => { this.assetImagePreview = e.target?.result as string; };
    reader.readAsDataURL(file);
  }
  removeImage(event: Event): void {
    event.stopPropagation();
    this.assetImageFile    = null;
    this.assetImagePreview = null;
  }

  // ── Navigation ────────────────────────────────────────────────────────────
  goBack()         : void { this.router.navigate(['/assets/view']); }
  goToDashboard()  : void { this.router.navigate(['/']); }
  goToIssueAsset() : void { this.router.navigate(['/assets/issue']); }
  goToIssueLog()   : void { this.router.navigate(['/assets/issue-log']); }
  goToReturnLog()  : void { this.router.navigate(['/assets/return-log']); }
  goToReports()    : void { this.router.navigate(['/assets/reports']); }

  // ── Submit ────────────────────────────────────────────────────────────────
  onSubmit(): void {
    this.errorMessage = '';

    if (!this.assetTag.trim()) {
      this.errorMessage = 'Asset Tag is required.';
      return;
    }
    if (!this.status) {
      this.errorMessage = 'Status is required.';
      return;
    }

    this.isLoading = true;

    const payload = {
      company:         this.company,
      assetTag:        this.assetTag,
      serial:          this.serial,
      model:           this.model,
      status:          this.status,
      category:        this.category,
      defaultLocation: this.defaultLocation,
      assetName:       this.assetName,
      orderNumber:     this.orderNumber,
      warranty:        this.warranty,
      purchaseDate:    this.purchaseDate,
      eolDate:         this.eolDate,
      supplier:        this.supplier,
      purchaseCost:    this.purchaseCost,
      isReturnable:    this.isReturnable,
    };

    console.log('Create Asset payload:', payload);

    // TODO: replace with real API call via AssetService
    setTimeout(() => {
      this.isLoading   = false;
      this.showSuccess = true;
    }, 600);
  }

  // ── Reset form ────────────────────────────────────────────────────────────
  createAnother(): void {
    this.showSuccess      = false;
    this.company          = '';
    this.assetTag         = '';
    this.serial           = '';
    this.model            = '';
    this.status           = '';
    this.category         = 'Consumables';
    this.defaultLocation  = '';
    this.assetName        = '';
    this.orderNumber      = '';
    this.warranty         = '';
    this.purchaseDate     = '';
    this.eolDate          = '';
    this.supplier         = '';
    this.purchaseCost     = '';
    this.isReturnable     = true;
    this.assetImageFile   = null;
    this.assetImagePreview = null;
    this.billFile         = null;
    this.errorMessage     = '';
  }
}
