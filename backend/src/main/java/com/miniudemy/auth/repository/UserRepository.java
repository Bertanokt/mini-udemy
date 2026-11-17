package com.miniudemy.auth.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.auth.domain.Role;
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

    String ADMIN_FILTER = """
            from User u
            where (:role is null or u.role = :role)
              and (:active is null or u.active = :active)
              and (lower(u.fullName) like :pattern escape '\\' or lower(u.email) like :pattern escape '\\')
            """;

    /**
     * Yönetici listesi: boş filtre koşulu devre dışı bırakır; {@code pattern} küçük harfli, {@code %} ve {@code _}
     * kaçırılmış LIKE desenidir (ad ya da e-postada arar). Sayfa başına en fazla iki sorgu (veri + sayım).
     */
    @Query(value = "select u " + ADMIN_FILTER, countQuery = "select count(u) " + ADMIN_FILTER)
    Page<User> searchForAdmin(@Param("role") Role role, @Param("active") Boolean active,
            @Param("pattern") String pattern, Pageable pageable);
}
