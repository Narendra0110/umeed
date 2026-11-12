package com.umeed.categories;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByActiveTrueOrderBySortOrderAsc();
}
