package com.example.todo.controller;

import com.example.todo.model.Task;
import com.example.todo.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import java.util.Collection;
import java.util.Optional;

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
    public Task createTask(@RequestBody Task task) {
        task.setCompleted(false);
        return taskRepository.save(task);
    }

//  @Secured("ROLE_VIEWER")
//  @PreAuthorize("hasRole('ROLE_VIEWER')")
//  @RolesAllowed({ "ROLE_VIEWER", "ROLE_EDITOR" })
    @PreAuthorize("hasAuthority('VIEW_TASK')")
    @GetMapping(value = "/tasks/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable("id") Long id) {
        Optional<Task> optional = taskRepository.findById(id);
        return optional.map(task -> new ResponseEntity<>(task, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping(value = "/tasks/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable("id") Long id,
                                           @RequestBody Task task) {
        Optional<Task> optional = taskRepository.findById(id);
        if(!optional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Task origTask = optional.get();
        origTask.setTitle(task.getTitle());
        origTask.setCompleted(task.getCompleted());
        Task taskUpdated = taskRepository.save(origTask);
        return new ResponseEntity<>(taskUpdated, HttpStatus.OK);
    }

    @DeleteMapping(value = "/tasks/{id}")
    public void deleteTask(@PathVariable("id") Long id) {
        taskRepository.deleteById(id);
    }
}
