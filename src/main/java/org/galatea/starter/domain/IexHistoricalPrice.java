package org.galatea.starter.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IexHistoricalPrice {
  private BigDecimal close;
  private BigDecimal high;
  private BigDecimal low;
  private BigDecimal open;
  private String symbol;
  private BigInteger volume;
  private String date;
}
