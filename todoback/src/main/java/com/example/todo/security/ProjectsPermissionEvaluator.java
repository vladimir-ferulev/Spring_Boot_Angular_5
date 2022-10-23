package com.example.todo.security;

import com.example.todo.model.Project;
import com.example.todo.model.User;
import com.example.todo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
public class ProjectsPermissionEvaluator {

    @Autowired
    private UserRepository userRepository;

    public boolean userHasProject(String username, String projectName) {
        if (!StringUtils.hasLength(username) || !StringUtils.hasLength(projectName)) {
            return false;
        }
        Optional<User> userOptional = userRepository.getUserByUsername(username);
        if (!userOptional.isPresent()) {
            return false;
        }
        for (Project project : userOptional.get().getProjects()) {
            if (projectName.equals(project.getName())) {
                return true;
            }
        }
        return false;
    }
}