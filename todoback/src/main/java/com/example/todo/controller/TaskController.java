package com.example.todo.controller;

import com.example.todo.model.Task;
import com.example.todo.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collection;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    TaskRepository taskRepository;

    @GetMapping("/tasks")
    public Collection<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @PostMapping("/tasks")
    public Task createTask(@Valid @RequestBody Task task) {
        task.setCompleted(false);
        return taskRepository.save(task);
    }

    @GetMapping(value = "/tasks/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable("id") Long id) {
        Task task = taskRepository.findOne(id);
        if (task == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(task, HttpStatus.OK);
        }
    }

    @PutMapping(value = "/tasks/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable("id") Long id,
                                           @Valid @RequestBody Task task) {
        Task taskRep = taskRepository.findOne(id);
        if(taskRep == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        taskRep.setTitle(task.getTitle());
        taskRep.setCompleted(task.getCompleted());
        Task taskUpdated = taskRepository.save(taskRep);
        return new ResponseEntity<>(taskUpdated, HttpStatus.OK);
    }

    @DeleteMapping(value = "/tasks/{id}")
    public void deleteTask(@PathVariable("id") Long id) {
        taskRepository.delete(id);
    }
}
