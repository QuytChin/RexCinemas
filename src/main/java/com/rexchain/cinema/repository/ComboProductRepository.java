package com.rexchain.cinema.repository;
import com.rexchain.cinema.entity.ComboProduct;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface ComboProductRepository extends JpaRepository<ComboProduct,Long>{
    List<ComboProduct> findByActiveTrueOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ComboProduct c where c.id in :ids order by c.id")
    List<ComboProduct> findByIdInForUpdate(@Param("ids") List<Long> ids);
}
