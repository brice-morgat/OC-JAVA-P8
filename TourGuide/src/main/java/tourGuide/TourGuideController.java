package tourGuide;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsoniter.output.JsonStream;

import gpsUtil.location.VisitedLocation;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tripPricer.Provider;

@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;
	
    /**
     * The function is called index, it returns a string, and it's mapped to the root URL
     *
     * @return A string
     */
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    /**
     * > This function returns the location of the user with the given userName
     *
     * @param userName The name of the user whose location you want to get.
     * @return The location of the user.
     */
    @GetMapping("/getLocation")
    public String getLocation(@RequestParam String userName) throws ExecutionException, InterruptedException {
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
		return JsonStream.serialize(visitedLocation.location);
    }

    /**
     * > This function takes in a userName as a parameter and returns a list of nearby attractions in JSON format
     *
     * @param userName The user name of the user who is requesting the nearby attractions.
     * @return A list of nearby attractions.
     */
    @RequestMapping("/getNearbyAttractions")
    public String getNearbyAttractions(@RequestParam String userName) throws ExecutionException, InterruptedException {
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
    	return JsonStream.serialize(tourGuideService.getNearByAttractionsList(visitedLocation));
    }
    
    /**
     * > This function returns a JSON string of the rewards that a user has earned
     *
     * @param userName The user's name
     * @return A JSON string of the user's rewards.
     */
    @RequestMapping("/getRewards")
    public String getRewards(@RequestParam String userName) {
    	return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
    }
    
    /**
     * > This function returns a JSON string of all the current locations of all the tour guides
     *
     * @return A list of all the current locations of the tour guides.
     */
    @RequestMapping("/getAllCurrentLocations")
    public String getAllCurrentLocations() {
    	return JsonStream.serialize(tourGuideService.getAllCurrentLocation());
    }
    
    /**
     * > This function returns a list of providers that have deals for the user
     *
     * @param userName The user name of the user who is requesting the trip deals.
     * @return A list of providers
     */
    @RequestMapping("/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
    	List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
    	return JsonStream.serialize(providers);
    }

    /**
     * > Update the user preferences for the given user
     *
     * @param userName The user name of the user whose preferences are being updated.
     * @param userPreferences This is the object that contains the user preferences.
     * @return A string
     */
    @PutMapping("/updateUserPreferences")
    public String updateUserPreferences(@RequestParam String userName, @RequestBody UserPreferences userPreferences) {
        User user = getUser(userName);
        return JsonStream.serialize(tourGuideService.updateUserPreferences(user,userPreferences));
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }
}