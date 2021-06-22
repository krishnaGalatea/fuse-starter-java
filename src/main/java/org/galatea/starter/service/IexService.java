package org.galatea.starter.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.galatea.starter.domain.IexHistoricalPrice;
import org.galatea.starter.domain.IexLastTradedPrice;
import org.galatea.starter.domain.IexSymbol;
import org.galatea.starter.domain.rpsy.IexHistoricalPricesRpsy;
import org.springframework.format.datetime.DateFormatter;
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
  private String getAlphabeticChars(final String str) {
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
  private int getIntFromString(final String str) {
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
  private String dateFormat(final String date) throws ParseException {
    // This is the expected format of the input date in the U.S.
    DateFormat inputFormatter = new SimpleDateFormat("yyyyMMdd");
    Date inputDate = inputFormatter.parse(date);

    DateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd");
    String outDate = outputFormatter.format(inputDate);
    return outDate;
    //return date.substring(0,4) + "-" + date.substring(4,6) + "-" + date.substring(6);
  }

  /**
   * Gets the past date.
   * @param i amount of days that passed.
   * @return the date in the past.
   */
  private LocalDate newDate(final int i) {
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
  private int getTotalNumberOfDays(final String rangeType, final int rangeLength) {
    int amtDays = 0;
    if (rangeType.equals("d")) {
      amtDays = rangeLength;
    } else if (rangeType.equals("w")) {
      amtDays = rangeLength * 7;
    } else if (rangeType.equals("m")) {
      // months follow the following calculations in this API
      amtDays = (rangeLength * 30) + 1;
    } else if (rangeType.equals("y")) {
      // Years follow the following calculations in this API
      amtDays = rangeLength * 365;
    } else if (rangeType.equals("ytd")) {
      amtDays = LocalDate.now().getDayOfYear();
    } else if (rangeType.equals("max")) {
      amtDays = 0;
    } else if (rangeType.equals("date")) {
      amtDays = -1;
    }
    return amtDays;
  }

  /**
   * Updates the historical prices repository with new calls to the cloud API.
   * @param symbol the symbol to look up.
   * @param range the amount of days to check.
   * @param date optional date to check for one day.
   * @return updated list of prices that were added to the repository.
   */
  private ArrayList<IexHistoricalPrice> updateHistoricalDB(final String symbol, final String range,
      final String date) {
    ArrayList<IexHistoricalPrice> newHistoricalPrices = new ArrayList();
    newHistoricalPrices.addAll(iexCloudClient.getAllHistoricalPrices(symbol, range, date));
    historicalPricesRpsy.saveAll(newHistoricalPrices);
    return newHistoricalPrices;
  }

  /**
   * Finds all of the historical prices requested from cloud and database.
   * @param symbol the symbol requested.
   * @param range the range of days requested.
   * @param amtDays the total amount of days from the range.
   * @param finalHistoricalPrices array list of historical prices.
   * @return updated list of all historical prices requested.
   */
  private ArrayList<IexHistoricalPrice> findHistoricalPrices(final String symbol,
      final String range, final int amtDays,
      final ArrayList<IexHistoricalPrice> finalHistoricalPrices) {
    for (int i = amtDays; i >= 1; i--) {
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

  /**
   * Gets individual historical price by the specific date requested.
   * @param symbol the symbol requested.
   * @param range "date".
   * @param date the date requested.
   * @param finalHistoricalPrices list of current historical prices.
   * @return updated list of historical prices.
   * @throws ParseException if date cannot be parsed.
   */
  private ArrayList<IexHistoricalPrice> getPricesByDate(final String symbol, final String range,
      final String date, final ArrayList<IexHistoricalPrice> finalHistoricalPrices)
      throws ParseException {
    String rpsyDate = dateFormat(date);
    finalHistoricalPrices.addAll(historicalPricesRpsy
        .findBySymbolIgnoreCaseAndDate(symbol, rpsyDate));
    if (finalHistoricalPrices.isEmpty() || finalHistoricalPrices.size() == 0) {
      log.info("getting one date from cloud");
      List<IexHistoricalPrice> newHistoricalPrices = updateHistoricalDB(symbol, range, date);
      finalHistoricalPrices.addAll(newHistoricalPrices);
    }
    return finalHistoricalPrices;
  }

  /**
   * Get all historical prices for a symbol and range that is passed in.
   * @param symbol a symbol to get historical prices for.
   * @param range the time period of prices to retrieve.
   * @param date optional specific date.
   * @return a list of historical prices for the symbol passed in.
   * @throws ParseException if date cannot be parsed.
   */
  public List<IexHistoricalPrice> getAllHistoricalPrices(final String symbol, final String range,
      final String date) throws ParseException {

    ArrayList<IexHistoricalPrice> finalHistoricalPrices = new ArrayList<IexHistoricalPrice>();

    if (historicalPricesRpsy.count() == 0
        || historicalPricesRpsy.findBySymbolIgnoreCase(symbol).isEmpty()) {
      log.info("call thinks there is no symbol in rpsy");
      return updateHistoricalDB(symbol, range, date);
    } else {
      log.info("symbol in rpsy, do everything else");
      int rangeLength = getIntFromString(range);
      String rangeType = getAlphabeticChars(range);
      int amtDays = getTotalNumberOfDays(rangeType, rangeLength);
      if (amtDays == 0) {
        finalHistoricalPrices = updateHistoricalDB(symbol, range, date);
      } else if (amtDays == -1) {
        if (date == null) {
          return Collections.emptyList();
        }
        finalHistoricalPrices = getPricesByDate(symbol, range, date, finalHistoricalPrices);
      } else if (amtDays > 0) {
        finalHistoricalPrices = findHistoricalPrices(symbol, range, amtDays, finalHistoricalPrices);
      }
      return finalHistoricalPrices;
    }
  }
}
