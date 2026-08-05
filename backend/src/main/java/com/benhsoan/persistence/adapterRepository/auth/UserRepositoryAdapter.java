package com.benhsoan.persistence.adapterRepository.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.auth.User;
import com.benhsoan.persistence.entity.auth.UserEntity;
import com.benhsoan.persistence.jpaRepository.auth.JpaUserRepository;
import com.benhsoan.persistence.mapper.auth.UserPersistenceMapper;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaRepository;

    private final UserPersistenceMapper mapper;

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll()
            .stream()
            .map(mapper::toDomain)
            .toList();
        }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public List<User> findAllById(List<UUID> ids) {
        return jpaRepository.findAllById(ids)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<User> findAllActiveByRoleId(UUID roleId) {
        return jpaRepository.findAllByRoleIdAndActiveTrue(roleId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
