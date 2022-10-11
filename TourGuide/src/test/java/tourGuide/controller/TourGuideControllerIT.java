package tourGuide.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONObject;
import org.javamoney.moneta.Money;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tourGuide.TourGuideController;
import tourGuide.service.TourGuideService;
import tourGuide.user.UserPreferences;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

import java.util.Locale;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
public class TourGuideControllerIT {

    @Autowired
    TourGuideService tourGuideService;

    @Autowired
    MockMvc mockMvc;

    @Test
    public void indexTest() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    public void getLocationTest() throws Exception {
        mockMvc.perform(get("/getLocation")
                        .param("userName", "internalUser1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(2)));
    }

    @Test
    public void getNearbyAttractionsTest() throws Exception {
        mockMvc.perform(get("/getNearbyAttractions")
                .param("userName", "internalUser1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)));
    }

    @Test
    public void getRewardsTest() throws Exception {
        mockMvc.perform(get("/getRewards")
                .param("userName", "internalUser1"))
                .andExpect(status().isOk());
    }

    @Test
    public void getAllCurrentLocationsTest() throws Exception {
        mockMvc.perform(get("/getAllCurrentLocations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(100)));
    }

    @Test
    public void getTripDealsTest() throws Exception {
        mockMvc.perform(get("/getTripDeals")
                .param("userName", "internalUser1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)));
    }

    @Test
    public void updateUserPreferencesTest() throws Exception {
        Locale.setDefault(Locale.US);
        UserPreferences userPreferences = new UserPreferences();
        CurrencyUnit currency = Monetary.getCurrency("USD");
        userPreferences.setHighPricePoint(Money.of(120, currency));
        userPreferences.setAttractionProximity(500);
        userPreferences.setLowerPricePoint(Money.of(0, currency));
        userPreferences.setNumberOfAdults(1);
        userPreferences.setTicketQuantity(1);
        userPreferences.setTripDuration(10);
        JSONObject input = new JSONObject();
        input.put("tripDuration", 1);
        input.put("currency", Monetary.getCurrency("USD"));
        input.put("lowerPricePoint", null);
        input.put("highPricePoint", null);
        input.put("ticketQuantity", 10);
        input.put("numberOfAdults", 2);
        input.put("numberOfChildren", 2);
        input.put("attractionProximity", 600);

        mockMvc.perform(put("/updateUserPreferences")
                .param("userName", "internalUser1")
                .content(asJsonString(input)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
