package com.goservi.serviceoffer.repository;

import com.goservi.serviceoffer.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    List<Subcategory> findByCategoryIdAndActiveTrue(Long categoryId);
    List<Subcategory> findByActiveTrue();
}
