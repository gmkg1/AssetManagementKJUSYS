import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AssetService {

  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** GET /assets — full asset list with joins */
  getAssets() {
    return this.http.get<{ assets: any[] }>(`${this.baseUrl}/assets`);

  }
  getCategoryCounts() {
    return this.http.get(
      'http://localhost:8080/kjusys-api/asset-management-api/assets/category-count'
    );
  }

  /** GET /assets/category-count — asset count grouped by category */
  getCategoryCount() {
    return this.http.get<any>(`${this.baseUrl}/assets/category-count`);
  }

  /** GET /assets/issued-assets — detailed issued assets */
  getIssuedAssets() {
    return this.http.get<any>(`${this.baseUrl}/assets/issued-assets`);
  }

  /** GET /health — backend health check */
  checkHealth() {
    return this.http.get(`${this.baseUrl}/health`);
  }
}
