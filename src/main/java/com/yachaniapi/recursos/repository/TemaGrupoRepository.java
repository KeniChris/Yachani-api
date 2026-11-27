package com.yachaniapi.recursos.repository;

import com.yachaniapi.recursos.entity.TemaGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TemaGrupoRepository extends JpaRepository<TemaGrupo, Long> {

    long countByGrupo_IdGrupo(Long idGrupo);

    List<TemaGrupo> findByGrupo_IdGrupoOrderByNombreAsc(Long idGrupo);

    Optional<TemaGrupo> findByIdTemaAndGrupo_IdGrupo(Long idTema, Long idGrupo);

    interface NombreTema {
        Long getIdGrupo();
        String getNombre();
    }

    @Query("""
            select t.grupo.idGrupo as idGrupo, t.nombre as nombre
            from TemaGrupo t
            where t.grupo.idGrupo in :ids
            order by t.grupo.idGrupo, t.idTema
            """)
    List<NombreTema> listarNombresPorGrupos(@Param("ids") Collection<Long> ids);
}