package com.rexchain.cinema.repository;
import com.rexchain.cinema.entity.ComboProduct; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface ComboProductRepository extends JpaRepository<ComboProduct,Long>{List<ComboProduct> findByActiveTrueOrderByIdAsc();}
