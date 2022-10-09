package tourGuide.controller;


import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tourGuide.TourGuideController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
public class TourGuideControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    public void indexTest() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    public void getLocationTest() throws Exception {
        mockMvc.perform(get("/getLocation").param("userName", "internalUser1")).andExpect(status().isOk());
    }

    @Test
    public void getNearbyAttractionsTest() throws Exception {
        mockMvc.perform(get("/getNearbyAttractions").param("userName", "internalUser1")).andExpect(status().isOk());
    }

    @Test
    public void getRewardsTest() throws Exception {
        mockMvc.perform(get("/getRewards").param("userName", "internalUser1")).andExpect(status().isOk());
    }

    @Test
    public void getAllCurrentLocationsTest() throws Exception {
        mockMvc.perform(get("/getAllCurrentLocations")).andExpect(status().isOk());
    }

    @Test
    public void getTripDealsTest() throws Exception {
        mockMvc.perform(get("/getTripDeals").param("userName", "internalUser1")).andExpect(status().isOk());
    }

    @Ignore
    //TODO:
    @Test
    public void updateUserPreferencesTest() throws Exception {
        mockMvc.perform(get("/updateUserPreferences").param("userName", "internalUser1")).andExpect(status().isOk());
    }
}
