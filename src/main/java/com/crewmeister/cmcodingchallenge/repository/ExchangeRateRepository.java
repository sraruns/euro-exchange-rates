package com.crewmeister.cmcodingchallenge.repository;

import com.crewmeister.cmcodingchallenge.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyAndDate(String currency, LocalDate date);

    List<ExchangeRate> findByDate(LocalDate date);

    List<ExchangeRate> findByCurrencyAndDateBetween(String currency, LocalDate startDate, LocalDate endDate);

    boolean existsByCurrencyAndDate(String currency, LocalDate date);
}
