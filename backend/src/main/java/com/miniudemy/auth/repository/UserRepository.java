package com.miniudemy.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.auth.domain.User;

/**
 * Kullanıcı sorguları.
 *
 * E-posta aramaları büyük/küçük harfe duyarsızdır ve `lower(email)` ile yapılır: veritabanındaki tekil
 * index (`uk_users_email`) aynı ifade üzerinde olduğu için sorgu bu index'i kullanır. Spring Data'nın
 * `...IgnoreCase` türetmesi `upper(...)` ürettiği için index'i kullanamazdı.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("select count(u) > 0 from User u where lower(u.email) = lower(:email)")
    boolean existsByEmail(@Param("email") String email);
}
