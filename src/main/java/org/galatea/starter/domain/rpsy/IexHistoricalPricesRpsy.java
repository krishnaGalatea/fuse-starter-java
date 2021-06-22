package org.galatea.starter.domain.rpsy;

import java.util.List;
import java.util.Optional;
import org.galatea.starter.domain.IexHistoricalPrice;
import org.springframework.data.repository.CrudRepository;

public interface IexHistoricalPricesRpsy extends CrudRepository<IexHistoricalPrice, Long> {

  /**
   * Retrieves all entities with the given symbol.
   * @param symbol the symbol to find in the DB
   * @return List of entities with that symbol
   */
  List<IexHistoricalPrice> findBySymbolIgnoreCase(String symbol);

  /**
   * Retrieves all entities with the given symbol and date.
   * @param symbol the symbol of the entity
   * @param date the date of the entity
   * @return List of entities with that symbol and date
   */
  List<IexHistoricalPrice> findBySymbolIgnoreCaseAndDate(String symbol, String date);


  @Override
  Optional<IexHistoricalPrice> findById(Long id);

  @Override
  <S extends IexHistoricalPrice> S save(S entity);

  /**
   * 'p0' required in key because java does not retain parameter names during compilation unless
   * specified. You must use position parameter bindings otherwise.
   */
  @Override
  <S extends IexHistoricalPrice> Iterable<S> saveAll(Iterable<S> entity);
}
