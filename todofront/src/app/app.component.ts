import { Component, OnInit } from '@angular/core';
import {TaskService} from './services/task.service';
import {Task} from './models/task';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  tasks: Task[];
  newTask: Task = new Task();
  editing = false;
  editingTask: Task = new Task;

  constructor(private taskService: TaskService) {}

  ngOnInit(): void {
    this.getTasks();
  }

  getTasks(): void {
    this.taskService.getTasks()
      .subscribe(value => {this.tasks = value; });
  }

  createTask(): void {
    this.taskService.createTask(this.newTask)
      .subscribe(createTask => {
        this.newTask = new Task();
        this.tasks.unshift(createTask);
      });
  }

  updateTask(taskData: Task): void {
    this.taskService.updateTask(taskData)
      .subscribe(updatedTask => {
        const existingTask = this.tasks.find(task => task.id === updatedTask.id);
        Object.assign(existingTask, updatedTask);
        this.clearEditing();
      });
  }

  toggleCompleted(taskData: Task): void {
    taskData.completed = !taskData.completed;
    this.taskService.updateTask(taskData)
      .subscribe(updatedTask => {
        const existingTask = this.tasks.find(task => task.id === updatedTask.id);
        Object.assign(existingTask, updatedTask);
      });
  }

  clearEditing(): void {
    this.editingTask = new Task();
    this.editing = false;
  }

  editTask(taskData: Task): void {
    this.editing = true;
    Object.assign(this.editingTask, taskData);
  }

  deleteTask(id: number): void {
    this.taskService.deleteTask(id)
      .subscribe(() => {
      this.tasks = this.tasks.filter(task => task.id !== id);
      });
  }


}

