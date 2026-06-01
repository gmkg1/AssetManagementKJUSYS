import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AssetService } from './asset.service';
import { environment } from '../../environments/environment';

/**
 * Standalone debug component — drop <app-api-health> anywhere in a template
 * to see live connection status for all three API endpoints.
 * Remove from production once verified.
 */
@Component({
  selector: 'app-api-health',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed bottom-4 right-4 z-50 bg-white border border-slate-200 rounded-xl shadow-lg p-4 w-72 text-xs font-mono">
      <p class="text-[10px] font-semibold text-slate-500 uppercase tracking-widest mb-2">API Connection Status</p>
      <p class="text-[10px] text-slate-400 mb-3 break-all">{{ baseUrl }}</p>
      <div class="flex flex-col gap-2">
        <div *ngFor="let e of endpoints" class="flex items-center justify-between gap-2">
          <span class="text-slate-600 truncate">{{ e.label }}</span>
          <span class="px-2 py-0.5 rounded-full text-[10px] font-semibold flex-shrink-0"
            [class.bg-green-100]="e.status === 'OK'"
            [class.text-green-700]="e.status === 'OK'"
            [class.bg-red-100]="e.status === 'ERROR'"
            [class.text-red-700]="e.status === 'ERROR'"
            [class.bg-slate-100]="e.status === 'checking'"
            [class.text-slate-500]="e.status === 'checking'">
            {{ e.status }}
          </span>
        </div>
      </div>
      <button (click)="runChecks()"
        class="mt-3 w-full text-center text-[10px] py-1 rounded-md bg-slate-800 text-white hover:bg-slate-700 transition-colors">
        Re-check
      </button>
    </div>
  `
})
export class ApiHealthComponent implements OnInit {
  baseUrl = environment.apiUrl;

  endpoints = [
    { label: '/health',               status: 'checking', call: () => this.svc.checkHealth() },
    { label: '/assets/category-count', status: 'checking', call: () => this.svc.getCategoryCount() },
    { label: '/assets/issued-assets',  status: 'checking', call: () => this.svc.getIssuedAssets() },
  ];

  constructor(private svc: AssetService) {}

  ngOnInit(): void { this.runChecks(); }

  runChecks(): void {
    this.endpoints.forEach(e => {
      e.status = 'checking';
      e.call().subscribe({
        next: () => e.status = 'OK',
        error: () => e.status = 'ERROR'
      });
    });
  }
}
