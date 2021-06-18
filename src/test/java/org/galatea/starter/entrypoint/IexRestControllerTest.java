package org.galatea.starter.entrypoint;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import junitparams.JUnitParamsRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.galatea.starter.ASpringTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


@RequiredArgsConstructor
@Slf4j
// We need to do a full application start up for this one, since we want the feign clients to be instantiated.
// It's possible we could do a narrower slice of beans, but it wouldn't save that much test run time.
@SpringBootTest
// this gives us the MockMvc variable
@AutoConfigureMockMvc
// we previously used WireMockClassRule for consistency with ASpringTest, but when moving to a dynamic port
// to prevent test failures in concurrent builds, the wiremock server was created too late and feign was
// already expecting it to be running somewhere else, resulting in a connection refused
@AutoConfigureWireMock(port = 0, files = "classpath:/wiremock")
// Use this runner since we want to parameterize certain tests.
// See runner's javadoc for more usage.
@RunWith(JUnitParamsRunner.class)
public class IexRestControllerTest extends ASpringTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void testGetSymbolsEndpoint() throws Exception {
    MvcResult result = this.mvc.perform(
        // note that we were are testing the fuse REST end point here, not the IEX end point.
        // the fuse end point in turn calls the IEX end point, which is WireMocked for this test.
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/iex/symbols")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        // some simple validations, in practice I would expect these to be much more comprehensive.
        .andExpect(jsonPath("$[0].symbol", is("A")))
        .andExpect(jsonPath("$[1].symbol", is("AA")))
        .andExpect(jsonPath("$[2].symbol", is("AAAU")))
        .andReturn();
  }

  @Test
  public void testGetLastTradedPrice() throws Exception {

    MvcResult result = this.mvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .get("/iex/lastTradedPrice?symbols=AAPL")
            // This URL will be hit by the MockMvc client. The result is configured in the file
            // src/test/resources/wiremock/mappings/mapping-lastTradedPrice.json
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].symbol", is("AAPL")))
        .andExpect(jsonPath("$[0].price").value(new BigDecimal("186.3011")))
        .andReturn();
  }

  @Test
  public void testGetHistorical() throws Exception {

    MvcResult result = this.mvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .get("/iex/historicalPrices?symbol=TWTR&range=1m&date=")
            // This URL will be hit by the MockMvc client. The result is configured in the file
            // src/test/resources/wiremock/mappings/mapping-historicalPrices.json
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].close").value(new BigDecimal("51.81")))
        .andExpect(jsonPath("$[0].high").value(new BigDecimal("52.08")))
        .andExpect(jsonPath("$[0].low").value(new BigDecimal("51.62")))
        .andExpect(jsonPath("$[0].open").value(new BigDecimal("53.01")))
        .andExpect(jsonPath("$[0].symbol", is("TWTR")))
        .andExpect(jsonPath("$[0].volume").value(new BigInteger("17362305")))
        .andExpect(jsonPath("$[1].close").value(new BigDecimal("52.88")))
        .andExpect(jsonPath("$[1].high").value(new BigDecimal("53.1865")))
        .andExpect(jsonPath("$[1].low").value(new BigDecimal("49.2")))
        .andExpect(jsonPath("$[1].open").value(new BigDecimal("49.52")))
        .andExpect(jsonPath("$[1].symbol", is("TWTR")))
        .andExpect(jsonPath("$[1].volume").value(new BigInteger("16749949")))
        .andExpect(jsonPath("$[2].close").value(new BigDecimal("50.7")))
        .andExpect(jsonPath("$[2].high").value(new BigDecimal("52.38")))
        .andExpect(jsonPath("$[2].low").value(new BigDecimal("50.06")))
        .andExpect(jsonPath("$[2].open").value(new BigDecimal("51.87")))
        .andExpect(jsonPath("$[2].symbol", is("TWTR")))
        .andExpect(jsonPath("$[2].volume").value(new BigInteger("17223215")))
        .andReturn();
  }

//  @Test
//  public void testGetHistoricalDate() throws Exception {
//
//    MvcResult result = this.mvc.perform(
//        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
//            .get("/iex/historicalPrices?symbol=TWTR&range=date&chartByDay=true&date=20200610")
//            // This URL will be hit by the MockMvc client. The result is configured in the file
//            // src/test/resources/wiremock/mappings/mapping-historicalPrices.json
//            .accept(MediaType.APPLICATION_JSON_VALUE))
//        .andExpect(status().isOk())
//        .andExpect(jsonPath("$[0].close").value(new BigDecimal("36.33")))
//        .andExpect(jsonPath("$[0].high").value(new BigDecimal("36.33")))
//        .andExpect(jsonPath("$[0].low").value(new BigDecimal("36.06")))
//        .andExpect(jsonPath("$[0].open").value(new BigDecimal("36.125")))
//        .andExpect(jsonPath("$[0].volume").value(new BigInteger("600")))
//        .andExpect(jsonPath("$[0].date", is("2020-06-10")))
//        .andExpect(jsonPath("$[1].close").value(new BigDecimal("36.2")))
//        .andExpect(jsonPath("$[1].high").value(new BigDecimal("36.37")))
//        .andExpect(jsonPath("$[1].low").value(new BigDecimal("36.2")))
//        .andExpect(jsonPath("$[1].open").value(new BigDecimal("36.37")))
//        .andExpect(jsonPath("$[1].volume").value(new BigInteger("936")))
//        .andExpect(jsonPath("$[1].date", is("2020-06-10")))
//        .andReturn();
//  }

  @Test
  public void testGetLastTradedPriceEmpty() throws Exception {

    MvcResult result = this.mvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .get("/iex/lastTradedPrice?symbols=")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", is(Collections.emptyList())))
        .andReturn();
  }
}