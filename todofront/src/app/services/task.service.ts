import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs/Observable';
import {Task} from '../models/task';

@Injectable()
export class TaskService {
  private baseUrl = 'http://localhost:8080/api/tasks/';

  constructor(private httpClient: HttpClient) { }

  getTasks(): Observable<any> {
    return this.httpClient.get(this.baseUrl);
  }

  createTask(taskData: Task): Observable<any> {
    return this.httpClient.post(this.baseUrl, taskData);
  }

  updateTask(taskData: Task): Observable<any> {
    return this.httpClient.put(this.baseUrl + taskData.id, taskData);
  }

  deleteTask(id: number): Observable<any> {
    return this.httpClient.delete(this.baseUrl + id);
  }

}
