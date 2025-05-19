package com.example.demo.repository;


import com.example.demo.model.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface UserRepository extends MongoRepository<User, String> {
    // Custom query methods (if needed) can be defined here
    User findByEmail(String email);
    @NotNull
    User save(User user);// Find a user by their email

}


