package org.galatea.starter.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/* For builder since we explicitly want to make the all args ctor private */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE) // For spring and jackson
@Builder
@Data
@Entity
@XmlRootElement(name = "iexHistoricalPrice")
public class IexHistoricalPrice {

  @Id
  @JsonIgnore
  @GeneratedValue(strategy = GenerationType.AUTO)
  protected Long id;

  @NonNull
  protected BigDecimal close;

  @NonNull
  protected BigDecimal high;

  @NonNull
  protected BigDecimal low;

  @NonNull
  protected BigDecimal open;

  @NonNull
  protected String symbol;

  @NonNull
  protected BigInteger volume;

  @NonNull
  protected String date;
}