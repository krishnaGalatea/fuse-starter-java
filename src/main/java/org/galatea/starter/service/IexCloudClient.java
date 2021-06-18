package org.galatea.starter.service;

import java.util.List;
import org.galatea.starter.domain.IexHistoricalPrice;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * A Feign Declarative REST Client to access endpoints from the Cloud IEX API to get market
 * data. See https://iexcloud.io/docs/api/#historical-prices
 */
@FeignClient(name = "IEXCloud", url = "${spring.rest.iexCloudBasePath}")
public interface IexCloudClient {

  /**
   * Get all historical prices for a symbol and range that is passed in.
   * @param symbol a symbol to get historical prices for.
   * @param range the time period of prices to retrieve.
   * @param date optional specific date.
   * @return a list of historical prices for the symbol passed in.
   */
  @GetMapping("/stock/{symbol}/chart/{range}/{date}?token=${spring.rest.token}")
  List<IexHistoricalPrice> getAllHistoricalPrices(@PathVariable("symbol") String symbol,
      @PathVariable("range") String range, @PathVariable(required = false) String date);
}
