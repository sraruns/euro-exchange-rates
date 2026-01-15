package com.crewmeister.cmcodingchallenge.repository;

import com.crewmeister.cmcodingchallenge.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    @Query("SELECT DISTINCT e.date FROM ExchangeRate e WHERE e.baseCurrency = :baseCurrency AND e.date BETWEEN :startDate AND :endDate ORDER BY e.date DESC")
    Page<LocalDate> findDistinctDatesByBaseCurrencyAndDateBetween(
            @Param("baseCurrency") String baseCurrency,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("SELECT COUNT(DISTINCT e.date) FROM ExchangeRate e WHERE e.baseCurrency = :baseCurrency AND e.date BETWEEN :startDate AND :endDate")
    long countDistinctDatesByBaseCurrencyAndDateBetween(
            @Param("baseCurrency") String baseCurrency,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT MIN(e.date) FROM ExchangeRate e WHERE e.baseCurrency = :baseCurrency AND e.date BETWEEN :startDate AND :endDate")
    Optional<LocalDate> findMinDateByBaseCurrencyAndDateBetween(
            @Param("baseCurrency") String baseCurrency,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT MAX(e.date) FROM ExchangeRate e WHERE e.baseCurrency = :baseCurrency AND e.date BETWEEN :startDate AND :endDate")
    Optional<LocalDate> findMaxDateByBaseCurrencyAndDateBetween(
            @Param("baseCurrency") String baseCurrency,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<ExchangeRate> findByBaseCurrencyAndDateIn(String baseCurrency, List<LocalDate> dates);

    Optional<ExchangeRate> findByBaseCurrencyAndTargetCurrencyAndDate(
            String baseCurrency, String targetCurrency, LocalDate date);

    List<ExchangeRate> findByBaseCurrencyAndDate(String baseCurrency, LocalDate date);

    List<ExchangeRate> findByBaseCurrencyAndDateBetween(
            String baseCurrency, LocalDate startDate, LocalDate endDate);

    Page<ExchangeRate> findByBaseCurrencyAndDateBetween(
            String baseCurrency, LocalDate startDate, LocalDate endDate, Pageable pageable);

    boolean existsByBaseCurrencyAndTargetCurrencyAndDate(
            String baseCurrency, String targetCurrency, LocalDate date);
}
