import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AssetService {

  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** GET /assets */
  getAssets() {
    return this.http.get<any>(`${this.baseUrl}/assets`);
  }

  /** GET /categories */
  getCategoryCount() {
    return this.http.get<any>(`${this.baseUrl}/categories`);
  }

  /** GET /issued-assets — detailed issued assets */
  getIssuedAssets() {
    return this.http.get<any>(`${this.baseUrl}/issued-assets`);
  }

  /** GET /return-logs */
  getReturnLogs() {
    return this.http.get<any>(`${this.baseUrl}/return-logs`);
  }

  /** GET /asset-status-summary */
  getAssetStatusSummary() {
    return this.http.get<any>(`${this.baseUrl}/asset-status-summary`);
  }
}
