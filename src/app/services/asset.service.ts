import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AssetService {

  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** GET /assets?page=X&size=Y */
  getAssets(page: number = 1, size: number = 10) {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/assets`, { params });
  }

  /** GET /grp?categoryId=X&page=Y&pageSize=Z */
  getAssetsByCategory(categoryId: string, page: number = 1, pageSize: number = 10) {
    const params = new HttpParams()
      .set('categoryId', categoryId)
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());
    return this.http.get<any>(`${this.baseUrl}/grp`, { params });
  }

  /** GET /categories?page=X&size=Y */
  getCategoryCount(page: number = 1, size: number = 10) {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/categories`, { params });
  }

  /** GET /issued-assets?page=X&size=Y */
  getIssuedAssets(page: number = 1, size: number = 10) {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/issued-assets`, { params });
  }

  /** GET /return-logs?page=X&size=Y */
  getReturnLogs(page: number = 1, size: number = 10) {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/return-logs`, { params });
  }

  /** GET /asset-status-summary?page=X&size=Y */
  getAssetStatusSummary(page: number = 1, size: number = 10) {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/asset-status-summary`, { params });
  }
}
