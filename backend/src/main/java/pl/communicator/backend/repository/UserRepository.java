package pl.communicator.backend.repository;

import pl.communicator.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByEmail(String email);
    boolean existsByLogin(String login);
    Optional<User> findByLogin(String login);

    List<User> findByLoginContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(String login, String displayName);
}