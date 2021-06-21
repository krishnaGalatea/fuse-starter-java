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
   * Gets all of the characters in a string that are letters.
   * @param str to get letters from.
   * @return letter characters from string.
   */
  private String getAlphabeticChars(String str) {
    StringBuilder sb = new StringBuilder();
    for (char c : str.toCharArray()) {
      if (Character.isAlphabetic(c)) {
        sb.append(c);
      }
    }
    String alphaChars = sb.toString();
    return alphaChars;
  }

  /**
   * Gets all of the digits from a string and forms and integer.
   * @param str to get digits from.
   * @return integer formed from string.
   */
  private int getIntFromString(String str) {
    StringBuilder sb = new StringBuilder();
    boolean hasNum = false;
    for (char c : str.toCharArray()) {
      if (Character.isDigit(c)) {
        sb.append(c);
        hasNum = true;
      }
    }
    int num = 0;
    if (hasNum) {
      num = Integer.parseInt(sb.toString());
    }
    return num;
  }

  /**
   * Formats date string of type "YYYYMMDD" to "YYYY-MM-DD".
   * @param date string formatted as "YYYYMMDD".
   * @return string of date in form "YYYY-MM-DD".
   */
  private String dateFormat(String date) {
    return date.substring(0,4) + "-" + date.substring(4,6) + "-" + date.substring(6);
  }

  /**
   * Gets the past date.
   * @param i amount of days that passed.
   * @return the date in the past.
   */
  private LocalDate newDate(int i) {
    LocalDate today = LocalDate.now();
    LocalDate dayCheck = today.minusDays(i);
    return dayCheck;
  }

  /**
   * Calculates the amount of days wanted for the request.
   * @param rangeType day, week, month, year, or year to date.
   * @param rangeLength the amount of the rangeType.
   * @return the number of days to check for prices.
   */
  private int getTotalNumberOfDays(String rangeType, int rangeLength) {
    int amtDays = 0;
    if (rangeType.equals("d")) {
      amtDays = rangeLength;
    } else if (rangeType.equals("w")) {
      amtDays = rangeLength * 7;
    } else if (rangeType.equals("m")) {
      amtDays = (rangeLength * 30) + 1;
    } else if (rangeType.equals("y")) {
      amtDays = rangeLength * 365;
    } else if (rangeType.equals("ytd")) {
      amtDays = LocalDate.now().getDayOfYear();
    }
    return amtDays;
  }

  /**
   * Updates the historical prices repository with new calls to the cloud API
   * @param symbol the symbol to look up
   * @param range the amount of days to check
   * @param date optional date to check for one day
   * @return updated list of prices that were added to the repository
   */
  private List<IexHistoricalPrice> updateHistoricalDB(String symbol, String range, String date) {
    List<IexHistoricalPrice> newHistoricalPrices;
    newHistoricalPrices = iexCloudClient.getAllHistoricalPrices(symbol, range, date);
    historicalPricesRpsy.saveAll(newHistoricalPrices);
    return newHistoricalPrices;
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

    ArrayList<IexHistoricalPrice> finalHistoricalPrices = new ArrayList<IexHistoricalPrice>();

    if (historicalPricesRpsy.count() == 0
        || historicalPricesRpsy.findBySymbolIgnoreCase(symbol).isEmpty()) {
      log.info("call thinks there is no symbol in rpsy");
      return updateHistoricalDB(symbol, range, date);
    } else {
      log.info("symbol in rpsy, do everything else");
      int rangeLength = getIntFromString(range);
      String rangeType = getAlphabeticChars(range);

      int amtDays = 0;
      if (rangeType.equals("d") || rangeType.equals("w") || rangeType.equals("m")
      || rangeType.equals("y") || rangeType.equals("ytd") ) {
        amtDays = getTotalNumberOfDays(rangeType, rangeLength);
      } else if (rangeType.equals("max")) {
        return updateHistoricalDB(symbol, range, date);
      } else if (rangeType.equals("date")) {
        if (date == null) {
          return Collections.emptyList();
        }
        String rpsyDate = dateFormat(date);
        finalHistoricalPrices.addAll(historicalPricesRpsy
            .findBySymbolIgnoreCaseAndDate(symbol, rpsyDate));
        if (finalHistoricalPrices.isEmpty() || finalHistoricalPrices.size() == 0) {
          log.info("getting one date from cloud");
          return updateHistoricalDB(symbol, range, date);
        }
        return finalHistoricalPrices;
      }

      for (int i = 1; i <= amtDays; i++) {
        LocalDate dayCheck = newDate(i);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYYMMdd");
        String dateFind = formatter.format(dayCheck);
        String dateString = dayCheck.toString();

        if (historicalPricesRpsy.findBySymbolIgnoreCaseAndDate(symbol, dateString).isEmpty()) {
          List<IexHistoricalPrice> newHistoricalPrices = updateHistoricalDB(symbol, range, dateFind);
          finalHistoricalPrices.addAll(newHistoricalPrices);
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
