package org.galatea.starter.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.galatea.starter.domain.IexHistoricalPrice;
import org.galatea.starter.domain.IexLastTradedPrice;
import org.galatea.starter.domain.IexSymbol;
import org.galatea.starter.domain.rpsy.IexHistoricalPricesRpsy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * A layer for transformation, aggregation, and business required when retrieving data from IEX.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IexService {

  @NonNull
  private IexClient iexClient;

  /**
   * For Historical Prices Endpoint.
   */
  @NonNull
  private IexCloudClient iexCloudClient;

  /**
   * Data store for historical prices retrieved in the past.
   */
  @NonNull
  private IexHistoricalPricesRpsy historicalPricesRpsy;


  /**
   * Get all stock symbols from IEX.
   *
   * @return a list of all Stock Symbols from IEX.
   */
  public List<IexSymbol> getAllSymbols() {
    return iexClient.getAllSymbols();
  }

  /**
   * Get the last traded price for each Symbol that is passed in.
   *
   * @param symbols the list of symbols to get a last traded price for.
   * @return a list of last traded price objects for each Symbol that is passed in.
   */
  public List<IexLastTradedPrice> getLastTradedPriceForSymbols(final List<String> symbols) {
    if (CollectionUtils.isEmpty(symbols)) {
      return Collections.emptyList();
    } else {
      return iexClient.getLastTradedPriceForSymbols(symbols.toArray(new String[0]));
    }
  }

  /**
   * Get all historical prices for a symbol and range that is passed in.
   * @param symbol a symbol to get historical prices for.
   * @param range the time period of prices to retrieve.
   * @param date optional specific date.
   * @return a list of historical prices for the symbol passed in.
   */
  public List<IexHistoricalPrice> getAllHistoricalPrices(final String symbol, final String range,
      final String date) {

    // This holds the new Historical Prices retrieved from the cloud for the first time.
    // Refreshes for each call
    List<IexHistoricalPrice> newHistoricalPrices;

    // Holds the final list of historical prices to return. Contains prices from DB and cloud
    ArrayList<IexHistoricalPrice> finalHistoricalPrices = new ArrayList<IexHistoricalPrice>();

    // Begin check to see if the cloud needs to be used.
    if (historicalPricesRpsy.count() == 0
        || historicalPricesRpsy.findBySymbolIgnoreCase(symbol).isEmpty()) {
      log.info("call thinks there is no symbol in rpsy");
      // If DB is empty, or if it doesn't have the symbol call client and store entities in the DB
      newHistoricalPrices = iexCloudClient.getAllHistoricalPrices(symbol, range, date);
      historicalPricesRpsy.saveAll(newHistoricalPrices);
      // Return the new call
      return newHistoricalPrices;
    } else {
      log.info("symbol in rpsy, do everything else");
      //if symbol exists in DB, find how many days we request and check for all of them
      // get the number of days, weeks, or months from the range if given
      StringBuilder sb = new StringBuilder();
      boolean hasNum = false;
      for (char c : range.toCharArray()) {
        if (Character.isDigit(c)) {
          sb.append(c);
          hasNum = true;
        }
      }
      int rangeLength = 0;
      if (hasNum) {
        rangeLength = Integer.parseInt(sb.toString());
      }

      //Get the type of range (days, weeks, months, or other)
      StringBuilder sb2 = new StringBuilder();
      for (char c : range.toCharArray()) {
        if (Character.isAlphabetic(c)) {
          sb2.append(c);
        }
      }
      String rangeType = sb2.toString();
      log.info("range Type is:" + rangeType);
      LocalDate today = LocalDate.now();

      // Find the final amount of days to check for
      int amtDays = 0;
      if (rangeType.equals("d")) {
        amtDays = rangeLength;
      } else if (rangeType.equals("w")) {
        amtDays = rangeLength * 7;
      } else if (rangeType.equals("m")) {
        amtDays = (rangeLength * 30) + 1;
      } else if (rangeType.equals("y")) {
        amtDays = rangeLength * 365;
      } else if (rangeType.equals("max")) {
        newHistoricalPrices = iexCloudClient.getAllHistoricalPrices(symbol, range, date);
        historicalPricesRpsy.saveAll(newHistoricalPrices);
        return newHistoricalPrices;
      } else if (rangeType.equals("ytd")) {
        amtDays = LocalDate.now().getDayOfYear();
      } else if (rangeType.equals("date")) {
        if (date == null) {
          return Collections.emptyList();
        }
        String newDate = date.substring(0,4) + "-" + date.substring(4,6) + "-" + date.substring(6);
        log.info("new Date is:" + newDate);
        finalHistoricalPrices.addAll(historicalPricesRpsy
            .findBySymbolIgnoreCaseAndDate(symbol, newDate));
        log.info(String.valueOf(finalHistoricalPrices.get(0)));
        if (finalHistoricalPrices.isEmpty() || finalHistoricalPrices.size() == 0) {
          log.info("getting one date from cloud");
          newHistoricalPrices = iexCloudClient.getAllHistoricalPrices(symbol, range, date);
          historicalPricesRpsy.saveAll(newHistoricalPrices);
          return newHistoricalPrices;
        }
        return finalHistoricalPrices;
      }

      // Check every date needed and see if it is in the rpsy
      for (int i = 1; i <= amtDays; i++) {
        LocalDate dayCheck = today.minusDays(i);
        String dateString = dayCheck.toString();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYYMMdd");
        String dateFind = formatter.format(dayCheck);

        log.info(String.valueOf(historicalPricesRpsy.findAll()));
        log.info(
            String.valueOf(historicalPricesRpsy.findBySymbolIgnoreCaseAndDate(symbol, dateString)));
        if (historicalPricesRpsy.findBySymbolIgnoreCaseAndDate(symbol, dateString).isEmpty()) {
          newHistoricalPrices = iexCloudClient
              .getAllHistoricalPrices(symbol, range, dateFind);
          historicalPricesRpsy.saveAll(newHistoricalPrices);
          finalHistoricalPrices.addAll(newHistoricalPrices);
          log.info(String.valueOf(finalHistoricalPrices.toArray()));
        } else {
          log.info("Getting Historical Prices from Database");
          finalHistoricalPrices.addAll(historicalPricesRpsy
              .findBySymbolIgnoreCaseAndDate(symbol, dateString));
        }

      }

      return finalHistoricalPrices;
    }
  }
}
