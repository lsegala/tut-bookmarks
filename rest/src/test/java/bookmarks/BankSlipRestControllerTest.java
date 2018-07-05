package bookmarks;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * @author Josh Long
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class BankSlipRestControllerTest {
    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));

    private MockMvc mockMvc;

    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BankSlipRepository bankSlipRepository;

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.asList(converters).stream()
            .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
            .findAny()
            .orElse(null);

        assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
    }

    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
        this.bankSlipRepository.deleteAllInBatch();
    }

    @Test
    public void bankSlipNotFound() throws Exception {
        mockMvc.perform(get("/bankslips/1")
                .contentType(contentType))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Bankslip not found with the specified id"));
    }

    @Test
    public void readSingleBankSlip() throws Exception {
        BankSlip bankSlip = this.bankSlipRepository.save(new BankSlip("Trillian Company", parseDate("2018-01-01"), new BigDecimal(100000)));

        mockMvc.perform(get("/bankslips/" + bankSlip.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.customer", is(bankSlip.getCustomer())))
                .andExpect(jsonPath("$.due_date", startsWith(dateToString(bankSlip.getDueDate()))))
                .andExpect(jsonPath("$.total_in_cents", is(bankSlip.getTotalInCents().intValue()+"")));
    }

    @Test
    public void readAllBankSlips() throws Exception {
        List<BankSlip> lista = new ArrayList<>();

        lista.add(this.bankSlipRepository.save(new BankSlip("Ford Prefect Company", parseDate("2018-01-01"), new BigDecimal(100000))));
        lista.add(this.bankSlipRepository.save(new BankSlip("Zaphod Company", parseDate("2018-02-01"), new BigDecimal(200000))));

        mockMvc.perform(get("/bankslips"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].customer", is(lista.get(0).getCustomer())))
                .andExpect(jsonPath("$[0].due_date", startsWith(dateToString(lista.get(0).getDueDate()))))
                .andExpect(jsonPath("$[0].total_in_cents", is(lista.get(0).getTotalInCents().intValue()+"")))
                .andExpect(jsonPath("$[1].customer", is(lista.get(1).getCustomer())))
                .andExpect(jsonPath("$[1].due_date", startsWith(dateToString(lista.get(1).getDueDate()))))
                .andExpect(jsonPath("$[1].total_in_cents", is(lista.get(1).getTotalInCents().intValue()+"")));
    }

    @Test
    public void createBankSlip() throws Exception {
        String customer = "Trillian Company";
        Calendar dueDate = Calendar.getInstance();
        BigDecimal totalInCents = new BigDecimal(100000);
        BankSlip bankSlip = new BankSlip(customer, dueDate, totalInCents);
        String bookmarkJson = json(bankSlip);

        this.mockMvc.perform(post("/bankslips")
                .contentType(contentType)
                .content(bookmarkJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customer", is(customer)))
                .andExpect(jsonPath("$.due_date", startsWith(dateToString(dueDate))))
                .andExpect(jsonPath("$.total_in_cents", is(totalInCents.intValue()+"")));
    }

    @Test
    public void createBankSlipWithoutBody() throws Exception {
        this.mockMvc.perform(post("/bankslips")
                .contentType(contentType))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string("Bankslip not provided in the request body"));
    }

    @Test
    public void createBankSlipWithoutRequiredFields() throws Exception {
        String customer = "Trillian Company";
        Calendar dueDate = parseDate("2018-01-01");
        BigDecimal totalInCents = new BigDecimal(100000);
        String bankSplitJson = json(new BankSlip(null, dueDate, totalInCents));
        String messageExpected = "Invalid bankslip provided.The possible reasons are:\n" +
                "* A field of the provided bankslip was null or with invalid values";

        this.mockMvc.perform(post("/bankslips")
                .contentType(contentType)
                .content(bankSplitJson))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(messageExpected));

        bankSplitJson = json(new BankSlip(customer, null, totalInCents));

        this.mockMvc.perform(post("/bankslips")
                .contentType(contentType)
                .content(bankSplitJson))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(messageExpected));

        bankSplitJson = json(new BankSlip(customer, dueDate, null));

        this.mockMvc.perform(post("/bankslips")
                .contentType(contentType)
                .content(bankSplitJson))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(messageExpected));

        bankSplitJson = json(new BankSlip(customer, dueDate, new BigDecimal(-1)));

        this.mockMvc.perform(post("/bankslips")
                .contentType(contentType)
                .content(bankSplitJson))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(messageExpected));
    }

    @Test
    public void doPaymentWithNonExistentBankSplip() throws Exception {
        this.mockMvc.perform(post("/bankslips/1/payments")
                .contentType(contentType)
                .content("{\"payment_date\" : \"2018-06-30\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Bankslip not found with the specified id"))
                .andDo(print());
    }

    @Test
    public void doPayment() throws Exception {
        String customer = "Trillian Company";
        Calendar dueDate = Calendar.getInstance();
        BigDecimal totalInCents = new BigDecimal(100000);
        BankSlip bankSlip = new BankSlip(customer, dueDate, totalInCents);
        bankSlipRepository.save(bankSlip);

        this.mockMvc.perform(post("/bankslips/" + bankSlip.getId() + "/payments")
                .contentType(contentType)
                .content("{\"payment_date\" : \"2018-06-30\"}"))
                .andExpect(status().isNoContent());

        Calendar paymentDate = bankSlipRepository.findById(bankSlip.getId()).get().getPaymentDate();
        assertEquals("2018-06-30", dateToString(paymentDate));
    }

    protected String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.mappingJackson2HttpMessageConverter.write(
                o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    private String dateToString(final Calendar calendar){
        return DateFormatUtils.format(calendar.getTime(), "yyyy-MM-dd");
    }

    private static Calendar parseDate(final String date) throws ParseException {
        return DateUtils.toCalendar(DateUtils.parseDate(date, new String[]{"yyyy-MM-dd"}));
    }
}