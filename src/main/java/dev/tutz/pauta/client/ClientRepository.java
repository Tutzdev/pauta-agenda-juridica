package dev.tutz.pauta.client;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findAllByOrderByNameAsc(Limit limit);

    /** {@code term} já chega com os curingas escapados por {@link ClientService}. */
    @Query("""
            select c from Client c
            where lower(c.name) like :term escape '\\'
               or (:digits <> '' and c.phone like :digitsTerm)
            order by c.name
            """)
    List<Client> search(@Param("term") String term, @Param("digits") String digits,
            @Param("digitsTerm") String digitsTerm, Limit limit);
}
